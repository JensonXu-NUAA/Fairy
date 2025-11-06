package cn.nuaa.jensonxu.gensokyo.integration.chat.config;

import cn.nuaa.jensonxu.gensokyo.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.gensokyo.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.gensokyo.repository.mysql.chat.CustomChatMemoryRepository;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ChatModel.class)
public class ModelConfig {

    private final CustomChatMemoryRepository repository;

    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    public ModelConfig(CustomChatMemoryRepository repository) {
        this.repository = repository;
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InSqlMemory(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new PersistenceMemoryAdvisor(chatMemory)
                )
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                .build();
    }
}
