package cn.nuaa.jensonxu.fairy.integration.agent.memory.hook;

import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer.SummaryItem;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentShortTermMemory;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息窗口压缩 Hook（BEFORE_MODEL）
 * 当 MemorySaver 中的消息数超过阈值时，触发摘要提炼并压缩上下文窗口：
 * 1. 对旧消息调用摘要模型，将提炼结果写入 MySQL 长期记忆
 * 2. 调用 replaceMessages() 同步替换 Redis 消息列表
 * 3. 通过 AgentCommand(REPLACE) 更新 MemorySaver，确保两者始终一致
 */
@Slf4j
@Component
@RequiredArgsConstructor
@HookPositions({HookPosition.BEFORE_MODEL})
public class AgentSummarizationHook extends MessagesModelHook {

    private static final String USER_ID_KEY = "user_id";

    private final AgentShortTermMemory shortTermMemory;
    private final AgentLongTermMemory longTermMemory;
    private final AgentMemorySummarizer summarizer;
    private final AgentProperties agentProperties;

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        int maxMessages = agentProperties.getMemory().getShortTerm().getMaxMessages();

        // 未超过阈值，直接透传
        if (messages.size() < maxMessages) {
            return new AgentCommand(messages);
        }

        String sessionId = config.threadId().orElse(null);
        String userId = config.metadata(USER_ID_KEY).map(Object::toString).orElse(null);

        if (sessionId == null || userId == null) {
            log.warn("[summarization] 缺少 sessionId 或 userId，跳过压缩, messages: {}", messages.size());
            return new AgentCommand(messages);
        }
        log.info("[summarization] 消息数 {} 达到阈值 {}，开始压缩, sessionId: {}", messages.size(), maxMessages, sessionId);

        // 保留最近一半消息，对前面的消息进行摘要
        int keepCount = maxMessages / 2;
        List<Message> toSummarize = messages.subList(0, messages.size() - keepCount);
        List<Message> toKeep = messages.subList(messages.size() - keepCount, messages.size());

        // 1. 摘要提炼 → 写入 MySQL 长期记忆
        List<SummaryItem> items = summarizer.summarize(toSummarize, userId, sessionId);
        for (SummaryItem item : items) {
            longTermMemory.saveMemory(userId, item.key(), item.content(), item.importance(), sessionId);
        }
        log.info("[summarization] 长期记忆写入 {} 条, sessionId: {}", items.size(), sessionId);

        // 2. 同步替换 Redis 消息列表
        shortTermMemory.replaceMessages(sessionId, toKeep);
        log.info("[summarization] Redis 已替换为压缩后 {} 条消息, sessionId: {}", toKeep.size(), sessionId);

        // 3. 更新 MemorySaver（与 Redis 保持一致）
        return new AgentCommand(toKeep, UpdatePolicy.REPLACE);
    }

    @Override
    public String getName() {
        return "AgentSummarizationHook";
    }
}