package cn.nuaa.jensonxu.gensokyo.service.chat;

import cn.nuaa.jensonxu.gensokyo.integration.chat.client.CustomModelClient;
import cn.nuaa.jensonxu.gensokyo.integration.chat.data.CustomChatDTO;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    @Autowired
    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public SseEmitter streamChat(CustomChatDTO customChatDTO) {
        customChatDTO.setChatId(UUID.randomUUID().toString());
        if(StringUtils.isBlank(customChatDTO.getConversationId())) {
            customChatDTO.setConversationId(UUID.randomUUID().toString());
        }

        log.info("开始流式聊天 - 用户ID: {}, 对话ID: {}, 消息: {}", customChatDTO.getUserId(), customChatDTO.getConversationId(), customChatDTO.getMessage());

        SseEmitter sseEmitter = new SseEmitter();
        setSseCallbacks(sseEmitter, customChatDTO.getChatId());

        try {
            CustomModelClient client = new CustomModelClient(chatClient, sseEmitter);
            client.chat(customChatDTO);
        } catch (Exception e) {
            sseEmitter.completeWithError(e);
        }

        return sseEmitter;
    }

    /**
     * 设置SSE生命周期回调
     */
    private void setSseCallbacks(SseEmitter emitter, String chatId) {
        emitter.onCompletion(() -> {
            log.info("SSE连接完成 - ChatID: {}", chatId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时 - ChatID: {}", chatId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            log.error("SSE连接错误 - ChatID: {}, 错误: {}", chatId, e.getMessage());
        });
    }
}
