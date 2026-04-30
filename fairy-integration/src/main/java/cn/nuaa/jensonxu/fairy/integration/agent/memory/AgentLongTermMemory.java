package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentMemoryRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentMemoryDO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 长期记忆管理
 * 基于 MySQL 存储用户跨会话的关键事实与偏好
 * 采用索引清单 + 按需召回的两阶段注入机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLongTermMemory {

    private final AgentMemoryRepository agentMemoryRepository;

    /**
     * 记忆索引清单，包含清单文本和合法 key 集合
     * 供 LongTermMemoryInterceptor 传给 AgentMemoryRecaller 使用
     */
    public record MemoryManifest(String text, Set<String> validKeys) {
        public boolean isEmpty() {
            return validKeys.isEmpty();
        }
    }

    /**
     * 构建用户记忆索引清单（不含 content 字段）
     * 每行格式：memory_key | category | name: description | 更新时间
     *
     * @param userId 用户 ID
     * @return MemoryManifest，包含清单文本和合法 key 集合；无记忆时 isEmpty() 为 true
     */
    public MemoryManifest buildMemoryManifest(String userId) {
        if (StringUtils.isBlank(userId)) {
            return new MemoryManifest("", Set.of());
        }

        List<AgentMemoryDO> indexList = agentMemoryRepository.findIndexByUserId(userId);
        if (indexList.isEmpty()) {
            return new MemoryManifest("", Set.of());
        }

        StringBuilder sb = new StringBuilder();
        for (AgentMemoryDO item : indexList) {
            sb.append(item.getMemoryKey())
                    .append(" | ").append(item.getCategory())
                    .append(" | ").append(item.getName()).append(": ").append(item.getDescription())
                    .append(" | ").append(formatAge(item.getUpdatedAt()))
                    .append("\n");
        }

        Set<String> validKeys = indexList.stream()
                .map(AgentMemoryDO::getMemoryKey)
                .collect(Collectors.toSet());

        log.debug("[long-term] 构建记忆清单, userId: {}, 共 {} 条", userId, indexList.size());
        return new MemoryManifest(sb.toString().trim(), validKeys);
    }

    /**
     * 按 memoryKey 列表加载完整记忆内容
     * 供 LongTermMemoryInterceptor 在召回后加载详细内容使用
     *
     * @param userId 用户 ID
     * @param keys   召回器选出的 memoryKey 列表
     * @return 含完整 content 的记忆列表
     */
    public List<AgentMemoryDO> loadSelectedMemories(String userId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return agentMemoryRepository.findContentByKeys(userId, keys);
    }

    /**
     * 将已选中的记忆列表拼装为 System Prompt 前缀文本
     * 按 category 分组展示：用户背景 / 行为反馈 / 项目约定 / 参考资料
     *
     * @param selectedMemories 召回并加载完整内容的记忆列表
     * @return 可直接拼接到 systemPrompt 头部的文本，无内容时返回空字符串
     */
    public String buildSystemPromptPrefix(List<AgentMemoryDO> selectedMemories) {
        if (selectedMemories == null || selectedMemories.isEmpty()) {
            return "";
        }

        // 按 category 分组，保持插入顺序
        Map<String, List<AgentMemoryDO>> grouped = selectedMemories.stream()
                .collect(Collectors.groupingBy(AgentMemoryDO::getCategory,
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, String> categoryLabels = Map.of(
                "user",      "[用户背景]",
                "feedback",  "[行为反馈]",
                "project",   "[项目约定]",
                "reference", "[参考资料]"
        );

        StringBuilder sb = new StringBuilder();
        // 按固定顺序输出，保证 System Prompt 结构稳定
        for (String category : List.of("user", "feedback", "project", "reference")) {
            List<AgentMemoryDO> group = grouped.get(category);
            if (group == null || group.isEmpty()) continue;
            sb.append(categoryLabels.getOrDefault(category, "[" + category + "]")).append("\n");
            for (AgentMemoryDO mem : group) {
                sb.append("- ").append(mem.getContent()).append("\n");
            }
            sb.append("\n");
        }

        log.debug("[long-term] 构建 System Prompt 前缀, 共 {} 条记忆", selectedMemories.size());
        return sb.toString().trim();
    }

    /**
     * 保存单条长期记忆（upsert）
     *
     * @param userId          用户 ID
     * @param memoryKey       记忆唯一标签
     * @param name            记忆名称（索引展示用）
     * @param description     一句话摘要（索引展示用）
     * @param category        分类：user / feedback / project / reference
     * @param content         记忆完整内容
     * @param sourceSessionId 来源会话 ID
     */
    public void saveMemory(String userId, String memoryKey, String name, String description,
                           String category, String content, String sourceSessionId) {
        LocalDateTime now = LocalDateTime.now();
        AgentMemoryDO record = AgentMemoryDO.builder()
                .userId(userId)
                .memoryKey(memoryKey)
                .name(name)
                .description(description)
                .category(category)
                .content(content)
                .sourceSessionId(sourceSessionId)
                .isDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        agentMemoryRepository.upsert(record);
        log.debug("[long-term] 保存长期记忆, userId: {}, key: {}", userId, memoryKey);
    }

    /**
     * 将 updatedAt 格式化为相对时间描述
     */
    private String formatAge(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return "未知时间";
        }
        long days = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());
        if (days == 0) return "今天更新";
        if (days < 30) return days + "天前更新";

        long months = ChronoUnit.MONTHS.between(updatedAt, LocalDateTime.now());
        if (months < 12) return months + "个月前更新";
        return ChronoUnit.YEARS.between(updatedAt, LocalDateTime.now()) + "年前更新";
    }
}