package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;

import com.alibaba.nacos.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeepSeekChatClientFactory extends BaseChatClientFactory {

    protected DeepSeekChatClientFactory(CustomChatMemoryRepository repository, ToolCallbackProvider provider) {
        super(repository, provider);
    }

    @Override
    public boolean supports(String provider) {
        return "deepseek".equalsIgnoreCase(provider);
    }

    @Override
    public ChatClient createChatClient(ModelConfig modelConfig) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[DeepSeek] API Key 不能为空");
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

        return create(model, modelConfig);  // 创建并返回 ChatClient
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }
}
