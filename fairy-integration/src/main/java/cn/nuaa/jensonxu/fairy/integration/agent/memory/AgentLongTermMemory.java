package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentMemoryRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentMemoryDO;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 长期记忆管理
 * 基于 MySQL 存储用户跨会话的关键事实与偏好，在每次对话启动时注入 System Prompt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLongTermMemory {

    private final AgentMemoryRepository agentMemoryRepository;
    private final AgentProperties agentProperties;

    /**
     * 构建注入 System Prompt 的长期记忆前缀
     * 按 importance 降序取前 maxFactsPerUser 条，拼接为文本块
     * 若该用户无任何长期记忆，返回空字符串
     *
     * @param userId 用户 ID
     * @return System Prompt 前缀字符串，可直接拼接到 systemPrompt 头部
     */
    public String buildSystemPromptPrefix(String userId) {
        if (StringUtils.isBlank(userId)) {
            return "";
        }

        int maxFacts = agentProperties.getMemory().getLongTerm().getMaxFactsPerUser();
        List<AgentMemoryDO> memories = agentMemoryRepository.findByUserId(userId, maxFacts);

        if (memories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[关于用户的长期记忆]\n");
        for (AgentMemoryDO memory : memories) {
            sb.append("- ").append(memory.getContent()).append("\n");
        }
        sb.append("\n");

        log.debug("[agent] 注入长期记忆 {} 条, userId: {}", memories.size(), userId);
        return sb.toString();
    }

    /**
     * 保存单条长期记忆（upsert）
     * 由 AgentMemorySummarizer 提炼完成后调用
     *
     * @param userId          用户 ID
     * @param memoryKey       记忆标签
     * @param content         记忆内容
     * @param importance      重要度 1-10
     * @param sourceSessionId 来源会话 ID
     */
    public void saveMemory(String userId, String memoryKey, String content, int importance, String sourceSessionId) {
        LocalDateTime now = LocalDateTime.now();
        AgentMemoryDO record = AgentMemoryDO.builder()
                .userId(userId)
                .memoryKey(memoryKey)
                .content(content)
                .source("auto")
                .importance(importance)
                .sourceSessionId(sourceSessionId)
                .isDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        agentMemoryRepository.upsert(record);
        log.debug("[agent] 保存长期记忆, userId: {}, key: {}", userId, memoryKey);
    }
}