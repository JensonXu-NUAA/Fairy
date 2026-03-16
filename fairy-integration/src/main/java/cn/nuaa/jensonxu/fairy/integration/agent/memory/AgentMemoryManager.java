package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer.SummaryItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 记忆管理统一入口
 * 编排短期记忆（Redis + MySQL）与长期记忆（MySQL）的读写
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMemoryManager {

    private final AgentShortTermMemory shortTermMemory;
    private final AgentLongTermMemory longTermMemory;
    private final AgentMemorySummarizer summarizer;

    /**
     * 会话启动阶段：加载短期记忆 + 构建长期记忆注入文本
     */
    public AgentLoadedContext loadContext(String sessionId, String userId) {
        log.info("[memory] 加载会话上下文, sessionId: {}, userId: {}", sessionId, userId);
        List<Message> messages = shortTermMemory.loadMessages(sessionId);
        String systemPromptPrefix = longTermMemory.buildSystemPromptPrefix(userId);
        log.info("[memory] 上下文加载完成，短期记忆 {} 条，长期记忆注入长度 {} 字符", messages.size(), systemPromptPrefix.length());
        return new AgentLoadedContext(messages, systemPromptPrefix);
    }

    /**
     * 每轮推理结束后：写入短期记忆，若有消息被滑动窗口淘汰则触发摘要（触发一）
     */
    public void saveRoundMessages(String sessionId, String userId, Message humanMessage, Message assistantMessage) {
        List<Message> evicted = shortTermMemory.saveMessages(sessionId, userId, List.of(humanMessage, assistantMessage));
        log.debug("[memory] 本轮消息已持久化, sessionId: {}", sessionId);

        if (!evicted.isEmpty()) {
            log.info("[memory] 触发一：滑动窗口淘汰 {} 条消息，开始摘要提炼, sessionId: {}", evicted.size(), sessionId);
            summarizeAndSave(evicted, userId, sessionId);
        }
    }

    /**
     * 会话显式结束时回调：对 Redis 中剩余消息执行最终摘要（触发二）
     */
    public void onSessionEnd(String sessionId, String userId) {
        log.info("[memory] 会话结束，触发二：对剩余消息执行最终摘要, sessionId: {}", sessionId);
        List<Message> remaining = shortTermMemory.loadMessages(sessionId);
        if (remaining.isEmpty()) {
            log.debug("[memory] 会话结束时无剩余消息，跳过摘要, sessionId: {}", sessionId);
            return;
        }
        summarizeAndSave(remaining, userId, sessionId);
    }

    /**
     * 调用摘要模型并将结果写入长期记忆
     */
    private void summarizeAndSave(List<Message> messages, String userId, String sessionId) {
        List<SummaryItem> items = summarizer.summarize(messages, userId, sessionId);
        if (items.isEmpty()) {
            log.debug("[memory] 摘要结果为空，无需写入长期记忆, sessionId: {}", sessionId);
            return;
        }
        for (SummaryItem item : items) {
            longTermMemory.saveMemory(userId, item.key(), item.content(), item.importance(), sessionId);
        }
        log.info("[memory] 长期记忆写入完成，共 {} 条, userId: {}, sessionId: {}", items.size(), userId, sessionId);
    }
}