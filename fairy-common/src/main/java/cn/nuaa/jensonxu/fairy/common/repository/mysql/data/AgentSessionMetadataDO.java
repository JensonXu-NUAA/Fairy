package cn.nuaa.jensonxu.fairy.common.repository.mysql.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 会话元数据表实体
 * 对应表：agent_session_metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_session_metadata")
public class AgentSessionMetadataDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话唯一ID，对应 agentSessionId */
    @TableField("session_id")
    private String sessionId;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 会话标题，由轻量模型异步生成 */
    @TableField("title")
    private String title;

    /** 本次会话使用的模型 */
    @TableField("model_name")
    private String modelName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 最后活跃时间，每轮对话更新 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}