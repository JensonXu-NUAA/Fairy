package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQProducer;
import cn.nuaa.jensonxu.fairy.common.rocketmq.message.AgentMemoryPersistMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 短期记忆持久化消息生产者
 * 在 ShortTermRedisSaveHook 写入 MySQL 失败时，将消息体投递至 RocketMQ 进行异步重试
 */
@Slf4j
@Component
public class AgentMemoryMessageProducer extends AbstractRocketMQProducer {

    private static final String TOPIC = "agent-memory-persist";

    public AgentMemoryMessageProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    /**
     * 投递记忆持久化消息
     * 使用异步发送，不阻塞 Hook 的返回路径
     *
     * @param message 包含 sessionId、userId、humanContent、assistantContent 的消息体
     */
    public void sendPersistMessage(AgentMemoryPersistMessage message) {
        asyncSend(TOPIC, message);
    }

    @Override
    protected void onSendError(String topic, Object payload, Throwable throwable) {
        // 父类已有日志，此处升级为 ERROR 级别，便于监控告警
        log.error("[memory] 记忆持久化消息投递失败，消息将彻底丢失，需人工介入, topic: {}, payload: {}", topic, payload, throwable);
    }
}