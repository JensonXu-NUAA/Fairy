package cn.nuaa.jensonxu.gensokyo.integration.chat.client;

import cn.nuaa.jensonxu.gensokyo.integration.chat.data.CustomChatDTO;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CustomModelClient {

    private final ChatClient chatClient;
    private final StringBuilder fullContent;
    private final SseEmitter sseEmitter;
    private static final String SSE_DONE_MSE = "[DONE]";

    public CustomModelClient(ChatClient chatClient, SseEmitter sseEmitter) {
        this.chatClient = chatClient;
        this.sseEmitter = sseEmitter;
        this.fullContent = new StringBuilder();
    }

    public void chat(CustomChatDTO customChatDTO) {
        log.info("ModelClient 开始处理聊天 - ChatID: {}, UserID: {}", customChatDTO.getChatId(), customChatDTO.getUserId());
        CompletableFuture.runAsync(() -> {
           try {
               chatHandler(customChatDTO);
           } catch (Exception e) {
               log.error("ModelClient 处理聊天异常 - ChatID: {}, 错误: {}", customChatDTO.getChatId(), e.getMessage(), e);
           }
        });
    }

    private void chatHandler(CustomChatDTO customChatDTO) {
        Map<String, Object> userMessageMetadata = new HashMap<>();
        userMessageMetadata.put("userId", customChatDTO.getUserId());
        userMessageMetadata.put("chatId", customChatDTO.getChatId());
        userMessageMetadata.put("conversationId", customChatDTO.getConversationId());
        userMessageMetadata.put("modelName", customChatDTO.getModelName());

        // 使用Spring AI标准的UserMessage
        UserMessage userMessage = UserMessage.builder()
                .text(customChatDTO.getMessage())
                .metadata(userMessageMetadata)
                .build();

        Flux<String> stream = chatClient.prompt()
                .messages(userMessage)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, customChatDTO.getConversationId())
                        .param("userId", customChatDTO.getUserId())
                        .param("chatId", customChatDTO.getChatId())
                        .param("conversationId", customChatDTO.getConversationId()))
                .stream()
                .content();

        stream.doOnNext(this::dataHandler)
                .doOnComplete(this::onCompleteHandler)
                .doOnError(this::onFailureHandler)
                .subscribe();
    }

    private void dataHandler(String data) {
        try {
            fullContent.append(data);
            log.info("get sse send chunk data: {}", data);
            sendSseEvent("message", data);
        } catch (Exception e) {
            log.error("sse send chunk data error", e);
        }
    }

    private void onCompleteHandler() {
        try {
            log.info("sse stream chat complete, full content: {}", fullContent);
            sseEmitter.send(SSE_DONE_MSE);
            sseEmitter.complete();
        } catch (Exception e) {
            log.error("sse close connection error", e);
        }
    }

    private void onFailureHandler(Throwable throwable) {
        try {
            log.error("SSE stream response fail");
            sendSseEvent("error", throwable.getMessage());
        } catch (Exception e) {
            log.error("sse send message error", e);
        }
    }

    private void sendSseEvent(String event,  String data) throws IOException {
        JSONObject message = new JSONObject();
        message.put("event", event);
        message.put("data", data);
        sseEmitter.send(message.toJSONString());
    }
}
