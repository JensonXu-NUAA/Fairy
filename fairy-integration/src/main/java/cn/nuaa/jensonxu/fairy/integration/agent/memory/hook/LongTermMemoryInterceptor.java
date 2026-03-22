package cn.nuaa.jensonxu.fairy.integration.agent.memory.hook;

import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 长期记忆动态注入拦截器（ModelInterceptor）
 * 在每次模型调用前，从 MySQL 读取最新长期记忆并动态注入 System Prompt 前缀
 */
@Slf4j
public class LongTermMemoryInterceptor extends ModelInterceptor {

    private final String userId;
    private final AgentLongTermMemory longTermMemory;

    private volatile boolean prefixLoaded = false;  // 同一个拦截器实例内只查询一次 MySQL
    private volatile String cachedPrefix;

    public LongTermMemoryInterceptor(AgentLongTermMemory longTermMemory, String userId) {
        this.longTermMemory = longTermMemory;
        this.userId = userId;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        if(!prefixLoaded) {
            cachedPrefix = longTermMemory.buildSystemPromptPrefix(userId);
            prefixLoaded = true;
            log.debug("[long-term-interceptor] 首次加载长期记忆前缀, userId: {}, 长度: {} 字符", userId, cachedPrefix != null ? cachedPrefix.length() : 0);
        }

        if (StringUtils.isBlank(cachedPrefix)) {
            return handler.call(request);
        }

        // 将长期记忆前缀拼接到现有 SystemMessage 头部
        SystemMessage currentSysMsg = request.getSystemMessage();
        String currentContent = currentSysMsg != null ? currentSysMsg.getText() : "";
        String newContent = cachedPrefix + currentContent;

        ModelRequest enriched = ModelRequest.builder(request)
                .systemMessage(new SystemMessage(newContent))
                .build();

        log.debug("[long-term-interceptor] 注入长期记忆前缀, userId: {}, 前缀长度: {} 字符", userId, cachedPrefix.length());

        return handler.call(enriched);
    }

    @Override
    public String getName() {
        return "LongTermMemoryInterceptor";
    }
}