package cn.nuaa.jensonxu.fairy.integration.agent.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DashScopeAgentModelFactory extends BaseAgentModelFactory {

    @Override
    public boolean supports(String provider) {
        return "dashscope".equalsIgnoreCase(provider);
    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] tools, BaseCheckpointSaver saver) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[agent] API Key 不能为空");
        }

        ModelConfig.Parameters params = modelConfig.getParameters();

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName())
                .maxTokens(params.getMaxTokens())
                .temperature(params.getTemperature())
                .topP(params.getTopP());

        // 思考模式：通过 additionalModelRequestFields 传入扩展参数
        if (Boolean.TRUE.equals(params.getEnableThinking())) {
            Map<String, Object> extraFields = new HashMap<>();
            extraFields.put("enable_thinking", true);
            if (params.getThinkingBudget() != null) {
                extraFields.put("thinking_budget", params.getThinkingBudget());
            }
            optionsBuilder.extraBody(extraFields);
        }

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();

        return ReactAgent.builder()
                .name(modelConfig.getModelName())
                .model(model)
                .tools(tools)
                .saver(saver)
                .build();
    }
}