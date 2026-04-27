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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_model_config")
public class AgentModelConfigDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("model_name")
    private String modelName;

    @TableField("provider")
    private String provider;

    @TableField("api_key")
    private String apiKey;

    @TableField("base_url")
    private String baseUrl;

    @TableField("is_enabled")
    private Integer isEnabled;

    /** 推理参数，序列化为 JSON 字符串存储 */
    @TableField("parameters")
    private String parameters;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}