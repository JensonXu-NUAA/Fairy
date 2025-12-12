package cn.nuaa.jensonxu.integration.chat.factory;

import cn.nuaa.jensonxu.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.integration.chat.data.ModelConfig;
import cn.nuaa.jensonxu.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.repository.mysql.chat.CustomChatMemoryRepository;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.nacos.common.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DashScopeChatClientFactory implements ChatClientFactory {

    private final CustomChatMemoryRepository repository;

    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    @Autowired
    public DashScopeChatClientFactory(CustomChatMemoryRepository repository) {
        this.repository = repository;
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
                .withModel(modelConfig.getModelName())
                .withMaxToken(params.getMaxTokens())
                .withTemperature(params.getTemperature())
                .withTopP(params.getTopP())
                .build();
        DashScopeChatModel model = DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();
        ChatMemory chatMemory = new InSqlMemory(repository);

        // 创建并返回 ChatClient
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new PersistenceMemoryAdvisor(chatMemory)
                )
                .build();

    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }
}
