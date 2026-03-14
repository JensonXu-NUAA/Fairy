package cn.nuaa.jensonxu.fairy.integration.agent.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZhipuAiAgentModelFactory extends BaseAgentModelFactory {

    @Override
    public boolean supports(String provider) {
        return "zhipuai".equalsIgnoreCase(provider);
    }

    @Override
    public String getProviderName() {
        return "zhipuai";
    }

    @Override
    public ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] tools, BaseCheckpointSaver saver) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[Agent-ZhipuAi] API Key 不能为空");
        }

        ModelConfig.Parameters params = modelConfig.getParameters();

        ZhiPuAiApi api = ZhiPuAiApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .build();

        ZhiPuAiChatOptions options = ZhiPuAiChatOptions.builder()
                .model(ZhiPuAiApi.ChatModel.GLM_4_5_Flash.getValue())
                .maxTokens(params.getMaxTokens())
                .temperature(params.getTemperature())
                .topP(params.getTopP())
                .build();

        ZhiPuAiChatModel model = new ZhiPuAiChatModel(api, options);

        return ReactAgent.builder()
                .name(modelConfig.getModelName())
                .model(model)
                .tools(tools)
                .saver(saver)
                .build();
    }
}