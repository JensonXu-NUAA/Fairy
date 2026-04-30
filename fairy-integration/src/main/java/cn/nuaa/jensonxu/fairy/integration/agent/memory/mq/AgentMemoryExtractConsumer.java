package cn.nuaa.jensonxu.fairy.integration.agent.memory.mq;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSessionMessageRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSessionMessageDO;
import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQConsumer;
import cn.nuaa.jensonxu.fairy.common.rocketmq.message.AgentMemoryExtractMessage;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer.SummaryItem;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 长期记忆提炼消息消费者
 * 消费 AfterAgentMemoryHook 投递的触发消息，加载本轮对话，
 * 调用 AgentMemorySummarizer 提炼结构化记忆并写入长期记忆表
 * RocketMQ 消费失败时自动重试（最多 16 次，指数退避），超出后进入死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "agent-memory-extract",
        consumerGroup = "agent-memory-extract-group"
)
public class AgentMemoryExtractConsumer extends AbstractRocketMQConsumer<AgentMemoryExtractMessage> implements RocketMQListener<AgentMemoryExtractMessage> {

    private final AgentSessionMessageRepository sessionMessageRepository;
    private final AgentMemorySummarizer summarizer;
    private final AgentLongTermMemory longTermMemory;

    @Override
    public void onMessage(AgentMemoryExtractMessage message) {
        consume(message);
    }

    @Override
    protected boolean doConsume(AgentMemoryExtractMessage message) {
        String sessionId = message.getSessionId();
        String userId = message.getUserId();

        log.info("[extract-consumer] 开始长期记忆提炼, sessionId: {}, originTimestamp: {}", sessionId, message.getOriginTimestamp());
        List<Message> messages = loadLatestMessages(sessionId);  // 加载本轮最新两条消息（UserMessage + AssistantMessage）
        if (messages.isEmpty()) {
            log.warn("[extract-consumer] 未找到可提炼的消息, sessionId: {}", sessionId);
            return true;  // 视为正常消费，无需重试
        }

        List<SummaryItem> items = summarizer.summarize(messages, userId, sessionId);  // 调用 Summarizer 提炼结构化记忆
        if (items.isEmpty()) {
            log.debug("[extract-consumer] 本轮无值得提炼的记忆, sessionId: {}", sessionId);
            return true;
        }

        for (SummaryItem item : items) {
            longTermMemory.saveMemory(userId, item.key(), item.name(), item.description(), item.category(), item.content(), sessionId);  // 逐条写入长期记忆
        }
        log.info("[extract-consumer] 长期记忆写入完成, sessionId: {}, 写入 {} 条", sessionId, items.size());
        return true;
    }

    @Override
    protected void afterConsumeFailed(AgentMemoryExtractMessage message) {
        log.warn("[extract-consumer] 本次消费失败，等待 RocketMQ 自动重试, sessionId: {}", message.getSessionId());
    }

    @Override
    protected void onConsumeError(AgentMemoryExtractMessage message, Throwable throwable) {
        log.error("[extract-consumer] 消费异常，触发 RocketMQ 重试, sessionId: {}", message.getSessionId(), throwable);
    }

    /**
     * 从 MySQL 加载该会话最近两条消息（本轮 user + assistant）
     * findBySessionId 按 seq 升序返回，取末尾两条即为本轮消息
     */
    private List<Message> loadLatestMessages(String sessionId) {
        List<AgentSessionMessageDO> all = sessionMessageRepository.findBySessionId(sessionId);
        if (all.isEmpty()) {
            return List.of();
        }
        int size = all.size();
        List<AgentSessionMessageDO> latest = all.subList(Math.max(0, size - 2), size);
        List<Message> messages = new ArrayList<>();
        for (AgentSessionMessageDO record : latest) {
            Message msg = deserialize(record.getContent());
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    /**
     * 与 AgentShortTermMemory.serialize() 保持一致的反序列化逻辑
     */
    private Message deserialize(String json) {
        try {
            JSONObject obj = JSON.parseObject(json);
            String role = obj.getString("role");
            String content = obj.getString("content");
            return "assistant".equals(role) ? new AssistantMessage(content) : new UserMessage(content);
        } catch (Exception e) {
            log.warn("[extract-consumer] 消息反序列化失败, json: {}", json, e);
            return null;
        }
    }
}