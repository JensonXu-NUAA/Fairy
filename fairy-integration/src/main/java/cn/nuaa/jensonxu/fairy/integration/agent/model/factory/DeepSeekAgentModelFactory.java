package cn.nuaa.jensonxu.fairy.integration.agent.model.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName())
                .maxTokens(params.getMaxTokens())
                .temperature(params.getTemperature())
                .topP(params.getTopP())
                .build();

        // 思考模式配置，透传给 DeepSeekThinkingChatModel
        Map<String, Object> thinkingConfig = new HashMap<>();
        if (Boolean.TRUE.equals(params.getEnableThinking())) {
            thinkingConfig.put("type", "enabled");
        }

        DeepSeekThinkingChatModel model = DeepSeekThinkingChatModel.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .defaultOptions(options)
                .thinkingConfig(thinkingConfig)
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