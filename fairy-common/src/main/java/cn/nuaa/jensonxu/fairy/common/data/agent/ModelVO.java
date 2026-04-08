package cn.nuaa.jensonxu.fairy.common.data.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可用模型列表 VO，供前端展示模型选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVO {

    /** 模型名称，发起对话请求时传入此值 */
    private String modelName;

    /** 服务商（DashScope / DeepSeek / ZhipuAI 等） */
    private String provider;

    /** 是否支持思考模式，前端据此决定是否显示 thinking 开关 */
    private Boolean enableThinking;
}