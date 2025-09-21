package cn.nuaa.jensonxu.gensokyo.integration.chat.advisor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PersistenceMemoryAdvisor implements StreamAdvisor, CallAdvisor {

    private final ChatMemory chatMemory;

    @Autowired
    public PersistenceMemoryAdvisor(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest request, @NotNull CallAdvisorChain chain) {
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String userId = (String) request.context().get("userId");
        String chatId = (String) request.context().get("chatId");

        log.info("开始同步对话存储, conversationId: {}", conversationId);
        storeUserMessage(request, conversationId);  // 1. 存储用户当前输入（只存储新的用户消息）
        ChatClientResponse response = chain.nextCall(request);  // 2. 调用下一个advisor获取AI响应
        storeAiResponse(response, userId, chatId, conversationId);  // 3. 存储AI响应

        return response;
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String userId = (String) request.context().get("userId");
        String chatId = (String) request.context().get("chatId");

        log.info("开始流式对话存储, conversationId: {}", conversationId);

        // 1. 对话开始前：存储用户当前输入
        storeUserMessage(request, conversationId);
        StringBuilder responseContent = new StringBuilder();

        return chain.nextStream(request)
                .doOnNext(response -> {
                    assert response.chatResponse() != null;
                    String content = response.chatResponse().getResult().getOutput().getText();
                    if (StringUtils.isNotBlank(content)) {
                        responseContent.append(content);
                    }
                })
                .doOnComplete(() -> {
                    if (!responseContent.isEmpty()) {
                        storeFullContent(responseContent.toString(), userId, chatId, conversationId);
                        log.info("流式对话存储完成, conversationId: {}", conversationId);
                    }
                })
                .doOnError(throwable -> log.error("流式对话存储过程中发生错误, conversationId: {}", conversationId, throwable));
    }

    /**
     * 存储当前用户输入的消息（过滤掉历史消息）
     */
    private void storeUserMessage(ChatClientRequest request, String conversationId) {
        request.prompt().getInstructions().forEach(message -> {
            if (message.getMessageType() == MessageType.USER && isNewUserMessage(message)) {
                try {
                    chatMemory.add(conversationId, message);
                    log.info("store user message: conversationId={}, content={}", conversationId, message.getText().substring(0, Math.min(50, message.getText().length())));
                } catch (Exception e) {
                    log.error("store user message fail, conversationId: {}", conversationId, e);
                }
            }
        });
    }

    /**
     * 判断是否为新的用户消息（非历史消息）
     */
    private boolean isNewUserMessage(Message message) {
        Map<String, Object> metadata = message.getMetadata();
        return !metadata.containsKey("id") && !metadata.containsKey("createTime");
    }

    /**
     * 存储AI响应（同步调用版本）
     */
    private void storeAiResponse(ChatClientResponse response, String userId, String chatId, String conversationId) {
        try {
            assert response.chatResponse() != null;
            String content = response.chatResponse().getResult().getOutput().getText();
            if (StringUtils.isNotBlank(content)) {
                storeFullContent(content, userId, chatId, conversationId);
            }
        } catch (Exception e) {
            log.error("存储AI响应失败, conversationId: {}", conversationId, e);
        }
    }

    /**
     * 存储AI响应内容
     */
    private void storeFullContent(String content, String userId, String chatId, String conversationId) {
        try {
            Map<String, Object> assistantMetaData = new HashMap<>();
            assistantMetaData.put("userId", userId);
            assistantMetaData.put("chatId", chatId);
            assistantMetaData.put("conversationId", conversationId);
            assistantMetaData.put("modelName", "qwen-turbo");

            AssistantMessage aiMessage = new AssistantMessage(content, assistantMetaData);
            chatMemory.add(conversationId, aiMessage);
            log.info("store assistant message: conversationId={}, length={}", conversationId, content.length());
        } catch (Exception e) {
            log.error("store assistant message fail, conversationId: {}", conversationId, e);
        }
    }
}
