package cn.nuaa.jensonxu.fairy.integration.chat.factory;

import cn.nuaa.jensonxu.fairy.integration.chat.data.ModelConfig;
import org.springframework.ai.chat.client.ChatClient;

public interface ChatClientFactory {
    /**
     * 判断是否支持该提供商
     */
    boolean supports(String provider);

    /**
     * 创建 ChatClient
     */
    ChatClient createChatClient(ModelConfig modelConfig);

    /**
     * 获取提供商名称
     */
    String getProviderName();
}
