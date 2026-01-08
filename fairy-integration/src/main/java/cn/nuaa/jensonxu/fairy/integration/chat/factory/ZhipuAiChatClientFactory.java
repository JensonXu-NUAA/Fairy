package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.integration.chat.advisor.ChatMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.integration.chat.data.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.fairy.repository.mysql.chat.CustomChatMemoryRepository;
import com.alibaba.nacos.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZhipuAiChatClientFactory implements ChatClientFactory {

    private final CustomChatMemoryRepository repository;

    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    @Autowired
    public ZhipuAiChatClientFactory(CustomChatMemoryRepository repository) {
        this.repository = repository;
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

        ChatMemory chatMemory = new InSqlMemory(repository);
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

        // 创建并返回 ChatClient
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new ChatMemoryAdvisor(chatMemory),
                        new PersistenceMemoryAdvisor(chatMemory, modelConfig.getModelName())
                )
                .build();
    }

    @Override
    public String getProviderName() {
        return "zhipuai";
    }
}
