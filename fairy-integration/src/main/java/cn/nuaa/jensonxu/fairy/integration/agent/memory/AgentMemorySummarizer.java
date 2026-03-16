package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.manager.AgentModelManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 记忆摘要提炼器
 * 调用轻量摘要模型，从对话消息中提炼关键事实和用户偏好，输出结构化记忆条目
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMemorySummarizer {

    private final AgentModelManager agentModelManager;
    private final AgentProperties agentProperties;

    private static final String SYSTEM_PROMPT = """
            你是一个对话记忆提炼助手。请从以下用户与 AI 的对话记录中，\
            提取值得长期记忆的关键事实和用户偏好信息。
            
            规则：
            1. 只提取客观事实（如姓名、职业、地点、项目信息）和明确偏好（如编程语言偏好、表达习惯）
            2. 忽略闲聊、问候语、通用知识问答等无实质记忆价值的内容
            3. 每条记忆内容简洁，不超过 50 字，表述客观、不带主观判断
            4. importance 字段取值 1-10，根据信息对未来对话的参考价值判断
            5. 严格输出 JSON 数组格式，不包含任何额外文字：
               [{"key":"标签","content":"内容","importance":数字}, ...]
            6. 若对话中无任何值得记忆的内容，输出空数组：[]
            """;

    /**
     * 对消息列表进行摘要提炼，返回结构化记忆条目列表
     * 同步调用摘要模型；调用方负责将结果写入 MySQL
     *
     * @param messages  待提炼的消息列表
     * @param userId    用户 ID（用于日志）
     * @param sessionId 来源会话 ID
     * @return 提炼出的记忆条目列表，失败或无内容时返回空列表
     */
    public List<SummaryItem> summarize(List<Message> messages, String userId, String sessionId) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        String modelName = agentProperties.getMemory().getLongTerm().getModelName();
        ModelConfig modelConfig = agentModelManager.getSummaryModelConfig(modelName);
        String userContent = buildUserContent(messages);

        log.info("[summarizer] 开始提炼摘要, userId: {}, sessionId: {}, 消息数: {}, 模型配置: {}", userId, sessionId, messages.size(),modelConfig);
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
                    .user(userContent)
                    .call()
                    .content();

            List<SummaryItem> items = parseResult(result);
            log.info("[summarizer] 提炼完成, userId: {}, sessionId: {}, 提炼出 {} 条记忆", userId, sessionId, items.size());
            return items;

        } catch (Exception e) {
            log.error("[summarizer] 摘要提炼失败, userId: {}, sessionId: {}", userId, sessionId, e);
            return List.of();
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

    private List<SummaryItem> parseResult(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        // 提取 JSON 数组，防止模型在前后附加说明文字
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end < 0 || start >= end) {
            log.warn("[summarizer] 摘要模型返回了非 JSON 格式内容: {}", json);
            return List.of();
        }
        try {
            JSONArray array = JSON.parseArray(json.substring(start, end + 1));
            List<SummaryItem> items = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String key = obj.getString("key");
                String content = obj.getString("content");
                int importance = obj.getIntValue("importance", 5);
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(content)) {
                    items.add(new SummaryItem(key, content, importance));
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("[summarizer] JSON 解析失败, 原始内容: {}", json, e);
            return List.of();
        }
    }

    /**
     * 摘要提炼结果的值对象
     */
    public record SummaryItem(String key, String content, int importance) {}
}