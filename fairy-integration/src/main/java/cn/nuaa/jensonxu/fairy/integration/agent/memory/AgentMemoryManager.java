package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 记忆管理统一入口
 * 编排短期记忆的加载与压缩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMemoryManager {

    private final AgentShortTermMemory shortTermMemory;
    private final AgentContextCompressor compressor;
    private final AgentProperties agentProperties;

    /**
     * 会话启动阶段：加载短期记忆
     * 若消息数超过窗口上限，先执行压缩再返回
     */
    public AgentLoadedContext loadContext(String sessionId, String userId) {
        log.info("[memory] 加载会话上下文, sessionId: {}, userId: {}", sessionId, userId);
        List<Message> messages = shortTermMemory.loadMessages(sessionId);
        int maxMessages = agentProperties.getMemory().getShortTerm().getMaxMessages();

        if (messages.size() > maxMessages) {
            log.info("[memory] 消息数 {} 超过阈值 {}，触发压缩, sessionId: {}", messages.size(), maxMessages, sessionId);
            messages = compressMessages(messages, sessionId);
        }

        log.info("[memory] 上下文加载完成，短期记忆 {} 条, sessionId: {}", messages.size(), sessionId);
        return new AgentLoadedContext(messages);
    }

    /**
     * 上下文压缩：将早期消息提炼为纯文本摘要，以摘要消息替换原始消息
     * 压缩结果仅用于维持当前会话上下文连贯性，不写入任何数据库
     *
     * @return 压缩后的消息列表（摘要消息 + 近期消息）
     */
    private List<Message> compressMessages(List<Message> messages, String sessionId) {
        int compressCount = agentProperties.getMemory().getShortTerm().getMaxMessages() / 2;
        List<Message> toCompress = messages.subList(0, compressCount);
        List<Message> toKeep = new ArrayList<>(messages.subList(compressCount, messages.size()));

        String summary = compressor.compress(toCompress, sessionId);

        if (StringUtils.isNotBlank(summary)) {
            Message summaryMsg = new AssistantMessage("【早期对话摘要】\n" + summary);  // 以摘要消息作为历史占位，插入保留消息列表的头部
            toKeep.addFirst(summaryMsg);
            log.info("[memory] 压缩完成，摘要消息已注入, sessionId: {}", sessionId);
        } else {
            log.warn("[memory] 摘要生成失败，直接丢弃早期消息, sessionId: {}", sessionId);
        }

        shortTermMemory.replaceMessages(sessionId, toKeep);
        log.info("[memory] Redis 已更新为 {} 条消息, sessionId: {}", toKeep.size(), sessionId);
        return toKeep;
    }
}