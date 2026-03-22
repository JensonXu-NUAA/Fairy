package cn.nuaa.jensonxu.fairy.integration.agent.memory.hook;

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
 * 每轮完整推理结束后，从 OverAllState 中提取本轮 HumanMessage + AssistantMessage
 * 写入 MySQL 和 Redis，替代原 AgentHandler.saveRoundMessages() 的手动调用
 */
@Slf4j
@Component
@HookPositions({HookPosition.AFTER_AGENT})
public class ShortTermRedisSaveHook extends AgentHook {

    private static final String USER_ID_KEY = "user_id";
    private static final String MESSAGES_KEY = "messages";

    private final AgentShortTermMemory shortTermMemory;

    public ShortTermRedisSaveHook(AgentShortTermMemory shortTermMemory) {
        this.shortTermMemory = shortTermMemory;
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

        try {
            shortTermMemory.saveMessages(sessionId, userId, List.of(lastHuman, lastAssistant));
            log.info("[short-term-save-hook] 本轮消息已持久化, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("[short-term-save-hook] 短期记忆写入失败, sessionId: {}", sessionId, e);
        }

        // 返回空 Map 表示不修改 OverAllState
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public String getName() {
        return "ShortTermRedisSaveHook";
    }
}