package cn.nuaa.jensonxu.fairy.common.rocketmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * RocketMQ 消息发送抽象模板类
 * 使用模板方法模式封装消息发送的通用流程
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRocketMQProducer {

    protected RocketMQTemplate template;

    /**
     * 构造器（由子类调用）
     */
    protected AbstractRocketMQProducer(RocketMQTemplate rocketMQTemplate) {
        this.template = rocketMQTemplate;
    }

    /**
     * 同步发送消息
     * @param topic 主题
     * @param payload 消息内容
     * @return 发送结果
     */
    public SendResult syncSend(String topic, Object payload) {
        try {
            beforeSend(topic, payload);  // 前置处理
            Message<?> message = buildMessage(payload);  // 构建消息
            SendResult result = template.syncSend(topic, message);  // 发送消息
            afterSend(topic, payload, result);  // 后置处理
            log.info("[rocketmq] 同步发送消息成功, topic: {}, msgId: {}", topic, result.getMsgId());
            return result;
        }catch (Exception e) {
            onSendError(topic, payload, e);  // 异常处理
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步发送消息
     * @param topic 主题
     * @param payload 消息内容
     */
    public void asyncSend(String topic, Object payload) {
        try {
            beforeSend(topic, payload);  // 前置处理
            Message<?> message = buildMessage(payload);  // 构建消息
            template.asyncSend(topic, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("[rocketmq] 异步发送消息成功, topic: {}, msgId: {}", topic, sendResult.getMsgId());
                    afterSend(topic, payload, sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("[rocketmq] 异步发送消息失败, topic: {}", topic, throwable);
                    onSendError(topic, payload, throwable);
                }
            });
        } catch (Exception e) {
            onSendError(topic, payload, e);
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 单向发送消息（不关心结果）
     * @param topic 主题
     * @param payload 消息内容
     */
    public void sendOneWay(String topic, Object payload) {
        try {
            beforeSend(topic, payload);
            Message<?> message = buildMessage(payload);
            template.sendOneWay(topic, message);
            log.info("[rocketmq] 单向发送消息, topic: {}", topic);
        } catch (Exception e) {
            onSendError(topic, payload, e);
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建消息（子类可以覆盖以自定义消息格式）
     */
    protected Message<?> buildMessage(Object payload) {
        return MessageBuilder.withPayload(payload).build();
    }

    /**
     * 发送前的钩子方法
     */
    protected void beforeSend(String topic, Object payload) {
    }

    /**
     * 发送后的钩子方法
     */
    protected void afterSend(String topic, Object payload, SendResult result) {
    }

    /**
     * 发送异常的钩子方法
     */
    protected void onSendError(String topic, Object payload, Throwable throwable) {
        log.error("[rocketmq] 消息发送异常, topic: {}, payload: {}", topic, payload, throwable);
    }
}
