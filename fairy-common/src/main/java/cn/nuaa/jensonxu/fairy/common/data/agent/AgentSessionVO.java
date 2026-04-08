package cn.nuaa.jensonxu.fairy.common.data.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionVO {

    /** 会话唯一ID */
    private String sessionId;

    /** 会话标题，轻量模型异步生成，可能为 null（前端展示兜底文案） */
    private String title;

    /** 本次会话使用的模型 */
    private String modelName;

    /** 会话创建时间 */
    private LocalDateTime createdAt;

    /** 最后活跃时间 */
    private LocalDateTime updatedAt;
}