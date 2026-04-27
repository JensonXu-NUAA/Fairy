package cn.nuaa.jensonxu.fairy.integration.agent.model.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OpenAiAgentModelFactory extends BaseAgentModelFactory {

    @Override
    public boolean supports(String provider) {
        return "openai".equalsIgnoreCase(provider);
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] tools, BaseCheckpointSaver saver, List<Hook> hooks, List<Interceptor> interceptors) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[Agent-OpenAI] API Key 不能为空");
        }

        ModelConfig.Parameters params = modelConfig.getParameters();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(StringUtils.hasText(modelConfig.getBaseUrl()) ? modelConfig.getBaseUrl() : "https://api.openai.com")
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName())
                .maxTokens(params != null ? params.getMaxTokens() : null)
                .temperature(params != null ? params.getTemperature() : null)
                .topP(params != null ? params.getTopP() : null)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        return ReactAgent.builder()
                .name(modelConfig.getModelName())
                .model(model)
                .tools(tools)
                .saver(saver)
                .hooks(hooks)
                .interceptors(interceptors)
                .build();
    }
}