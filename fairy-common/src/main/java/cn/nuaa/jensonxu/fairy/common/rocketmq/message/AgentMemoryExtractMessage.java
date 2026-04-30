package cn.nuaa.jensonxu.fairy.common.rocketmq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Agent 长期记忆提炼 MQ 消息体
 * 每轮对话结束后由 ShortTermRedisSaveHook 投递，
 * 由 AgentMemoryExtractConsumer 异步消费，调用 Summarizer 提炼并写入长期记忆
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMemoryExtractMessage implements Serializable {

    /** 会话 ID，消费者据此从 MySQL 加载本轮消息 */
    private String sessionId;

    /** 用户 ID */
    private String userId;

    /** 投递时间戳，用于日志追踪 */
    private long originTimestamp;
}