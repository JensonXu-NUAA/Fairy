package cn.nuaa.jensonxu.fairy.integration.agent.manager;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.agent.factory.BaseAgentModelFactory;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 工厂管理器
 * 负责收集所有 BaseAgentModelFactory 实现，按 provider 路由创建 ReactAgent
 */
@Slf4j
@Component
public class AgentModelFactoryManager {

    /** 按 provider 名称索引的工厂注册表 */
    private final Map<String, BaseAgentModelFactory> factoryMap = new ConcurrentHashMap<>();

    @Autowired
    public AgentModelFactoryManager(List<BaseAgentModelFactory> factories) {
        for (BaseAgentModelFactory factory : factories) {
            factoryMap.put(factory.getProviderName().toLowerCase(), factory);
            log.info("[agent] 注册工厂: {}", factory.getProviderName());
        }
    }

    /**
     * 根据模型配置和工具回调创建 ReactAgent
     *
     * @param modelConfig     来自 Nacos 的模型配置
     * @param toolCallbacks   已注册的工具回调列表，由 ToolCallbackProvider 提供
     * @return 配置好工具的 ReactAgent 实例
     */
    public ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] toolCallbacks, BaseCheckpointSaver saver) {
        String provider = modelConfig.getProvider();
        if (StringUtils.isEmpty(provider)) {
            throw new IllegalArgumentException("模型提供商 (provider) 不能为空");
        }

        BaseAgentModelFactory factory = factoryMap.get(provider.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException(String.format("不支持的 Agent 提供商: %s，可用提供商: %s", provider, factoryMap.keySet()));
        }
        log.info("[agent] 创建 ReactAgent，provider: {}, model: {}", provider, JSON.toJSONString(modelConfig.getModelName()));

        // 先由各工厂创建基础 ReactAgent
        return factory.createAgent(modelConfig, toolCallbacks, saver);
    }

    /**
     * 检查是否支持指定提供商
     */
    public boolean isProviderSupported(String provider) {
        return factoryMap.containsKey(provider.toLowerCase());
    }

    /**
     * 获取所有支持的提供商名称
     */
    public List<String> getSupportedProviders() {
        return factoryMap.values().stream()
                .map(BaseAgentModelFactory::getProviderName)
                .toList();
    }
}
