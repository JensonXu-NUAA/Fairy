package cn.nuaa.jensonxu.fairy.service.chat;

import cn.nuaa.jensonxu.fairy.integration.chat.handler.CustomModelClientHandler;
import cn.nuaa.jensonxu.fairy.common.data.llm.CustomChatDTO;

import cn.nuaa.jensonxu.fairy.integration.chat.manager.ModelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CacheManager cacheManager;
    private final ModelManager modelManager;

    public SseEmitter streamChat(CustomChatDTO customChatDTO) {
        customChatDTO.setChatId(UUID.randomUUID().toString());
        if(StringUtils.isBlank(customChatDTO.getConversationId())) {
            customChatDTO.setConversationId(UUID.randomUUID().toString());
        }

        log.info("[Chat] 开始流式聊天对话 - 用户ID: {}, 对话ID: {}, 消息: {}", customChatDTO.getUserId(), customChatDTO.getConversationId(), customChatDTO.getMessage());

        SseEmitter sseEmitter = new SseEmitter(0L);
        setSseCallbacks(sseEmitter, customChatDTO.getChatId());

        try {
            CustomModelClientHandler customModelClientHandler = new CustomModelClientHandler(modelManager.createChatClient(customChatDTO.getModelName()), sseEmitter, cacheManager);
            customModelClientHandler.chat(customChatDTO);
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
            log.info("[Chat] SSE连接完成 - ChatID: {}", chatId);
        });

        emitter.onTimeout(() -> {
            log.warn("[Chat] SSE连接超时 - ChatID: {}", chatId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            log.error("[Chat] SSE连接错误 - ChatID: {}, 错误: {}", chatId, e.getMessage());
        });
    }
}
