package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 会话启动阶段的记忆加载结果
 * @param shortTermMessages  从 Redis/MySQL 加载的短期记忆消息列表，用于回填 MemorySaver
 */
public record AgentLoadedContext(List<Message> shortTermMessages) {}