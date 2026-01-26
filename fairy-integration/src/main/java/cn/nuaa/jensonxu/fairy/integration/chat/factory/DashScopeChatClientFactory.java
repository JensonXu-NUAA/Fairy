package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DashScopeChatClientFactory extends BaseChatClientFactory {

    @Autowired
    public DashScopeChatClientFactory(CustomChatMemoryRepository repository, ToolCallbackProvider provider) {
        super(repository, provider);
    }

    @Override
    public boolean supports(String provider) {
        return "dashscope".equalsIgnoreCase(provider);
    }

    @Override
    public ChatClient createChatClient(ModelConfig modelConfig) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("DashScope API Key 不能为空");
        }

        ModelConfig.Parameters params = modelConfig.getParameters();

        DashScopeApi api = DashScopeApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .build();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(modelConfig.getModelName())
                .maxToken(params.getMaxTokens())
                .temperature(params.getTemperature())
                .topP(params.getTopP())
                .build();
        DashScopeChatModel model = DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();

        return create(model, modelConfig);  // 创建并返回 ChatClient

    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }
}
