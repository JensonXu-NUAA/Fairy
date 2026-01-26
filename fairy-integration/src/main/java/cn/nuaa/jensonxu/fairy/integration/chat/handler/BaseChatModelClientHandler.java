package cn.nuaa.jensonxu.fairy.integration.chat.handler;

import cn.nuaa.jensonxu.fairy.common.data.llm.CustomChatDTO;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型对话处理器抽象基类
 */
@Slf4j
public abstract class BaseChatModelClientHandler {

    protected String chatId;  // 当前对话 ID

    protected Integer chunkId = 1;  // 数据块 ID，用于标识SSE流式响应的数据块序号

    protected final ChatClient chatClient;  // Spring AI 的聊天客户端

    protected final SseEmitter sseEmitter;  // 用于向客户端推送流式响应

    protected final Cache cache;  // 缓存对象，用于存储 SSE 数据块

    protected final StringBuilder fullContent = new StringBuilder();  // 完整的聊天内容字符串

    protected final AtomicReference<Usage> usageRef = new AtomicReference<>();  // Token 使用情况

    protected static final String SSE_START = "start";  // SSE 开始标记

    protected static final String SSE_MESSAGE = "message";  // SSE 数据标记

    protected static final String SSE_END = "end";  // SSE 结束标记

    protected static final String SSE_DONE = "[DONE]";  // SSE 流结束标记

    /**
     * 构造函数
     *
     * @param chatClient   Spring AI 聊天客户端
     * @param sseEmitter   SSE 发射器
     * @param cacheManager 缓存管理器
     */
    public BaseChatModelClientHandler(ChatClient chatClient, SseEmitter sseEmitter, CacheManager cacheManager) {
        this.chatClient = chatClient;
        this.sseEmitter = sseEmitter;
        this.cache = cacheManager.getCache("sseChunkCache");
    }

    /**
     * 聊天处理
     * 由子类实现具体的聊天逻辑
     *
     * @param customChatDTO 自定义聊天数据传输对象
     */
    public abstract void chat(CustomChatDTO customChatDTO);

    /**
     * 聊天处理器
     * 由子类实现具体的聊天处理逻辑
     *
     * @param customChatDTO 自定义聊天数据传输对象
     */
    protected abstract void chatHandler(CustomChatDTO customChatDTO);

    /**
     * 完成处理器
     * 当 SSE 流正常完成时调用
     */
    protected abstract void onCompleteHandler();

    /**
     * 失败处理器
     * 当 SSE 流出现异常时调用
     *
     * @param throwable 异常对象
     */
    protected abstract void onFailureHandler(Throwable throwable);

    /**
     * 获取指定 chunkId 之后的缓存数据（抽象方法）
     *
     * @param chunkId 起始数据块ID
     * @return 指定ID之后的聊天响应列表
     */
    protected abstract List<ChatResponse> getCacheAfter(Integer chunkId);

    /**
     * 发送 SSE 事件
     *
     * @param event 事件类型（如：start, message, end, error等）
     * @param data  事件数据
     * @throws Exception 发送异常
     */
    protected void sendSseEvent(String event, String data) throws Exception {
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .id(chunkId.toString())
                .name(event)
                .data(data)
                .reconnectTime(3000L);

        sseEmitter.send(builder.build());
        chunkId++;
    }

    /**
     * 数据处理器
     * 处理接收到的聊天响应数据
     * 子类可以重写此方法以实现自定义的数据处理逻辑
     *
     * @param chatResponse 聊天响应对象
     * @throws Exception 处理异常
     */
    protected void dataHandler(ChatResponse chatResponse) throws Exception {
    }

    /**
     * 数据块内部类
     * 用于存储数据块ID和对应的聊天响应
     */
    @Data
    @Builder
    static class Chunk {

        private Integer chunkId;  // 数据块ID

        private ChatResponse chatResponse;  // 聊天响应对象
    }
}
