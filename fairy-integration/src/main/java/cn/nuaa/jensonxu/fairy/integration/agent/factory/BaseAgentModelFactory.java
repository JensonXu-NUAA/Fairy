package cn.nuaa.jensonxu.fairy.integration.agent.factory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallback;

/**
 * Agent 模型工厂抽象基类
 */
public abstract class BaseAgentModelFactory {

    /**
     * 判断当前工厂是否支持指定的模型提供商
     *
     * @param provider 提供商标识，如 "dashscope"、"deepseek"、"zhipuai"
     */
    public abstract boolean supports(String provider);

    /**
     * 返回当前工厂负责的提供商名称
     * 用于 AgentModelFactoryManager 注册时的 key
     */
    public abstract String getProviderName();

    /**
     * 根据模型配置创建 ChatModel
     * 子类实现各自提供商 SDK 的初始化逻辑
     *
     * @param modelConfig 来自 Nacos 的模型配置（含 apiKey、baseUrl、参数等）
     * @return 对应提供商的 ChatModel 实例
     */
    public abstract ReactAgent createAgent(ModelConfig modelConfig, ToolCallback[] tools);
}
