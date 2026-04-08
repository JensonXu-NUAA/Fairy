package cn.nuaa.jensonxu.fairy.integration.agent.model.manager;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Agent 模型管理器
 * 独立监听 Nacos 模型配置，与 ModelManager 平行，互不干扰
 * 负责向上提供 createAgent() 入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentModelManager {

    private final NacosConfigManager nacosConfigManager;
    private final AgentModelFactoryManager factoryManager;

    private static final String AGENT_DATA_ID = "fairy_agent_config";
    private static final String SUMMARY_DATA_ID = "fairy_summary_config";
    private static final String GROUP_ID = "FAIRY_LLM_GROUP";

    /** Agent 侧独立维护的模型配置缓存 */
    private final Map<String, ModelConfig> agentModels = new ConcurrentHashMap<>();
    /** 摘要模型独立缓存，与 Agent 主模型隔离，避免 removeIf 互相影响 */
    private final Map<String, ModelConfig> summaryModels = new ConcurrentHashMap<>();

    @PostConstruct
    public void initAgentModel() {
        try {
            log.info("[agent] 初始化模型配置...");
            ConfigService configService = nacosConfigManager.getConfigService();
            String configInfo = configService.getConfig(AGENT_DATA_ID, GROUP_ID, 5000);

            if (StringUtils.isBlank(configInfo)) {
                log.warn("[agent] 未能获取到模型配置，dataId: {}, group: {}", AGENT_DATA_ID, GROUP_ID);
                return;
            }

            loadModels(configInfo);
            log.info("[agent] 模型配置初始化成功，共加载 {} 个模型", agentModels.size());

            // 监听 Nacos 配置变更，动态更新
            configService.addListener(AGENT_DATA_ID, GROUP_ID, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String updateInfo) {
                    log.info("[agent] 处理模型配置更新");
                    Set<String> updatedKeys = loadModels(updateInfo);

                    // 移除已下线的模型
                    agentModels.keySet().removeIf(key -> {
                        if (!updatedKeys.contains(key)) {
                            log.info("[agent] 模型配置已移除: {}", key);
                            return true;
                        }
                        return false;
                    });
                    log.info("[agent] 模型配置更新完成，当前共 {} 个模型", agentModels.size());
                }
            });

        } catch (Exception e) {
            log.error("[agent] AgentModelManager 初始化异常: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void initSummaryModel() {
        try {
            log.info("[agent] 初始化摘要模型配置...");
            ConfigService configService = nacosConfigManager.getConfigService();
            String configInfo = configService.getConfig(SUMMARY_DATA_ID, GROUP_ID, 5000);

            if (StringUtils.isBlank(configInfo)) {
                log.warn("[agent] 未能获取摘要模型配置，dataId: {}, group: {}", SUMMARY_DATA_ID, GROUP_ID);
                return;
            }

            loadSummaryModels(configInfo);
            log.info("[agent] 摘要模型配置初始化成功，共加载 {} 个模型", summaryModels.size());

            // 监听动态变更，支持热更新
            configService.addListener(SUMMARY_DATA_ID, GROUP_ID, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String updateInfo) {
                    log.info("[agent] 处理摘要模型配置更新");
                    Set<String> updatedKeys = loadSummaryModels(updateInfo);

                    // 移除已下线的摘要模型
                    summaryModels.keySet().removeIf(key -> {
                        if (!updatedKeys.contains(key)) {
                            log.info("[agent] 摘要模型配置已移除: {}", key);
                            return true;
                        }
                        return false;
                    });
                    log.info("[agent] 摘要模型配置更新完成，当前共 {} 个模型", summaryModels.size());
                }
            });

        } catch (Exception e) {
            log.error("[agent] 摘要模型配置初始化异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取摘要模型的 ModelConfig，供 AgentMemorySummarizer（Phase 2）使用
     *
     * @param modelName 摘要模型名称，对应 AgentProperties.memory.longTerm.modelName
     */
    public ModelConfig getSummaryModelConfig(String modelName) {
        ModelConfig config = summaryModels.get(modelName);
        if (config == null) {
            throw new IllegalArgumentException("[agent] 未找到摘要模型配置，modelName: " + modelName);
        }
        return config;
    }

    /**
     * 根据模型名称和工具集创建 ReactAgent
     *
     * @param modelName     请求指定的模型名称
     * @param toolCallbacks 由 ToolCallbackProvider 提供的工具回调数组
     * @return 配置好模型和工具的 ReactAgent 实例
     */
    public ReactAgent createAgent(String modelName, ToolCallback[] toolCallbacks, BaseCheckpointSaver saver, List<Hook> hooks, List<Interceptor> interceptors) {
        ModelConfig modelConfig = agentModels.get(modelName);
        if (modelConfig == null) {
            throw new IllegalArgumentException("[agent] 未找到模型配置，modelName: " + modelName);
        }
        log.info("[agent] 创建 ReactAgent，modelName: {}", modelName);
        return factoryManager.createAgent(modelConfig, toolCallbacks, saver, hooks, interceptors);
    }

    /**
     * 返回当前已加载且启用的模型列表
     */
    public List<ModelConfig> getAvailableModels() {
        return agentModels.values().stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .map(m -> {
                    ModelConfig config = new ModelConfig();
                    config.setModelName(m.getModelName());
                    config.setProvider(m.getProvider());
                    config.setEnabled(m.getEnabled());
                    config.setParameters(m.getParameters());
                    return config;
                })
                .toList();
    }

    /**
     * 解析配置 JSON 并写入缓存，返回本次解析的 key 集合
     */
    private Set<String> loadModels(String configJson) {
        JSONObject jsonObject = JSON.parseObject(configJson);
        for (String modelName : jsonObject.keySet()) {
            ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
            modelConfig.setModelName(modelName);
            agentModels.put(modelName, modelConfig);
            log.info("[agent] 加载模型配置: {}", modelName);
        }
        return jsonObject.keySet();
    }

    /**
     * 解析摘要模型配置 JSON 并写入 summaryModels 缓存
     */
    private Set<String> loadSummaryModels(String configJson) {
        JSONObject jsonObject = JSON.parseObject(configJson);
        for (String modelName : jsonObject.keySet()) {
            ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
            modelConfig.setModelName(modelName);
            summaryModels.put(modelName, modelConfig);
            log.info("[agent] 加载摘要模型配置: {}", modelName);
        }
        return jsonObject.keySet();
    }
}