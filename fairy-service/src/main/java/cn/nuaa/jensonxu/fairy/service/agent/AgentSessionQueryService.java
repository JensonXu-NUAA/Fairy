package cn.nuaa.jensonxu.fairy.service.agent;

import cn.nuaa.jensonxu.fairy.common.data.llm.AgentSessionMessageVO;
import cn.nuaa.jensonxu.fairy.common.data.llm.AgentSessionVO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSessionMessageRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSessionMetadataRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSessionMessageDO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSessionMetadataDO;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSessionQueryService {

    private final AgentSessionMetadataRepository metadataRepository;
    private final AgentSessionMessageRepository messageRepository;

    /**
     * 查询用户的所有会话列表，按最后活跃时间倒序
     */
    public List<AgentSessionVO> listSessions(String userId) {
        List<AgentSessionMetadataDO> records = metadataRepository.findByUserId(userId);
        return records.stream()
                .map(r -> AgentSessionVO.builder()
                        .sessionId(r.getSessionId())
                        .title(r.getTitle())
                        .modelName(r.getModelName())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();
    }

    /**
     * 查询某会话的完整消息记录，按 seq 升序
     */
    public List<AgentSessionMessageVO> listMessages(String sessionId) {
        List<AgentSessionMessageDO> records = messageRepository.findBySessionId(sessionId);
        return records.stream()
                .map(r -> AgentSessionMessageVO.builder()
                        .seq(r.getSeq())
                        .role(r.getRole())
                        .content(extractContent(r.getContent()))
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }

    private String extractContent(String rawContent) {
        try {
            JSONObject json = JSON.parseObject(rawContent);
            String content = json.getString("content");
            return content != null ? content : rawContent;
        } catch (Exception e) {
            return rawContent;  // 解析失败则返回原始内容
        }
    }
}