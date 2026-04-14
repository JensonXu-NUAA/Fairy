package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer.SummaryItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private final AgentProperties agentProperties;

    /**
     * 会话启动阶段：加载短期记忆 + 构建长期记忆注入文本
     * 若短期记忆超过阈值，在此阶段完成压缩，保证返回的消息数在窗口上限以内
     */
    public AgentLoadedContext loadContext(String sessionId, String userId) {
        log.info("[memory] 加载会话上下文, sessionId: {}, userId: {}", sessionId, userId);
        List<Message> messages = shortTermMemory.loadMessages(sessionId);
        int maxMessages = agentProperties.getMemory().getShortTerm().getMaxMessages();  // 获取最大消息数

        if (messages.size() > maxMessages) {
            log.info("[memory] 消息数 {} 超过阈值 {}，触发加载阶段压缩, sessionId: {}", messages.size(), maxMessages, sessionId);
            messages = compressMessages(messages, sessionId, userId);
        }

        log.info("[memory] 上下文加载完成，短期记忆 {} 条, sessionId: {}", messages.size(), sessionId);
        return new AgentLoadedContext(messages);
    }

    /**
     * 消息压缩：取最早的 N 条进行摘要提炼，结果写入长期记忆，Redis 同步修剪
     * @return 压缩后保留的消息列表
     */
    private List<Message> compressMessages(List<Message> messages, String sessionId, String userId) {
        int summarizeCount = agentProperties.getMemory().getShortTerm().getMaxMessages() / 2;
        List<Message> toSummarize = messages.subList(0, summarizeCount);
        List<Message> toKeep = new ArrayList<>(messages.subList(summarizeCount, messages.size()));
        List<SummaryItem> items = summarizer.summarize(toSummarize, userId, sessionId);
        if (!items.isEmpty()) {
            for (SummaryItem item : items) {
                longTermMemory.saveMemory(userId, item.key(), item.category(), item.content(), item.importance(), sessionId);
            }
            log.info("[memory] 加载阶段压缩：长期记忆写入 {} 条, sessionId: {}", items.size(), sessionId);
        } else {
            log.debug("[memory] 加载阶段压缩：摘要结果为空，无内容写入长期记忆, sessionId: {}", sessionId);
        }

        shortTermMemory.replaceMessages(sessionId, toKeep);
        log.info("[memory] 加载阶段压缩：Redis 已修剪为 {} 条消息, sessionId: {}", toKeep.size(), sessionId);
        return toKeep;
    }
}