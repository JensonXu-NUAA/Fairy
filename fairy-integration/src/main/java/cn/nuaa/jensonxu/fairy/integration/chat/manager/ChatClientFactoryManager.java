package cn.nuaa.jensonxu.fairy.integration.chat.manager;

import cn.nuaa.jensonxu.fairy.integration.chat.data.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.chat.factory.BaseChatClientFactory;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatClient 工厂管理器
 * 负责根据 provider 选择合适的工厂
 */
@Slf4j
@Component
public class ChatClientFactoryManager {

    private final Map<String, BaseChatClientFactory> factoryMap = new ConcurrentHashMap<>();

    @Autowired
    public ChatClientFactoryManager(List<BaseChatClientFactory> factories) {
        // 自动注册所有工厂实现
        for (BaseChatClientFactory factory : factories) {
            factoryMap.put(factory.getProviderName().toLowerCase(), factory);
        }
    }

    /**
     * 根据模型配置创建 ChatClient
     */
    public ChatClient createChatClient(ModelConfig modelConfig) {
        String provider = modelConfig.getProvider();
        if (StringUtils.isEmpty(provider)) {
            throw new IllegalArgumentException("模型提供商 (provider) 不能为空");
        }

        // 查找支持该提供商的工厂
        BaseChatClientFactory factory = findFactory(provider);
        if (factory == null) {
            throw new IllegalArgumentException(String.format("不支持的模型提供商: %s，可用提供商: %s", provider, factoryMap.keySet()));
        }

        log.info("[chat client] 创建{}模型请求: {}", factory.getProviderName(), JSON.toJSONString(modelConfig));
        return factory.createChatClient(modelConfig);
    }

    /**
     * 查找支持指定提供商的工厂
     */
    private BaseChatClientFactory findFactory(String provider) {
        return factoryMap.get(provider.toLowerCase());
    }

    /**
     * 检查是否支持指定的提供商
     */
    public boolean isProviderSupported(String provider) {
        return findFactory(provider) != null;
    }

    /**
     * 获取所有支持的提供商
     */
    public List<String> getSupportedProviders() {
        return factoryMap.values().stream()
                .map(BaseChatClientFactory::getProviderName)
                .toList();
    }
}
