package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentModelConfigDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.AgentModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AgentModelConfigRepository {

    private final AgentModelConfigMapper mapper;

    public void insert(AgentModelConfigDO record) {
        mapper.insert(record);
    }

    public void updateById(AgentModelConfigDO record) {
        mapper.updateById(record);
    }

    /** 软删除 */
    public void deleteById(Long id) {
        mapper.update(
                new LambdaUpdateWrapper<AgentModelConfigDO>()
                        .eq(AgentModelConfigDO::getId, id)
                        .set(AgentModelConfigDO::getIsDeleted, 1)
        );
    }

    /** 按 ID 查询（过滤软删除） */
    public Optional<AgentModelConfigDO> findById(Long id) {
        return Optional.ofNullable(
                mapper.selectOne(
                        new LambdaQueryWrapper<AgentModelConfigDO>()
                                .eq(AgentModelConfigDO::getId, id)
                                .eq(AgentModelConfigDO::getIsDeleted, 0)
                )
        );
    }

    /** 查询用户全部未删除配置 */
    public List<AgentModelConfigDO> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AgentModelConfigDO>()
                        .eq(AgentModelConfigDO::getUserId, userId)
                        .eq(AgentModelConfigDO::getIsDeleted, 0)
                        .orderByDesc(AgentModelConfigDO::getCreatedAt)
        );
    }

    /** 按用户 + 模型名称精确查询，供 AgentModelManager 加载单条配置使用 */
    public Optional<AgentModelConfigDO> findByUserIdAndModelName(String userId, String modelName) {
        return Optional.ofNullable(
                mapper.selectOne(
                        new LambdaQueryWrapper<AgentModelConfigDO>()
                                .eq(AgentModelConfigDO::getUserId, userId)
                                .eq(AgentModelConfigDO::getModelName, modelName)
                                .eq(AgentModelConfigDO::getIsDeleted, 0)
                )
        );
    }

    /** 校验同一用户下模型名称是否已存在（用于新增时的唯一性校验） */
    public boolean existsByUserIdAndModelName(String userId, String modelName) {
        return mapper.exists(
                new LambdaQueryWrapper<AgentModelConfigDO>()
                        .eq(AgentModelConfigDO::getUserId, userId)
                        .eq(AgentModelConfigDO::getModelName, modelName)
                        .eq(AgentModelConfigDO::getIsDeleted, 0)
        );
    }
}