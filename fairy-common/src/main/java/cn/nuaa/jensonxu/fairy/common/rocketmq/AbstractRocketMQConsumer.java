package cn.nuaa.jensonxu.fairy.common.rocketmq;

import lombok.extern.slf4j.Slf4j;

/**
 * RocketMQ 消息消费抽象模板类
 * 使用模板方法模式封装消息消费的通用流程
 */
@Slf4j
public abstract class AbstractRocketMQConsumer<T> {
    /**
     * 模板方法：消费消息的完整流程
     * @param message 消息内容
     */
    public void consume(T message) {
        try {
            // 前置处理（钩子方法）
            beforeConsume(message);

            // 参数校验
            if (!validateMessage(message)) {
                log.warn("[consumer] 消息校验失败, message: {}", message);
                onValidateFailed(message);
                return;
            }

            // 核心业务处理（由子类实现）
            boolean success = doConsume(message);

            if (success) {
                // 消费成功后置处理
                afterConsumeSuccess(message);
                log.info("[consumer] 消息消费成功, message: {}", message);
            } else {
                // 消费失败处理
                afterConsumeFailed(message);
                log.warn("[consumer] 消息消费失败, message: {}", message);
            }

        } catch (Exception e) {
            // 异常处理（钩子方法）
            onConsumeError(message, e);
            log.error("[consumer] 消息消费异常, message: {}", message, e);
            throw new RuntimeException("消息消费失败: " + e.getMessage(), e);
        }
    }

    /**
     * 消息校验（子类可以覆盖）
     * @param message 消息内容
     * @return true-校验通过, false-校验失败
     */
    protected Boolean validateMessage(T message) {
        return message != null;
    }

    /**
     * 消费前的钩子方法
     */
    protected void beforeConsume(T message) {
    }

    /**
     * 消费成功后的钩子方法
     */
    protected void afterConsumeSuccess(T message) {
    }

    /**
     * 消费失败后的钩子方法
     */
    protected void afterConsumeFailed(T message) {
    }

    /**
     * 校验失败的钩子方法
     */
    protected void onValidateFailed(T message) {
    }

    /**
     * 消费异常的钩子方法
     */
    protected void onConsumeError(T message, Throwable throwable) {
        log.error("[consumer] 消息消费异常, message: {}", message, throwable);
    }

    /**
     * 核心业务处理逻辑（子类必须实现）
     * @param message 消息内容
     * @return true-消费成功, false-消费失败
     */
    protected abstract boolean doConsume(T message);
}
