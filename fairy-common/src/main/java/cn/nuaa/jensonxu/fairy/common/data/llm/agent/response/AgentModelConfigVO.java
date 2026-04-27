package cn.nuaa.jensonxu.fairy.common.data.llm.agent.response;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentModelConfigDO;
import com.alibaba.fastjson2.JSON;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentModelConfigVO {

    private Long id;
    private String userId;
    private String modelName;
    private String provider;
    private String apiKeyMasked;  // apiKey 脱敏，仅展示后4位
    private String baseUrl;
    private Integer isEnabled;
    private Parameters parameters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class Parameters {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Boolean enableThinking;
        private Integer thinkingBudget;
    }

    public static AgentModelConfigVO from(AgentModelConfigDO record) {
        Parameters parameters = null;
        if (record.getParameters() != null) {
            parameters = JSON.parseObject(record.getParameters(), Parameters.class);
        }

        String masked = null;
        if (record.getApiKey() != null && record.getApiKey().length() >= 4) {
            masked = "****" + record.getApiKey().substring(record.getApiKey().length() - 4);
        }

        return AgentModelConfigVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .modelName(record.getModelName())
                .provider(record.getProvider())
                .apiKeyMasked(masked)
                .baseUrl(record.getBaseUrl())
                .isEnabled(record.getIsEnabled())
                .parameters(parameters)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}