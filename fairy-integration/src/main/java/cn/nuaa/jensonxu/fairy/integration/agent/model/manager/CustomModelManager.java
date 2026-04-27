package cn.nuaa.jensonxu.fairy.integration.agent.model.manager;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.common.data.llm.agent.response.AgentModelConfigVO;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentModelConfigRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentModelConfigDO;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.fastjson2.JSON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomModelManager {

    private final AgentModelConfigRepository repository;
    private final AgentModelFactoryManager factoryManager;

    /**
     * 模型配置的增删改查
     */
    public void insert(AgentModelConfigDO record) {
        repository.insert(record);
    }

    public void updateById(AgentModelConfigDO record) {
        repository.updateById(record);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Optional<AgentModelConfigDO> findById(Long id) {
        return repository.findById(id);
    }

    public List<AgentModelConfigVO> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(AgentModelConfigVO::from)
                .toList();
    }

    public List<String> listModelNames(String userId) {
        return repository.findByUserId(userId).stream()
                .map(AgentModelConfigDO::getModelName)
                .toList();
    }

    public boolean existsByUserIdAndModelName(String userId, String modelName) {
        return repository.existsByUserIdAndModelName(userId, modelName);
    }


    /**
     * 从 MySQL 加载用户指定的模型配置，创建 ReactAgent
     */
    public ReactAgent createAgent(String modelName, String userId, ToolCallback[] toolCallbacks, BaseCheckpointSaver saver, List<Hook> hooks, List<Interceptor> interceptors) {
        AgentModelConfigDO configDO = repository.findByUserIdAndModelName(userId, modelName)
                .orElseThrow(() -> new IllegalArgumentException("[agent] 未找到模型配置, userId: " + userId + ", modelName: " + modelName));
        if (configDO.getIsEnabled() == null || configDO.getIsEnabled() != 1) {
            throw new IllegalStateException("[agent] 模型已禁用, modelName: " + modelName);
        }
        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setModelName(configDO.getModelName());
        modelConfig.setProvider(configDO.getProvider());
        modelConfig.setApiKey(configDO.getApiKey());
        modelConfig.setBaseUrl(configDO.getBaseUrl());
        modelConfig.setEnabled(configDO.getIsEnabled() == 1);
        if (configDO.getParameters() != null) {
            modelConfig.setParameters(JSON.parseObject(configDO.getParameters(), ModelConfig.Parameters.class));
        }
        log.info("[agent] 创建 ReactAgent, userId: {}, modelName: {}, provider: {}", userId, modelName, modelConfig.getProvider());
        return factoryManager.createAgent(modelConfig, toolCallbacks, saver, hooks, interceptors);
    }
}