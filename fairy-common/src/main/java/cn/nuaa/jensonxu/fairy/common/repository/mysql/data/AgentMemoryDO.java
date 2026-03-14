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
 * Agent 用户长期记忆实体
 * 对应表：agent_user_memory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_user_memory")
public class AgentMemoryDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    @TableField("user_id")
    private String userId;

    /** 记忆标签，如 user_language_preference */
    @TableField("memory_key")
    private String memoryKey;

    /** 记忆内容 */
    @TableField("content")
    private String content;

    /** 来源：auto=摘要自动提炼，manual=用户显式保存 */
    @TableField("source")
    private String source;

    /** 重要度 1-10，数值越高越优先注入 System Prompt */
    @TableField("importance")
    private Integer importance;

    /** 最近一次提炼该记忆的会话 ID，用于追溯 */
    @TableField("source_session_id")
    private String sourceSessionId;

    /** 软删除标记：0=正常，1=已删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}