package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.common.repository.minio.MinioProperties;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;
import com.alibaba.nacos.common.utils.StringUtils;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZhipuAiChatClientFactory extends BaseChatClientFactory {

    @Autowired
    public ZhipuAiChatClientFactory(CustomChatMemoryRepository repository, ToolCallbackProvider provider,
                                    MinioClient minioClient, MinioProperties minioProperties) {
        super(repository, provider, minioClient, minioProperties);
    }


    @Override
    public boolean supports(String provider) {
        return "zhipuai".equals(provider);
    }

    @Override
    public ChatClient createChatClient(ModelConfig modelConfig) {
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalArgumentException("[ZhipuAi] API Key 不能为空");
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

        return create(model, modelConfig);  // 创建并返回 ChatClient
    }

    @Override
    public String getProviderName() {
        return "zhipuai";
    }
}
