package cn.nuaa.jensonxu.fairy.integration.agent;

import cn.nuaa.jensonxu.fairy.common.repository.minio.MinioProperties;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSkillRepository;

import cn.nuaa.jensonxu.fairy.integration.agent.memory.hook.AgentSummarizationHook;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.hook.LongTermMemoryInterceptor;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.hook.ShortTermRedisSaveHook;
import cn.nuaa.jensonxu.fairy.integration.agent.model.manager.AgentModelManager;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLoadedContext;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentLongTermMemory;
import cn.nuaa.jensonxu.fairy.integration.agent.skill.MixedSkillRegistry;
import cn.nuaa.jensonxu.fairy.integration.agent.skill.NativeSkillRegistry;
import cn.nuaa.jensonxu.fairy.integration.agent.skill.UserSkillRegistry;

import cn.nuaa.jensonxu.fairy.integration.service.tools.service.SkillToolService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private final AgentSummarizationHook agentSummarizationHook;
    private final ShortTermRedisSaveHook shortTermRedisSaveHook;
    private final AgentLongTermMemory agentLongTermMemory;

    // Skills 相关：NativeSkillRegistry 为单例 Bean，其余依赖用于 per-session 构建
    private final NativeSkillRegistry nativeSkillRegistry;
    private final AgentSkillRepository agentSkillRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final List<SkillToolService> skillToolServices;  // skill 列表

    /**
     * 根据请求上下文构建 ReactAgent
     *
     * @param modelName 请求指定的模型名称，为空时回退到 defaultModel
     * @param sessionId 会话 ID（agentSessionId），用于 MemorySaver 的 threadId 隔离
     * @param userId    用户 ID，用于创建 LongTermMemoryInterceptor
     * @param context   由 AgentMemoryManager.loadContext() 加载的记忆上下文
     * @return 已注入工具、记忆、System Prompt 的 ReactAgent 实例
     */
    public ReactAgent build(String modelName, String sessionId, String userId, AgentLoadedContext context) {
        String resolvedName = StringUtils.isNotBlank(modelName) ? modelName : agentProperties.getDefaultModel();
        log.info("[agent] 构建 ReactAgent, modelName: {}, sessionId: {}", resolvedName, sessionId);
        prepopulateIfNeeded(sessionId, context.shortTermMessages());  // 若 MemorySaver 中尚无该会话的记录（进程重启），从 Redis/MySQL 回填历史消息
        LongTermMemoryInterceptor memInterceptor = new LongTermMemoryInterceptor(agentLongTermMemory, userId);

        // 按 userId 构建会话级 MixedSkillRegistry，并组装 SkillsAgentHook
        UserSkillRegistry userSkillRegistry = new UserSkillRegistry(userId, agentSkillRepository, minioClient, minioProperties);
        MixedSkillRegistry mixedSkillRegistry = new MixedSkillRegistry(nativeSkillRegistry, userSkillRegistry);
        SkillsAgentHook skillsAgentHook = SkillsAgentHook.builder()
                .skillRegistry(mixedSkillRegistry)
                .groupedTools(buildGroupedTools())
                .build();

        List<Hook> hooks = List.of(skillsAgentHook, agentSummarizationHook, shortTermRedisSaveHook);
        List<Interceptor> interceptors = List.of(memInterceptor);

        /*
        if (StringUtils.isNotBlank(context.systemPromptPrefix())) {
            agent.setSystemPrompt(context.systemPromptPrefix());  // 注入长期记忆前缀到 System Prompt
        }
        */

        return agentModelManager.createAgent(resolvedName, toolCallbackProvider.getToolCallbacks(), memorySaver, hooks, interceptors);
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

    /**
     * 将 SkillToolService 实现类按 skillName 分组，构建 groupedTools
     */
    private Map<String, List<ToolCallback>> buildGroupedTools() {
        Map<String, List<ToolCallback>> groupedTools = new HashMap<>();
        for (SkillToolService skillTool : skillToolServices) {
            ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                    .toolObjects(skillTool)
                    .build()
                    .getToolCallbacks();
            groupedTools.computeIfAbsent(skillTool.getSkillName(), k -> new ArrayList<>()).addAll(Arrays.asList(callbacks));
        }
        log.info("[skill] groupedTools 构建完成，共 {} 个 skill 绑定工具组", groupedTools.size());
        return groupedTools;
    }

}
