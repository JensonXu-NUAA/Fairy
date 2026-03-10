package cn.nuaa.jensonxu.fairy.integration.agent;

import cn.nuaa.jensonxu.fairy.integration.agent.manager.AgentModelManager;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Agent 构建器
 * 整合模型解析、工具注入，对外提供统一的 ReactAgent 创建入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentClientBuilder {

    private final AgentModelManager agentModelManager;
    private final ToolCallbackProvider toolCallbackProvider;
    private final AgentProperties agentProperties;

    /**
     * 根据请求指定的模型名称构建 ReactAgent
     * 若 modelName 为空，自动回退到 AgentProperties.defaultModel
     *
     * @param modelName 请求中指定的模型名称，允许为 null 或空字符串
     * @return 已注入工具集的 ReactAgent 实例，可直接执行
     */
    public ReactAgent build(String modelName) {
        String resolvedName = StringUtils.isNotBlank(modelName) ? modelName : agentProperties.getDefaultModel();
        log.info("[agent] 构建 ReactAgent，modelName: {}", resolvedName);
        return agentModelManager.createAgent(resolvedName, toolCallbackProvider.getToolCallbacks());
    }
}