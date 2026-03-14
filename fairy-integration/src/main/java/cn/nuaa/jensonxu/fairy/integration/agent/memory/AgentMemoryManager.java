package cn.nuaa.jensonxu.fairy.integration.agent.memory;

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

    /**
     * 会话启动阶段：加载短期记忆 + 构建长期记忆注入文本
     * 由 AgentService 在构建 ReactAgent 前调用
     *
     * @param sessionId 会话 ID（agentSessionId）
     * @param userId    用户 ID
     * @return 包含历史消息列表和 System Prompt 前缀的上下文对象
     */
    public AgentLoadedContext loadContext(String sessionId, String userId) {
        log.info("[memory] 加载会话上下文, sessionId: {}, userId: {}", sessionId, userId);
        List<Message> messages = shortTermMemory.loadMessages(sessionId);
        String systemPromptPrefix = longTermMemory.buildSystemPromptPrefix(userId);
        log.info("[memory] 上下文加载完成，短期记忆 {} 条，长期记忆注入长度 {} 字符", messages.size(), systemPromptPrefix.length());
        return new AgentLoadedContext(messages, systemPromptPrefix);
    }

    /**
     * 每轮推理结束后：将本轮新增的 HumanMessage + AssistantMessage 写入短期记忆
     * 由 AgentHandler 在流式推理完成后调用
     *
     * @param sessionId        会话 ID
     * @param userId           用户 ID
     * @param humanMessage     本轮用户消息
     * @param assistantMessage 本轮 Agent 回复消息
     */
    public void saveRoundMessages(String sessionId, String userId, Message humanMessage, Message assistantMessage) {
        shortTermMemory.saveMessages(sessionId, userId, List.of(humanMessage, assistantMessage));
        log.debug("[memory] 本轮消息已持久化, sessionId: {}", sessionId);
    }

    /**
     * 会话显式结束时回调
     * Phase 1：仅记录日志，占位
     * Phase 2：触发对 Redis 剩余消息的最终摘要提炼，结果写入 MySQL 长期记忆
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     */
    public void onSessionEnd(String sessionId, String userId) {
        log.info("[memory] 会话结束, sessionId: {}, userId: {} —— Phase 2 摘要待实现", sessionId, userId);
    }
}