package cn.nuaa.jensonxu.fairy.integration.agent.memory.hook;

import cn.nuaa.jensonxu.fairy.common.rocketmq.message.AgentMemoryPersistMessage;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemoryMessageProducer;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentShortTermMemory;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 短期记忆持久化 Hook（AFTER_AGENT）
 * 每轮完整推理结束后，从 OverAllState 中提取本轮 HumanMessage + AssistantMessage 写入 MySQL 和 Redis
 */
@Slf4j
@Component
@HookPositions({HookPosition.AFTER_AGENT})
public class ShortTermRedisSaveHook extends AgentHook {

    private static final String USER_ID_KEY = "user_id";
    private static final String MESSAGES_KEY = "messages";

    private final AgentShortTermMemory shortTermMemory;
    private final AgentMemoryMessageProducer memoryMessageProducer;

    public ShortTermRedisSaveHook(AgentShortTermMemory shortTermMemory, AgentMemoryMessageProducer memoryMessageProducer) {
        this.shortTermMemory = shortTermMemory;
        this.memoryMessageProducer = memoryMessageProducer;
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        String sessionId = config.threadId().orElse(null);
        String userId = config.metadata(USER_ID_KEY).map(Object::toString).orElse(null);

        if (sessionId == null || userId == null) {
            log.warn("[short-term-save-hook] 缺少 sessionId 或 userId，跳过记忆写入");
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = state.<List<Message>>value(MESSAGES_KEY).orElse(List.of());

        // 从末尾向前查找本轮最后一条 UserMessage 和纯文本 AssistantMessage
        UserMessage lastHuman = null;
        AssistantMessage lastAssistant = null;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (lastAssistant == null && msg instanceof AssistantMessage am && !am.hasToolCalls() && StringUtils.isNotBlank(am.getText())) {
                lastAssistant = am;
            }
            if (lastHuman == null && msg instanceof UserMessage) {
                lastHuman = (UserMessage) msg;
            }
            if (lastHuman != null && lastAssistant != null) {
                break;
            }
        }

        if (lastHuman == null || lastAssistant == null) {
            log.debug("[short-term-save-hook] 未找到完整消息对，跳过写入, sessionId: {}", sessionId);
            return CompletableFuture.completedFuture(Map.of());
        }
        log.info("[short-term-save-hook] 本轮消息提取完成, sessionId: {}, human 长度: {}, assistant 长度: {}", sessionId, lastHuman.getText().length(), lastAssistant.getText().length());

        try {
            throw new RuntimeException("[TEST] 强制模拟 MySQL 写入失败");  // 测试完成后删除
            // shortTermMemory.saveMessages(sessionId, userId, List.of(lastHuman, lastAssistant));
            // log.info("[short-term-save-hook] 本轮消息已持久化, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("[short-term-save-hook] MySQL 写入失败，降级投递 RocketMQ, sessionId: {}", sessionId, e);
            fallbackToMq(sessionId, userId, lastHuman.getText(), lastAssistant.getText());
        }

        return CompletableFuture.completedFuture(Map.of());  // 返回空 Map 表示不修改 OverAllState
    }

    @Override
    public String getName() {
        return "ShortTermRedisSaveHook";
    }

    /**
     * MQ 兜底投递
     * 异步发送，不阻塞 Hook 返回；投递本身失败时由 Producer 记录 ERROR 日志
     */
    private void fallbackToMq(String sessionId, String userId, String humanContent, String assistantContent) {
        AgentMemoryPersistMessage message = AgentMemoryPersistMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .humanContent(humanContent)
                .assistantContent(assistantContent)
                .originTimestamp(System.currentTimeMillis())
                .build();
        memoryMessageProducer.sendPersistMessage(message);
        log.info("[short-term-save-hook] 记忆持久化消息已投递 RocketMQ, sessionId: {}", sessionId);
    }
}