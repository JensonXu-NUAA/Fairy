package cn.nuaa.jensonxu.fairy.integration.chat.config;

import cn.nuaa.jensonxu.fairy.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ChatModel.class)
public class ModelConfig {

    private final CustomChatMemoryRepository repository;

    public ModelConfig(CustomChatMemoryRepository repository) {
        this.repository = repository;
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InSqlMemory(repository);
    }
}
