package cn.nuaa.jensonxu.fairy.integration.agent.memory.hook;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentMemoryDO;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory.MemoryManifest;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemoryRecaller;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;

/**
 * 长期记忆动态注入拦截器
 * 会话首次模型调用时执行两阶段召回：
 *   1. 构建索引清单，交给 AgentMemoryRecaller 选出相关 key
 *   2. 按 key 加载完整内容，拼装注入 System Prompt 前缀
 * 同一会话内后续调用直接复用缓存结果，不再查询
 */
@Slf4j
public class LongTermMemoryInterceptor extends ModelInterceptor {

    private final String userId;
    private final AgentLongTermMemory longTermMemory;
    private final AgentMemoryRecaller recaller;

    private volatile boolean prefixLoaded = false;
    private volatile String cachedPrefix = "";

    public LongTermMemoryInterceptor(AgentLongTermMemory longTermMemory, AgentMemoryRecaller recaller, String userId) {
        this.longTermMemory = longTermMemory;
        this.recaller = recaller;
        this.userId = userId;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        if (!prefixLoaded) {
            cachedPrefix = buildPrefix(request);
            prefixLoaded = true;
        }

        if (StringUtils.isBlank(cachedPrefix)) {
            return handler.call(request);
        }

        SystemMessage currentSysMsg = request.getSystemMessage();
        String currentContent = currentSysMsg != null ? currentSysMsg.getText() : "";
        ModelRequest enriched = ModelRequest.builder(request)
                .systemMessage(new SystemMessage(cachedPrefix + "\n" + currentContent))
                .build();

        log.debug("[long-term-interceptor] 注入长期记忆前缀, userId: {}, 前缀长度: {} 字", userId, cachedPrefix.length());
        return handler.call(enriched);
    }

    @Override
    public String getName() {
        return "LongTermMemoryInterceptor";
    }

    /**
     * 两阶段召回：构建 manifest → LLM 选 key → 加载内容 → 拼装前缀
     * 任意阶段为空则直接返回空字符串，不阻断主流程
     */
    private String buildPrefix(ModelRequest request) {
        MemoryManifest manifest = longTermMemory.buildMemoryManifest(userId);  // Step 1：构建索引清单
        if (manifest.isEmpty()) {
            log.debug("[long-term-interceptor] 用户无长期记忆, userId: {}", userId);
            return "";
        }

        String currentMessage = extractLastUserMessage(request.getMessages());  // Step 2：从请求消息中提取最后一条用户消息
        List<String> selectedKeys = recaller.recall(userId, currentMessage, manifest.text(), manifest.validKeys());  // Step 3：LLM 侧查询召回相关 key
        if (selectedKeys.isEmpty()) {
            log.debug("[long-term-interceptor] 召回结果为空, userId: {}", userId);
            return "";
        }

        List<AgentMemoryDO> selectedMemories = longTermMemory.loadSelectedMemories(userId, selectedKeys);  // Step 4：加载完整内容
        if (selectedMemories.isEmpty()) {
            return "";
        }

        String prefix = longTermMemory.buildSystemPromptPrefix(selectedMemories);  // Step 5：拼装 System Prompt 前缀
        log.info("[long-term-interceptor] 长期记忆加载完成, userId: {}, 注入 {} 条", userId, selectedMemories.size());
        return prefix;
    }

    /**
     * 从消息列表末尾向前查找最后一条 UserMessage
     */
    private String extractLastUserMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (MessageType.USER.equals(msg.getMessageType())) {
                return msg.getText();
            }
        }
        return "";
    }
}