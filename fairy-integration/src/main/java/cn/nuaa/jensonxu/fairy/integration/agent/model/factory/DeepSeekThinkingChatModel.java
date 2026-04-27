package cn.nuaa.jensonxu.fairy.integration.agent.model.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * DeepSeek 思考模式专用 ChatModel
 * 解决问题：DeepSeek 在思考模式 + 工具调用场景下，后续请求必须将上一轮的
 * reasoning_content 完整回传，否则 API 返回 400。Spring AI 的 OpenAiChatModel
 * 不包含此字段，因此自行实现请求构造层。
 */
@Slf4j
public class DeepSeekThinkingChatModel implements ChatModel {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final WebClient webClient;
    private final OpenAiChatOptions defaultOptions;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> thinkingConfig;  // {"type": "enabled"}

    private DeepSeekThinkingChatModel(String baseUrl, String apiKey, OpenAiChatOptions defaultOptions, Map<String, Object> thinkingConfig) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.defaultOptions = defaultOptions;
        this.thinkingConfig = thinkingConfig;
        this.objectMapper = new ObjectMapper();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NonNull ChatResponse call(Prompt prompt) {
        return Objects.requireNonNull(stream(prompt).blockLast());
    }

    @Override
    public @NonNull Flux<ChatResponse> stream(Prompt prompt) {
        Map<String, Object> requestBody = buildRequestBody(prompt);
        ToolCallAccumulator accumulator = new ToolCallAccumulator();

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> !"[DONE]".equals(data))
                .flatMapIterable(data -> processChunk(data, accumulator));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return defaultOptions;
    }

    /**
     * 构造请求体
     */
    private Map<String, Object> buildRequestBody(Prompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", defaultOptions.getModel());
        body.put("stream", true);
        body.put("messages", buildMessages(prompt.getInstructions()));

        if (defaultOptions.getTemperature() != null)    body.put("temperature", defaultOptions.getTemperature());
        if (defaultOptions.getMaxTokens() != null)  body.put("max_tokens", defaultOptions.getMaxTokens());
        if (defaultOptions.getTopP() != null)   body.put("top_p", defaultOptions.getTopP());

        if (thinkingConfig != null && !thinkingConfig.isEmpty()) {
            body.put("thinking", thinkingConfig);
        }

        List<Map<String, Object>> tools = buildTools(prompt);
        if (!tools.isEmpty()) body.put("tools", tools);

        return body;
    }

    /**
     * 将 Spring AI Message 列表转换为 DeepSeek 请求格式。
     * 核心修复点：AssistantMessage 中 metadata.reasoningContent 不为空时，
     * 将其作为 reasoning_content 字段写入请求体。
     */
    private List<Map<String, Object>> buildMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message message : messages) {
            if (message instanceof SystemMessage) {
                result.add(Map.of("role", "system", "content", message.getText()));
            } else if (message instanceof UserMessage) {
                result.add(Map.of("role", "user", "content", message.getText()));
            } else if (message instanceof AssistantMessage assistantMessage) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("role", "assistant");
                msg.put("content", Objects.toString(assistantMessage.getText(), ""));

                // 回传上一轮的 reasoning_content，否则 DeepSeek 返回 400
                Object reasoningContent = assistantMessage.getMetadata().get("reasoningContent");
                if (reasoningContent != null && !reasoningContent.toString().isBlank()) {
                    msg.put("reasoning_content", reasoningContent.toString());
                }

                if (assistantMessage.hasToolCalls()) {
                    List<Map<String, Object>> toolCalls = new ArrayList<>();
                    for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
                        toolCalls.add(Map.of(
                                "id", tc.id(),
                                "type", "function",
                                "function", Map.of(
                                        "name", tc.name(),
                                        "arguments", tc.arguments()
                                )
                        ));
                    }
                    msg.put("tool_calls", toolCalls);
                }
                result.add(msg);

            } else if (message instanceof ToolResponseMessage toolResponse) {
                for (ToolResponseMessage.ToolResponse resp : toolResponse.getResponses()) {
                    result.add(Map.of(
                            "role", "tool",
                            "tool_call_id", resp.id(),
                            "content", resp.responseData()
                    ));
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildTools(Prompt prompt) {
        List<Map<String, Object>> tools = new ArrayList<>();
        ChatOptions options = prompt.getOptions();
        log.debug("[deepseek] prompt.options 类型: {}", options == null ? "null" : options.getClass().getName());  // 加日志，上线前排查用
        if (!(options instanceof ToolCallingChatOptions toolOpts)) return tools;

        List<ToolCallback> callbacks = toolOpts.getToolCallbacks();
        if (callbacks.isEmpty()) {
            log.debug("[deepseek] toolCallbacks 为空");
            return tools;
        }
        log.debug("[deepseek] 注入工具数量: {}", callbacks.size());

        for (ToolCallback cb : callbacks) {
            try {
                Map<String, Object> parameters = objectMapper.readValue(
                        cb.getToolDefinition().inputSchema(), MAP_TYPE);
                tools.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name",        cb.getToolDefinition().name(),
                                "description", cb.getToolDefinition().description(),
                                "parameters",  parameters
                        )
                ));
            } catch (Exception e) {
                log.warn("[deepseek] 工具 schema 解析失败: {}", cb.getToolDefinition().name(), e);
            }
        }
        return tools;
    }

    /**
     * 处理单个 SSE chunk，返回需要向下游 emit 的 ChatResponse 列表：
     * - reasoning_content / 文本 chunk：立即 emit
     * - tool_call delta：仅在 accumulator 中累积，暂不 emit
     * - finish_reason=tool_calls：将累积的完整 tool calls 组装后 emit
     */
    @SuppressWarnings("unchecked")
    private List<ChatResponse> processChunk(String json, ToolCallAccumulator accumulator) {
        try {
            Map<String, Object> chunk = objectMapper.readValue(json, MAP_TYPE);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (choices == null || choices.isEmpty()) return List.of();

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> delta  = (Map<String, Object>) choice.get("delta");
            String finishReason        = (String) choice.get("finish_reason");
            if (delta == null) return List.of();

            List<ChatResponse> responses = new ArrayList<>();

            // thinking 内容块
            Object reasoning = delta.get("reasoning_content");
            if (reasoning != null && !reasoning.toString().isBlank()) {
                accumulator.accumulateReasoning(reasoning.toString());
                responses.add(toResponse("", Map.of("reasoningContent", reasoning.toString()), null, null));
            }

            // 正常文本块
            String content = (String) delta.get("content");
            if (content != null && !content.isEmpty()) {
                responses.add(toResponse(content, Map.of(), null, null));
            }

            // tool_call delta 累积
            List<Map<String, Object>> rawToolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
            if (rawToolCalls != null) {
                accumulator.accumulate(rawToolCalls);
            }

            // 工具调用轮次结束，emit 完整 tool calls，将累积的 reasoning_content 写入 metadata
            if ("tool_calls".equals(finishReason)) {
                String accReasoning = accumulator.getAccumulatedReasoning();
                Map<String, Object> metaData = accReasoning.isBlank() ? Map.of() : Map.of("reasoningContent", accReasoning);
                responses.add(toResponse("", metaData, accumulator.build(), finishReason));
            }

            // 正常对话结束
            if ("stop".equals(finishReason)) {
                responses.add(toResponse("", Map.of(), null, finishReason));
            }

            return responses;
        } catch (Exception e) {
            log.warn("[DeepSeek] SSE chunk 解析失败: {}", json, e);
            return List.of();
        }
    }

    private ChatResponse toResponse(String text, Map<String, Object> metadata, List<AssistantMessage.ToolCall> toolCalls, String finishReason) {
        AssistantMessage msg = AssistantMessage.builder()
                .content(text)
                .properties(metadata)
                .toolCalls(toolCalls != null ? toolCalls : List.of())
                .build();

        ChatGenerationMetadata genMeta = (finishReason != null) ? ChatGenerationMetadata.builder().finishReason(finishReason).build() : ChatGenerationMetadata.NULL;
        return new ChatResponse(List.of(new Generation(msg, genMeta)));
    }

    /**
     * 将流式返回的 tool_call delta 按 index 累积，最终拼接成完整的 ToolCall 列表
     */
    private static class ToolCallAccumulator {

        private final StringBuilder reasoningBuffer = new StringBuilder();
        private final Map<Integer, ToolCallBuilder> builders = new LinkedHashMap<>();

        public void accumulateReasoning(String reasoning) {
            reasoningBuffer.append(reasoning);
        }

        public String getAccumulatedReasoning() {
            return reasoningBuffer.toString();
        }

        @SuppressWarnings("unchecked")
        public void accumulate(List<Map<String, Object>> rawToolCalls) {
            for (Map<String, Object> raw : rawToolCalls) {
                int index = ((Number) raw.getOrDefault("index", 0)).intValue();
                ToolCallBuilder builder = builders.computeIfAbsent(index, k -> new ToolCallBuilder());

                if (raw.containsKey("id"))   builder.id   = (String) raw.get("id");
                if (raw.containsKey("type")) builder.type = (String) raw.get("type");

                Map<String, Object> fn = (Map<String, Object>) raw.get("function");
                if (fn != null) {
                    if (fn.containsKey("name"))      builder.name = (String) fn.get("name");
                    if (fn.containsKey("arguments")) builder.arguments.append(fn.get("arguments"));
                }
            }
        }

        public List<AssistantMessage.ToolCall> build() {
            List<AssistantMessage.ToolCall> result = new ArrayList<>();
            for (ToolCallBuilder b : builders.values()) {
                result.add(new AssistantMessage.ToolCall(
                        b.id,
                        Objects.toString(b.type, "function"),
                        Objects.toString(b.name, ""),
                        b.arguments.toString()
                ));
            }
            return result;
        }

        private static class ToolCallBuilder {
            String id, type, name;
            final StringBuilder arguments = new StringBuilder();
        }
    }

    public static final class Builder {
        private String baseUrl;
        private String apiKey;
        private OpenAiChatOptions defaultOptions;
        private Map<String, Object> thinkingConfig = Map.of();

        private Builder() {}

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder defaultOptions(OpenAiChatOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public Builder thinkingConfig(Map<String, Object> thinkingConfig) {
            this.thinkingConfig = thinkingConfig;
            return this;
        }

        public DeepSeekThinkingChatModel build() {
            Assert.hasText(baseUrl, "baseUrl must not be blank");
            Assert.hasText(apiKey, "apiKey must not be blank");
            Assert.notNull(defaultOptions, "defaultOptions must not be null");
            return new DeepSeekThinkingChatModel(baseUrl, apiKey, defaultOptions, thinkingConfig);
        }
    }
}