package cn.nuaa.jensonxu.fairy.integration.agent.memory.mq;

import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQProducer;
import cn.nuaa.jensonxu.fairy.common.rocketmq.message.AgentMemoryExtractMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 长期记忆提炼消息生产者
 * 每轮对话 MySQL 写入成功后，由 AfterAgentMemoryHook 调用投递提炼触发消息
 */
@Slf4j
@Component
public class AgentMemoryExtractProducer extends AbstractRocketMQProducer {

    private static final String TOPIC = "agent-memory-extract";

    public AgentMemoryExtractProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    /**
     * 投递长期记忆提炼触发消息（异步，fire-and-forget）
     */
    public void sendExtractMessage(AgentMemoryExtractMessage message) {
        asyncSend(TOPIC, message);
    }

    @Override
    protected void onSendError(String topic, Object payload, Throwable throwable) {
        log.error("[memory] 记忆提炼消息投递失败，本轮提炼将跳过, topic: {}, payload: {}", topic, payload, throwable);
    }
}