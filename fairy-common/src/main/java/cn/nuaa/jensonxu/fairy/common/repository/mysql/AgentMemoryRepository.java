package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentMemoryDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.AgentMemoryMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentMemoryRepository {

    private final AgentMemoryMapper mapper;

    /**
     * 插入或更新长期记忆（以 userId + memoryKey 为唯一键）
     */
    public void upsert(AgentMemoryDO record) {
        AgentMemoryDO existing = mapper.selectOne(
                new LambdaQueryWrapper<AgentMemoryDO>()
                        .eq(AgentMemoryDO::getUserId, record.getUserId())
                        .eq(AgentMemoryDO::getMemoryKey, record.getMemoryKey())
        );
        if (existing == null) {
            mapper.insert(record);
        } else {
            mapper.update(
                    new LambdaUpdateWrapper<AgentMemoryDO>()
                            .eq(AgentMemoryDO::getId, existing.getId())
                            .set(AgentMemoryDO::getName, record.getName())
                            .set(AgentMemoryDO::getDescription, record.getDescription())
                            .set(AgentMemoryDO::getCategory, record.getCategory())
                            .set(AgentMemoryDO::getContent, record.getContent())
                            .set(AgentMemoryDO::getSourceSessionId, record.getSourceSessionId())
                            .set(AgentMemoryDO::getUpdatedAt, record.getUpdatedAt())
            );
        }
    }

    /**
     * 查询用户记忆索引（不含 content 字段），按更新时间降序
     * 用于构建传给召回器的索引清单
     */
    public List<AgentMemoryDO> findIndexByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AgentMemoryDO>()
                        .select(AgentMemoryDO.class, info -> !info.getColumn().equals("content"))
                        .eq(AgentMemoryDO::getUserId, userId)
                        .eq(AgentMemoryDO::getIsDeleted, 0)
                        .orderByDesc(AgentMemoryDO::getUpdatedAt)
        );
    }

    /**
     * 按 memoryKey 列表查询完整记忆内容
     * 用于召回器选出相关 key 后加载详细内容注入 System Prompt
     */
    public List<AgentMemoryDO> findContentByKeys(String userId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(
                new LambdaQueryWrapper<AgentMemoryDO>()
                        .select(AgentMemoryDO::getMemoryKey, AgentMemoryDO::getCategory,
                                AgentMemoryDO::getName, AgentMemoryDO::getContent)
                        .eq(AgentMemoryDO::getUserId, userId)
                        .in(AgentMemoryDO::getMemoryKey, keys)
                        .eq(AgentMemoryDO::getIsDeleted, 0)
        );
    }

    /**
     * 查询用户全量长期记忆（含 content），按更新时间降序
     */
    public List<AgentMemoryDO> findByUserId(String userId, int limit) {
        return mapper.selectList(
                new LambdaQueryWrapper<AgentMemoryDO>()
                        .eq(AgentMemoryDO::getUserId, userId)
                        .eq(AgentMemoryDO::getIsDeleted, 0)
                        .orderByDesc(AgentMemoryDO::getUpdatedAt)
                        .last("LIMIT " + limit)
        );
    }
}