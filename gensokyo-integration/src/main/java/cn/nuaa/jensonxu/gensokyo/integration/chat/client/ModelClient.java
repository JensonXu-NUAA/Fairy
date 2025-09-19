package cn.nuaa.jensonxu.gensokyo.integration.chat.client;

import cn.nuaa.jensonxu.gensokyo.integration.chat.data.CustomChatDTO;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ModelClient {

    private final ChatClient chatClient;
    private final StringBuilder fullContent;
    private final SseEmitter sseEmitter;
    private static final String SSE_DONE_MSE = "[DONE]";

    public ModelClient(ChatClient chatClient, SseEmitter sseEmitter) {
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
        Flux<String> stream = chatClient.prompt()
                .user(customChatDTO.getMessage())
                .advisors(advisor -> advisor
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
