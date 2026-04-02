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
 * 用户自定义 Agent 技能实体
 * 对应表：agent_skill
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_user_skill")
public class AgentSkillDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    @TableField("user_id")
    private String userId;

    /** 技能唯一标识，格式：小写字母、数字、连字符，最长 64 字符 */
    @TableField("skill_name")
    private String skillName;

    /** 技能简介，注入 System Prompt 供模型感知 */
    @TableField("description")
    private String description;

    /** SKILL.md 在 MinIO 中的完整路径，格式：skills/{userId}/{skillName}/SKILL.md */
    @TableField("minio_path")
    private String minioPath;

    /** 是否启用：1-启用，0-禁用 */
    @TableField("is_enabled")
    private Integer isEnabled;

    /** 软删除标记：0-正常，1-已删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}