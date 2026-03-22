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
 * 替代 AgentClientBuilder.build() 中一次性的 setSystemPrompt() 调用
 *
 * 注意：非 Spring 单例 Bean，由 AgentClientBuilder 按请求创建（userId 作为构造参数传入）
 */
@Slf4j
public class LongTermMemoryInterceptor extends ModelInterceptor {

    private final AgentLongTermMemory longTermMemory;
    private final String userId;

    public LongTermMemoryInterceptor(AgentLongTermMemory longTermMemory, String userId) {
        this.longTermMemory = longTermMemory;
        this.userId = userId;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        String prefix = longTermMemory.buildSystemPromptPrefix(userId);

        if (StringUtils.isBlank(prefix)) {
            return handler.call(request);
        }

        // 将长期记忆前缀拼接到现有 SystemMessage 头部
        SystemMessage currentSysMsg = request.getSystemMessage();
        String currentContent = currentSysMsg != null ? currentSysMsg.getText() : "";
        String newContent = prefix + currentContent;

        ModelRequest enriched = ModelRequest.builder(request)
                .systemMessage(new SystemMessage(newContent))
                .build();

        log.debug("[long-term-interceptor] 注入长期记忆前缀, userId: {}, 前缀长度: {} 字符",
                userId, prefix.length());

        return handler.call(enriched);
    }

    @Override
    public String getName() {
        return "LongTermMemoryInterceptor";
    }
}