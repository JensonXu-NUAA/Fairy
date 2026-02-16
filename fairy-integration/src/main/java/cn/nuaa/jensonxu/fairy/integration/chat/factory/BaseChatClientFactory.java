package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.common.repository.minio.MinioProperties;
import cn.nuaa.jensonxu.fairy.integration.chat.advisor.ChatMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.integration.chat.advisor.FileAdvisor;
import cn.nuaa.jensonxu.fairy.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;

import io.minio.MinioClient;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;

@RequiredArgsConstructor
public abstract class BaseChatClientFactory {

    protected final CustomChatMemoryRepository repository;
    protected final ToolCallbackProvider provider;
    protected final MinioClient client;
    protected final MinioProperties properties;

    protected static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    public abstract boolean supports(String provider);

    public abstract ChatClient createChatClient(ModelConfig modelConfig);

    public abstract String getProviderName();

    protected ChatClient create(ChatModel model, ModelConfig modelConfig) {
        ChatMemory chatMemory = new InSqlMemory(repository);
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new FileAdvisor(client, properties),
                        new ChatMemoryAdvisor(chatMemory),
                        new PersistenceMemoryAdvisor(chatMemory, modelConfig.getModelName())
                )
                .defaultToolCallbacks(provider.getToolCallbacks())
                .build();
    }
}
