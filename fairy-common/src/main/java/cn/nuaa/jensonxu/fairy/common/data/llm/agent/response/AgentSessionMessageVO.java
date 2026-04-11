package cn.nuaa.jensonxu.fairy.common.data.llm.agent.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话消息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionMessageVO {

    /** 消息在会话内的顺序号 */
    private Integer seq;

    /** 消息角色：human / assistant / tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息时间 */
    private LocalDateTime createdAt;
}