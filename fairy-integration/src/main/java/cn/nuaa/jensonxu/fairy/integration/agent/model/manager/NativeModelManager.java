package cn.nuaa.jensonxu.fairy.integration.agent.model.manager;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class NativeModelManager {

    private final NacosConfigManager nacosConfigManager;

    private static final String SUMMARY_DATA_ID = "fairy_summary_config";
    private static final String GROUP_ID = "FAIRY_LLM_GROUP";

    private final Map<String, ModelConfig> summaryModels = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSummaryModel() {
        try {
            log.info("[native] 初始化摘要模型配置...");
            ConfigService configService = nacosConfigManager.getConfigService();
            String configInfo = configService.getConfig(SUMMARY_DATA_ID, GROUP_ID, 5000);

            if (StringUtils.isBlank(configInfo)) {
                log.warn("[native] 未能获取摘要模型配置，dataId: {}, group: {}", SUMMARY_DATA_ID, GROUP_ID);
                return;
            }

            loadSummaryModels(configInfo);
            log.info("[native] 摘要模型配置初始化成功，共加载 {} 个模型", summaryModels.size());

            configService.addListener(SUMMARY_DATA_ID, GROUP_ID, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String updateInfo) {
                    log.info("[native] 处理摘要模型配置更新");
                    Set<String> updatedKeys = loadSummaryModels(updateInfo);
                    summaryModels.keySet().removeIf(key -> {
                        if (!updatedKeys.contains(key)) {
                            log.info("[native] 摘要模型配置已移除: {}", key);
                            return true;
                        }
                        return false;
                    });
                    log.info("[native] 摘要模型配置更新完成，当前共 {} 个模型", summaryModels.size());
                }
            });

        } catch (Exception e) {
            log.error("[native] NativeModelManager 初始化异常: {}", e.getMessage(), e);
        }
    }

    public ModelConfig getSummaryModelConfig(String modelName) {
        ModelConfig config = summaryModels.get(modelName);
        if (config == null) {
            throw new IllegalArgumentException("[native] 未找到摘要模型配置，modelName: " + modelName);
        }
        return config;
    }

    private Set<String> loadSummaryModels(String configJson) {
        JSONObject jsonObject = JSON.parseObject(configJson);
        for (String modelName : jsonObject.keySet()) {
            ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
            modelConfig.setModelName(modelName);
            summaryModels.put(modelName, modelConfig);
            log.info("[native] 加载摘要模型配置: {}", modelName);
        }
        return jsonObject.keySet();
    }
}