package cn.nuaa.jensonxu.gensokyo.integration.chat.advisor;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.jetbrains.annotations.NotNull;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ChatMemoryAdvisor implements StreamAdvisor, CallAdvisor {

    private final ChatMemory chatMemory;
    private final int maxHistorySize;

    public ChatMemoryAdvisor(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.maxHistorySize = 20; // 最大历史消息数量
    }

    @NotNull
    @Override
    public String getName() {
        return "ChatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // 最高优先级，确保在其他advisor之前加载历史消息
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest request, @NotNull CallAdvisorChain chain) {
        ChatClientRequest enhancedRequest = addChatHistoryToRequest(request);
        return chain.nextCall(enhancedRequest);
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest enhancedRequest = addChatHistoryToRequest(request);
        return chain.nextStream(enhancedRequest);
    }

    /**
     * 将聊天历史添加到请求中
     */
    private ChatClientRequest addChatHistoryToRequest(ChatClientRequest request) {
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);

        if (StringUtils.isBlank(conversationId)) {
            log.debug("无conversationId，跳过历史消息加载");
            return request;
        }

        try {
            List<Message> chatHistory = chatMemory.get(conversationId);  // 获取历史消息
            if (CollectionUtils.isEmpty(chatHistory)) {
                log.debug("无历史消息, conversationId: {}", conversationId);
                return request;
            }

            List<Message> limitedHistory = limitHistorySize(chatHistory);  // 限制历史消息数量

            // 将历史消息和当前消息组合
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(limitedHistory);
            allMessages.addAll(request.prompt().getInstructions());

            // 创建新的prompt
            Prompt enhancedPrompt = new Prompt(allMessages, request.prompt().getOptions());
            log.info("加载历史消息: conversationId={}, historyCount={}, currentCount={}",
                    conversationId, limitedHistory.size(), request.prompt().getInstructions().size());

            return ChatClientRequest.builder()
                    .prompt(enhancedPrompt)
                    .context(request.context())
                    .build();

        } catch (Exception e) {
            log.error("加载聊天历史失败, conversationId: {}", conversationId, e);
            return request; // 发生错误时返回原始请求
        }
    }

    /**
     * 限制历史消息数量
     */
    private List<Message> limitHistorySize(List<Message> messages) {
        if (messages.size() <= maxHistorySize) {
            return messages;
        }

        // 保留最近的消息
        return messages.subList(messages.size() - maxHistorySize, messages.size());
    }
}
