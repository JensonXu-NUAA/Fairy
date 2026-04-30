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
 * Agent 用户长期记忆
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

    /** 记忆标签，唯一标识一条记忆，如 api_naming_convention */
    @TableField("memory_key")
    private String memoryKey;

    /** 记忆名称，简短展示用，如"API命名约定" */
    @TableField("name")
    private String name;

    /** 一句话摘要，用于索引展示，如"接口字段统一使用驼峰命名" */
    @TableField("description")
    private String description;

    /**
     * 记忆分类：
     * user       - 用户背景、身份、跨项目个人特征
     * feedback   - 用户对 AI 行为的评价，哪些该做哪些不该做
     * project    - 当前项目的决策、约定、上下文
     * reference  - 外部资源指针（文档链接等）
     */
    @TableField("category")
    private String category;

    /** 记忆完整内容 */
    @TableField("content")
    private String content;

    /** 最近一次更新该记忆的会话 ID，用于追溯 */
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