package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSkillDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.AgentSkillMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AgentSkillRepository {

    private final AgentSkillMapper mapper;

    /**
     * 查询用户所有已启用的 skill
     */
    public List<AgentSkillDO> findEnabledByUserId(String userId) {
        return mapper.selectList(new LambdaQueryWrapper<AgentSkillDO>()
                .eq(AgentSkillDO::getUserId, userId)
                .eq(AgentSkillDO::getIsEnabled, 1)
                .eq(AgentSkillDO::getIsDeleted, 0));
    }

    /**
     * 按用户 ID + 技能名称查询单个 skill
     */
    public Optional<AgentSkillDO> findByUserIdAndSkillName(String userId, String skillName) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<AgentSkillDO>()
                .eq(AgentSkillDO::getUserId, userId)
                .eq(AgentSkillDO::getSkillName, skillName)
                .eq(AgentSkillDO::getIsDeleted, 0)));
    }

    /**
     * 插入新 skill
     */
    public void insert(AgentSkillDO skillDO) {
        mapper.insert(skillDO);
    }

    /**
     * 软删除 skill
     */
    public void softDelete(String userId, String skillName) {
        mapper.update(new LambdaUpdateWrapper<AgentSkillDO>()
                .eq(AgentSkillDO::getUserId, userId)
                .eq(AgentSkillDO::getSkillName, skillName)
                .set(AgentSkillDO::getIsDeleted, 1)
                .set(AgentSkillDO::getUpdatedAt, LocalDateTime.now()));
    }
}