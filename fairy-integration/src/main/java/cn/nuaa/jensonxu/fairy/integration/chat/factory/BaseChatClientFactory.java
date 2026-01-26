package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.integration.chat.advisor.ChatMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;

public abstract class BaseChatClientFactory {

    protected final CustomChatMemoryRepository repository;

    protected final ToolCallbackProvider provider;

    protected static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    protected BaseChatClientFactory(CustomChatMemoryRepository repository, ToolCallbackProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    public abstract boolean supports(String provider);

    public abstract ChatClient createChatClient(ModelConfig modelConfig);

    public abstract String getProviderName();

    protected ChatClient create(ChatModel model, ModelConfig modelConfig) {
        ChatMemory chatMemory = new InSqlMemory(repository);
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new ChatMemoryAdvisor(chatMemory),
                        new PersistenceMemoryAdvisor(chatMemory, modelConfig.getModelName())
                )
                .defaultToolCallbacks(provider.getToolCallbacks())
                .build();
    }
}
