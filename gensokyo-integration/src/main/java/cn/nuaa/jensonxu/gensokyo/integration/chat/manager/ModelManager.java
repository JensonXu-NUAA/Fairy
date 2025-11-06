package cn.nuaa.jensonxu.gensokyo.integration.chat.manager;

import cn.nuaa.jensonxu.gensokyo.integration.chat.advisor.PersistenceMemoryAdvisor;
import cn.nuaa.jensonxu.gensokyo.integration.chat.data.ModelConfig;
import cn.nuaa.jensonxu.gensokyo.integration.chat.memory.InSqlMemory;
import cn.nuaa.jensonxu.gensokyo.repository.mysql.chat.CustomChatMemoryRepository;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class ModelManager {

    private final NacosConfigManager nacosConfigManager;

    private final CustomChatMemoryRepository repository;

    @Value("${spring.cloud.nacos.config.extension-configs[0].data-id:llm_config.json}")
    private String dataId;

    @Value("${spring.cloud.nacos.config.extension-configs[0].group:GENSOKYO_AI_GROUP}")
    private String group;

    private final Map<String, ModelConfig> models = new ConcurrentHashMap<>();

    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    @Autowired
    public ModelManager(NacosConfigManager nacosConfigManager, CustomChatMemoryRepository repository) {
        this.repository = repository;
        this.nacosConfigManager = nacosConfigManager;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("[nacos] 初始化大模型参数配置...");
            ConfigService configService = nacosConfigManager.getConfigService();
            String configInfo = configService.getConfig(dataId, group, 5000);
            if(StringUtils.isBlank(configInfo)) {
                log.warn("[nacos] 未能获取到模型配置，dataId: {}, group: {}", dataId, group);
                return;
            }

            // 项目启动时加载模型参数配置
            JSONObject jsonObject = JSON.parseObject(configInfo);
            for(String modelName : jsonObject.keySet()) {
                ModelConfig modelConfig = jsonObject.getObject(modelName, ModelConfig.class);
                modelConfig.setModelName(modelName);
                models.put(modelName, modelConfig);
                log.info("[nacos] {}模型配置加载成功: {}", modelConfig.getModelName(), JSON.toJSONString(modelConfig));
            }
            log.info("[nacos] 模型配置初始化成功");

            // 配置 nacos 监听器，用于动态更新模型配置
            configService.addListener(
                    dataId,
                    group,
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
                                log.info("[nacos] {}模型配置更新成功: {}", modelConfig.getModelName(), JSON.toJSONString(modelConfig));
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
        ModelConfig.Parameters params = modelConfig.getParameters();
        log.info("find model config: {}", JSON.toJSONString(modelConfig));
        log.info("base url: {}", modelConfig.getBaseUrl());

        DashScopeApi api = DashScopeApi.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .build();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(modelConfig.getModelName())
                .withMaxToken(params.getMaxTokens())
                .withTemperature(params.getTemperature())
                .withTopP(params.getTopP())
                .build();
        DashScopeChatModel model = DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();
        ChatMemory chatMemory = new InSqlMemory(repository);

        // 创建并返回 ChatClient
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        new PersistenceMemoryAdvisor(chatMemory)
                )
                .build();
    }
}
