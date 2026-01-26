package cn.nuaa.jensonxu.fairy.integration.chat.manager;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class ModelManager {

    private final NacosConfigManager nacosConfigManager;

    private final ChatClientFactoryManager factoryManager;

    private static final String DATA_ID = "llm_config";

    private static final String GROUP_ID = "FAIRY_LLM_GROUP";

    private final Map<String, ModelConfig> models = new ConcurrentHashMap<>();

    @Autowired
    public ModelManager(NacosConfigManager nacosConfigManager, ChatClientFactoryManager factoryManager) {
        this.nacosConfigManager = nacosConfigManager;
        this.factoryManager = factoryManager;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("[nacos] 初始化大模型参数配置...");
            ConfigService configService = nacosConfigManager.getConfigService();
            String configInfo = configService.getConfig(DATA_ID, GROUP_ID, 5000);
            if(StringUtils.isBlank(configInfo)) {
                log.warn("[nacos] 未能获取到模型配置，dataId: {}, group: {}", DATA_ID, GROUP_ID);
                return;
            }

            // 项目启动时加载模型参数配置
            JSONObject jsonObject = JSON.parseObject(configInfo);
            for(String modelName : jsonObject.keySet()) {
                ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
                modelConfig.setModelName(modelName);
                models.put(modelName, modelConfig);
                log.info("[nacos] 模型: {} 配置加载: {}", modelConfig.getModelName(), JSON.toJSONString(modelConfig));
            }
            log.info("[nacos] 模型配置初始化成功");

            // 配置 nacos 监听器，用于动态更新模型配置
            configService.addListener(
                    DATA_ID,
                    GROUP_ID,
                    new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }

                        @Override
                        public void receiveConfigInfo(String updateInfo) {
                            log.info("[nacos] 处理模型配置更新: {}", updateInfo);
                            JSONObject jsonObject = JSON.parseObject(updateInfo);
                            Set<String> set = jsonObject.keySet();

                            // 新增和更新模型配置
                            for(String modelName : set) {
                                ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
                                modelConfig.setModelName(modelName);
                                models.put(modelName, modelConfig);
                                log.info("[nacos] {} 模型配置更新成功: {}", modelConfig.getModelName(), JSON.toJSONString(modelConfig));
                            }

                            // 删除模型配置
                            for(Map.Entry<String, ModelConfig> entry : models.entrySet()) {
                                if(!set.contains(entry.getKey())) {
                                    models.remove(entry.getKey());
                                    log.info("[nacos] {}模型配置已移除", entry.getKey());
                                }
                            }
                            log.info("[nacos] 模型配置更新处理完成");
                        }
                    });
        } catch (Exception e) {
            log.error("[nacos] ModelManager 初始化异常: {}", e.getMessage(), e);
        }
    }

    public ChatClient createChatClient(String modelName) {
        ModelConfig modelConfig = models.get(modelName);
        log.info("find model config: {}", JSON.toJSONString(modelConfig));
        return  factoryManager.createChatClient(modelConfig);
    }
}
