package cn.nuaa.jensonxu.fairy.common.data.llm.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentModelConfigFormDTO {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "提供商不能为空")
    private String provider;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    private String baseUrl;

    @NotNull(message = "启用状态不能为空")
    private Integer isEnabled;

    private Parameters parameters;

    @Data
    public static class Parameters {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Boolean enableThinking;
        private Integer thinkingBudget;
    }
}