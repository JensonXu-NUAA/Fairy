package cn.nuaa.jensonxu.fairy.common.data.llm;

import lombok.Data;

@Data
public class ModelConfig {
    private String modelName;
    private String provider;
    private String apiKey;
    private String baseUrl;
    private Boolean enabled;
    private Parameters parameters;

    @Data
    public static class Parameters {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
    }
}
