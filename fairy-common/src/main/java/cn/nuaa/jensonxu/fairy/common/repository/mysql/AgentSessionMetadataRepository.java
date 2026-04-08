package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSessionMetadataDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.AgentSessionMetadataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentSessionMetadataRepository {

    private final AgentSessionMetadataMapper mapper;

    /**
     * 新建会话元数据，首条消息时调用
     */
    public void insert(AgentSessionMetadataDO metadata) {
        mapper.insert(metadata);
    }

    /**
     * 更新会话最后活跃时间，每轮对话后调用
     */
    public void updateActiveTime(String sessionId) {
        mapper.update(null, new LambdaUpdateWrapper<AgentSessionMetadataDO>()
                .eq(AgentSessionMetadataDO::getSessionId, sessionId)
                .set(AgentSessionMetadataDO::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 更新会话标题，轻量模型异步生成后回填
     */
    public void updateTitle(String sessionId, String title) {
        mapper.update(null, new LambdaUpdateWrapper<AgentSessionMetadataDO>()
                .eq(AgentSessionMetadataDO::getSessionId, sessionId)
                .set(AgentSessionMetadataDO::getTitle, title));
    }

    /**
     * 按用户 ID 查询所有会话，按最后活跃时间倒序
     */
    public List<AgentSessionMetadataDO> findByUserId(String userId) {
        return mapper.selectList(new LambdaQueryWrapper<AgentSessionMetadataDO>()
                .eq(AgentSessionMetadataDO::getUserId, userId)
                .orderByDesc(AgentSessionMetadataDO::getUpdatedAt));
    }

    /**
     * 查询单个会话元数据
     */
    public AgentSessionMetadataDO findBySessionId(String sessionId) {
        return mapper.selectOne(new LambdaQueryWrapper<AgentSessionMetadataDO>()
                .eq(AgentSessionMetadataDO::getSessionId, sessionId));
    }

    /**
     * 判断会话是否已存在，用于区分首条消息与后续消息
     */
    public boolean existsBySessionId(String sessionId) {
        return mapper.selectCount(new LambdaQueryWrapper<AgentSessionMetadataDO>()
                .eq(AgentSessionMetadataDO::getSessionId, sessionId)) > 0;
    }
}