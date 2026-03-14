package cn.nuaa.jensonxu.fairy.integration.agent;

import cn.nuaa.jensonxu.fairy.integration.agent.manager.AgentModelManager;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLoadedContext;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 构建器
 * 整合模型解析、工具注入、记忆回填，对外提供统一的 ReactAgent 创建入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentClientBuilder {

    private final AgentModelManager agentModelManager;
    private final ToolCallbackProvider toolCallbackProvider;
    private final AgentProperties agentProperties;
    private final MemorySaver memorySaver;

    /**
     * 根据请求上下文构建 ReactAgent
     *
     * @param modelName 请求指定的模型名称，为空时回退到 defaultModel
     * @param sessionId 会话 ID（agentSessionId），用于 MemorySaver 的 threadId 隔离
     * @param context   由 AgentMemoryManager.loadContext() 加载的记忆上下文
     * @return 已注入工具、记忆、System Prompt 的 ReactAgent 实例
     */
    public ReactAgent build(String modelName, String sessionId, AgentLoadedContext context) {
        String resolvedName = StringUtils.isNotBlank(modelName) ? modelName : agentProperties.getDefaultModel();
        log.info("[agent] 构建 ReactAgent, modelName: {}, sessionId: {}", resolvedName, sessionId);
        prepopulateIfNeeded(sessionId, context.shortTermMessages());  // 若 MemorySaver 中尚无该会话的记录（进程重启），从 Redis/MySQL 回填历史消息
        ReactAgent agent = agentModelManager.createAgent(resolvedName, toolCallbackProvider.getToolCallbacks(), memorySaver);  // 创建 ReactAgent，注入共享 MemorySaver

        if (StringUtils.isNotBlank(context.systemPromptPrefix())) {
            agent.setSystemPrompt(context.systemPromptPrefix());  // 注入长期记忆前缀到 System Prompt
        }

        return agent;
    }

    /**
     * 当 MemorySaver 中不存在该 sessionId 的历史时，从 Redis/MySQL 加载的消息重建 Checkpoint
     * 用于应对服务重启后 MemorySaver 内存状态丢失的场景
     */
    private void prepopulateIfNeeded(String sessionId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        try {
            RunnableConfig config = RunnableConfig.builder().threadId(sessionId).build();
            if (memorySaver.get(config).isEmpty()) {
                Checkpoint checkpoint = Checkpoint.builder()
                        .id(UUID.randomUUID().toString())
                        .state(Map.of("messages", messages))
                        .build();
                memorySaver.put(config, checkpoint);
                log.info("[agent] MemorySaver 回填历史消息 {} 条, sessionId: {}", messages.size(), sessionId);
            }
        } catch (Exception e) {
            log.warn("[agent] MemorySaver 回填失败, sessionId: {}", sessionId, e);
        }
    }
}
