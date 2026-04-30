package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.model.manager.NativeModelManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 上下文压缩器
 * 在短期记忆消息数超过窗口上限时，将早期消息压缩为一段纯文本摘要，
 * 以摘要消息替换原始消息注入上下文，不写入任何数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentContextCompressor {

    private final NativeModelManager nativeModelManager;
    private final AgentProperties agentProperties;

    private static final String SYSTEM_PROMPT = """
            你是一个对话上下文压缩助手。请将以下对话历史压缩为一段简洁的纯文本摘要。
            
            【要求】
            1. 保留关键信息：用户提出的核心问题、重要决策、达成的结论、确认的事实
            2. 忽略过渡性内容：问候语、重复确认、无实质内容的闲聊
            3. 使用第三人称客观叙述，如"用户询问了……""双方确认了……"
            4. 控制在 200 字以内，语言简洁流畅
            5. 直接输出摘要正文，不加任何标题或前缀说明
            """;

    /**
     * 将消息列表压缩为纯文本摘要
     *
     * @param messages  待压缩的早期消息列表
     * @param sessionId 会话 ID（用于日志）
     * @return 压缩后的摘要文本；压缩失败时返回空字符串
     */
    public String compress(List<Message> messages, String sessionId) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        String modelName = agentProperties.getMemory().getLongTerm().getModelName();
        ModelConfig modelConfig = nativeModelManager.getSummaryModelConfig(modelName);

        log.info("[compressor] 开始压缩上下文, sessionId: {}, 消息数: {}", sessionId, messages.size());
        try {
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey(modelConfig.getApiKey())
                    .baseUrl(modelConfig.getBaseUrl())
                    .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(modelConfig.getModelName())
                            .build())
                    .build();

            String result = ChatClient.builder(chatModel).build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserContent(messages))
                    .call()
                    .content();

            if (StringUtils.isBlank(result)) {
                log.warn("[compressor] 压缩结果为空, sessionId: {}", sessionId);
                return "";
            }

            log.info("[compressor] 压缩完成, sessionId: {}, 摘要长度: {} 字", sessionId, result.length());
            return result.trim();

        } catch (Exception e) {
            log.error("[compressor] 上下文压缩失败, sessionId: {}", sessionId, e);
            return "";
        }
    }

    private String buildUserContent(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String role = MessageType.ASSISTANT.equals(msg.getMessageType()) ? "AI" : "用户";
            sb.append(role).append(": ").append(msg.getText()).append("\n");
        }
        return sb.toString();
    }
}