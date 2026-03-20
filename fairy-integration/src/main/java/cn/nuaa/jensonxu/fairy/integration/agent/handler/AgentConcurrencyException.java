package cn.nuaa.jensonxu.fairy.integration.agent.handler;

/**
 * Agent 并发限流异常
 * 当等待获取 Semaphore 许可超时时抛出，由 AgentHandler 捕获并映射为 429 错误 SSE 事件
 */
public class AgentConcurrencyException extends Exception {

    public AgentConcurrencyException(String message) {
        super(message);
    }
}