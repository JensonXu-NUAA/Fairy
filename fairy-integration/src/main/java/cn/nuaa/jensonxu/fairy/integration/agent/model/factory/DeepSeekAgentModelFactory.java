package cn.nuaa.jensonxu.fairy.integration.agent.model.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeepSeekAgentModelFactory extends BaseAgentModelFactory {

    @Override
    public boolean supports(String provider) {
        return "deepseek".equalsIgnoreCase(provider);
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }

    @Override
    public ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] tools, BaseCheckpointSaver saver, List<Hook> hooks, List<Interceptor> interceptors) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[Agent-DeepSeek] API Key 不能为空");
        }

        ModelConfig.Parameters params = modelConfig.getParameters();

        DeepSeekApi api = DeepSeekApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                .maxTokens(params.getMaxTokens())
                .temperature(params.getTemperature())
                .topP(params.getTopP())
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(api)
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