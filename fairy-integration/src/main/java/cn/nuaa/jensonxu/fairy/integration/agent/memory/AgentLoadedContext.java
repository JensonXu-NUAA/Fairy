package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 会话启动阶段的记忆加载结果
 *
 * @param shortTermMessages  从 Redis/MySQL 加载的短期记忆消息列表，用于回填 MemorySaver
 * @param systemPromptPrefix 由长期记忆构建的 System Prompt 前缀，直接注入 ReactAgent
 */
public record AgentLoadedContext(List<Message> shortTermMessages, String systemPromptPrefix) {}