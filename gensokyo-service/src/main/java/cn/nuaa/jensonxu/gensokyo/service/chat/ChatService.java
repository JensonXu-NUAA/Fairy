package cn.nuaa.jensonxu.gensokyo.service.chat;

import cn.nuaa.jensonxu.gensokyo.service.chat.data.CustomChatDTO;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    @Autowired
    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> streamChat(CustomChatDTO customChatDTO) {
        StringBuilder fullContent = new StringBuilder();
        customChatDTO.setChatId(UUID.randomUUID().toString());

        log.info("开始流式聊天 - 用户ID: {}, 对话ID: {}, 消息: {}",
                customChatDTO.getUserId(),
                customChatDTO.getConversationId(),
                customChatDTO.getMessage());

        return chatClient.prompt()
                .user(customChatDTO.getMessage())  // 简化消息设置
                .advisors(advisor -> advisor
                        .param("userId", customChatDTO.getUserId())
                        .param("chatId", customChatDTO.getChatId())
                        .param("conversationId", customChatDTO.getConversationId()))
                .stream()
                .content()
                .doOnNext(chunk -> {
                    fullContent.append(chunk);
                    log.info("接收到内容块: {}", chunk);
                })
                .doOnComplete(() -> log.info("流式响应完成 - ChatID: {}, full content: {}", customChatDTO.getChatId(), fullContent))
                .doOnError(error -> log.error("流式响应出错 - ChatID: {}, 错误: {}", customChatDTO.getChatId(), error.getMessage()));
    }
}
