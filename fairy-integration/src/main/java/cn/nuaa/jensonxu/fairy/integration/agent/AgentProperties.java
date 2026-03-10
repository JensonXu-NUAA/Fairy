package cn.nuaa.jensonxu.fairy.integration.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 全局配置属性
 * 绑定 application.yml 中 fairy.agent.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "fairy.agent")
public class AgentProperties {

    /**
     * 全局默认模型名称
     * 请求中 modelName 为空时使用此值
     * 需与 Nacos llm_config 中注册的 modelName 一致
     */
    private String defaultModel = "qwen-turbo";

    /**
     * ReAct 最大循环迭代次数上限
     * 请求中 maxIterations 为 null 或 0 时使用此值
     * AgentService 同时以此值作为请求值的硬上限，防止客户端滥用
     */
    private int maxIterations = 10;

    /**
     * 是否将 LLM 推理过程（Thought）推送给客户端
     * true：下发 agent_thinking 事件，用户可见推理链路
     * false：仅推送工具调用结果和最终答案，减少流量
     */
    private boolean streamThinking = true;

    /**
     * Agent 会话历史保留的最大轮次
     * 超出时截取最近 N 轮，避免上下文 token 超限
     */
    private int maxHistorySize = 10;
}