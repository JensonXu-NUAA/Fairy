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
     * 插入或更新长期记忆
     * MySQL 唯一索引作为最终兜底
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
                            .set(AgentMemoryDO::getContent, record.getContent())
                            .set(AgentMemoryDO::getCategory, record.getCategory())
                            .set(AgentMemoryDO::getSource, record.getSource())
                            .set(AgentMemoryDO::getImportance, record.getImportance())
                            .set(AgentMemoryDO::getSourceSessionId, record.getSourceSessionId())
                            .set(AgentMemoryDO::getUpdatedAt, record.getUpdatedAt())
            );
        }
    }

    /**
     * 查询用户长期记忆，按重要度降序，过滤软删除记录
     */
    public List<AgentMemoryDO> findByUserId(String userId, int limit) {
        return mapper.selectList(
                new LambdaQueryWrapper<AgentMemoryDO>()
                        .eq(AgentMemoryDO::getUserId, userId)
                        .eq(AgentMemoryDO::getIsDeleted, 0)
                        .orderByDesc(AgentMemoryDO::getImportance)
                        .last("LIMIT " + limit)
        );
    }
}