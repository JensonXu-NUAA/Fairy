package cn.nuaa.jensonxu.fairy.integration.chat.handler;

import cn.nuaa.jensonxu.fairy.integration.chat.data.CustomChatDTO;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CustomModelClientHandler {

    private String chatId;

    private Integer chunkId = 1;

    private final ChatClient chatClient;

    private final SseEmitter sseEmitter;

    private final Cache cache;

    private final StringBuilder fullContent = new StringBuilder();

    private final AtomicReference<Usage> usageRef = new AtomicReference<>();

    private static final String SSE_DONE_MSE = "[DONE]";

    public CustomModelClientHandler(ChatClient chatClient, SseEmitter sseEmitter, CacheManager cacheManager) {
        this.chatClient = chatClient;
        this.sseEmitter = sseEmitter;
        this.cache = cacheManager.getCache("sseChunkCache");
    }

    public void chat(CustomChatDTO customChatDTO) {
        chatId = customChatDTO.getChatId();
        log.info("[Chat] 开始处理聊天 - ChatID: {}, UserID: {}", customChatDTO.getChatId(), customChatDTO.getUserId());
        CompletableFuture.runAsync(() -> {
           try {
               JSONObject startMessage = new JSONObject();
               startMessage.put("userId", customChatDTO.getUserId());
               startMessage.put("conversationId", customChatDTO.getConversationId());
               startMessage.put("chatId", customChatDTO.getChatId());
               sendSseEvent("start", JSON.toJSONString(startMessage));  // 预发送一个 start 数据包
               chatHandler(customChatDTO);
           } catch (Exception e) {
               log.error("[Chat] 处理聊天异常 - ChatID: {}, 信息: {}", customChatDTO.getChatId(), e.getMessage(), e);
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

        Flux<ChatResponse> stream = chatClient.prompt()
                .messages(userMessage)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, customChatDTO.getConversationId())
                        .param("userId", customChatDTO.getUserId())
                        .param("chatId", customChatDTO.getChatId())
                        .param("conversationId", customChatDTO.getConversationId()))
                .stream()
                .chatResponse();  // 使用chatResponse()获取完整响应

        stream.doOnNext(this::dataHandler)
                .doOnComplete(this::onCompleteHandler)
                .doOnError(this::onFailureHandler)
                .subscribe();
    }

    private void dataHandler(ChatResponse chatResponse) {
        try {
            String content = "";
            Chunk chunk = Chunk.builder()
                    .chunkId(chunkId)
                    .chatResponse(chatResponse)
                    .build();

            // 提取并保存Usage信息
            if (ObjectUtils.isNotEmpty(chatResponse.getMetadata())) {
                ChatResponseMetadata metadata = chatResponse.getMetadata();
                usageRef.set(metadata.getUsage());
            }

            if (ObjectUtils.isNotEmpty(chatResponse.getResult()) && chatResponse.getResult().getOutput() != null) {
                content = chatResponse.getResult().getOutput().getText();
            }

            // 如果内容为空或null，跳过
            if (StringUtils.isBlank(content)) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<Chunk> chunkCache = cache.get(chatId, List.class);
            if(cache.get(chatId) == null) {
                chunkCache = new CopyOnWriteArrayList<>();
            }

            chunkCache.add(chunk);
            cache.put(chatId, chunkCache);
            fullContent.append(content);
            log.info("[Chat] 获取SSE数据包, id: {}, content: {}, 已缓存chunk数量: {}", chunkId, content, chunkCache.size());
            sendSseEvent("message", content);
        } catch (Exception e) {
            log.error("[Chat] sse send chunk data error", e);
        }
    }

    private void onCompleteHandler() {
        try {
            log.info("[Chat] SSE流式对话完成, 完整内容: {}", fullContent);
            // 获取并发送token使用信息
            Usage usage = usageRef.get();
            if (usage != null) {
                JSONObject tokenInfo = new JSONObject();
                tokenInfo.put("promptTokens", usage.getPromptTokens());
                tokenInfo.put("completionTokens", usage.getCompletionTokens());
                tokenInfo.put("totalTokens", usage.getTotalTokens());
                log.info("Token usage - Prompt: {}, Completion: {}, Total: {}", usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

                sendSseEvent("end", tokenInfo.toJSONString());  // 发送token信息
            } else {
                log.warn("No token usage information available");
            }

            sseEmitter.send(SSE_DONE_MSE);
            sseEmitter.complete();

            cache.evict(chatId);  // 清理对应chat id 下的本地缓存
            log.info("[Chat] 清理 chatId {} 的缓存", chatId);
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
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .id(chunkId.toString())
                .name(event)
                .data(data)
                .reconnectTime(3000L);

        sseEmitter.send(builder.build());
        chunkId++;
    }

    private List<ChatResponse> getCacheAfter(Integer chunkId) {
        @SuppressWarnings("unchecked")
        List<Chunk> allChunks = cache.get(chatId, List.class);

        if(CollectionUtils.isEmpty(allChunks)) {
            return Collections.emptyList();
        }

        boolean found = false;
        List<ChatResponse> responseList = new ArrayList<>();
        for(Chunk chunk : allChunks) {
            if(found) {
                responseList.add(chunk.getChatResponse());
            } else if(chunk.getChunkId().equals(chunkId)) {
                found = true;
            }
        }
        return responseList;
    }

    @Data
    @Builder
    static class Chunk {
        private Integer chunkId;

        private ChatResponse chatResponse;
    }
}
