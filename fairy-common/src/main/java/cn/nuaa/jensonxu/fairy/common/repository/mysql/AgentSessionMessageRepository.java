package cn.nuaa.jensonxu.fairy.common.repository.mysql;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSessionMessageDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper.AgentSessionMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentSessionMessageRepository {

    private final AgentSessionMessageMapper mapper;

    /**
     * 批量保存会话消息（每轮对话最多写入 2 条，循环 insert 性能可接受）
     */
    public void batchInsert(List<AgentSessionMessageDO> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        messages.forEach(mapper::insert);
    }

    /**
     * 按会话 ID 查询全量消息，用于 Redis 宕机后 fallback 回填
     */
    public List<AgentSessionMessageDO> findBySessionId(String sessionId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AgentSessionMessageDO>()
                        .eq(AgentSessionMessageDO::getSessionId, sessionId)
                        .orderByAsc(AgentSessionMessageDO::getSeq)
        );
    }

    /**
     * 查询会话消息总数，用于计算新消息的 seq 起始值
     */
    public int countBySessionId(String sessionId) {
        return Math.toIntExact(mapper.selectCount(
                new LambdaQueryWrapper<AgentSessionMessageDO>()
                        .eq(AgentSessionMessageDO::getSessionId, sessionId)
        ));
    }
}