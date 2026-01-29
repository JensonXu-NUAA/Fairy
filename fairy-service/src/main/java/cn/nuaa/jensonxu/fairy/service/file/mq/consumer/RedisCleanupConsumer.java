package cn.nuaa.jensonxu.fairy.service.file.mq.consumer;

import cn.nuaa.jensonxu.fairy.common.repository.redis.RedisUtil;
import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQConsumer;
import cn.nuaa.jensonxu.fairy.service.file.mq.producer.FileCleanupProducer;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;

/**
 * Redis 清理消费者
 * 负责异步清理文件上传相关的 Redis 缓存数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "TOPIC_FILE_CLEANUP",
        selectorExpression = "REDIS",  // 只消费 REDIS 标签的消息
        consumerGroup = "fairy-redis-cleanup-consumer-group"
)
public class RedisCleanupConsumer extends AbstractRocketMQConsumer<FileCleanupProducer.RedisCleanupMessage> implements RocketMQListener<FileCleanupProducer.RedisCleanupMessage> {
    private final RedisUtil redisUtil;
    private static final String CHUNK_STATUS_PREFIX = "upload:chunk:";
    private static final String FILE_META_PREFIX = "upload:meta:";

    @Override
    public void onMessage(FileCleanupProducer.RedisCleanupMessage message) {
        consume(message);
    }

    @Override
    protected Boolean validateMessage(FileCleanupProducer.RedisCleanupMessage message) {
        if (message == null) {
            return false;
        }
        if (message.getUserId() == null || message.getUserId().trim().isEmpty()) {
            log.warn("[consumer] userId 为空");
            return false;
        }
        if (message.getFileMd5() == null || message.getFileMd5().trim().isEmpty()) {
            log.warn("[consumer] fileMd5 为空");
            return false;
        }
        return true;
    }

    @Override
    protected boolean doConsume(FileCleanupProducer.RedisCleanupMessage message) {
        String userId = message.getUserId();
        String fileMd5 = message.getFileMd5();

        log.info("[consumer] 开始清理 Redis 数据, userId: {}, fileMd5: {}", userId, fileMd5);

        try {
            // 删除分片状态位图
            String chunkStatusKey = CHUNK_STATUS_PREFIX + userId + ":" + fileMd5;
            Boolean deleted1 = redisUtil.delete(chunkStatusKey);

            // 删除文件元数据
            String fileMetaKey = FILE_META_PREFIX + userId + ":" + fileMd5;
            Boolean deleted2 = redisUtil.delete(fileMetaKey);

            log.info("[consumer] Redis 清理完成, userId: {}, fileMd5: {}, 分片状态删除: {}, 元数据删除: {}",
                    userId, fileMd5, deleted1, deleted2);

            return true;

        } catch (Exception e) {
            log.error("[consumer] Redis 清理失败, userId: {}, fileMd5: {}", userId, fileMd5, e);
            return false;
        }
    }

    @Override
    protected void afterConsumeSuccess(FileCleanupProducer.RedisCleanupMessage message) {
        log.info("[consumer] Redis 清理成功, userId: {}, fileMd5: {}, 耗时: {}ms",
                message.getUserId(), message.getFileMd5(),
                System.currentTimeMillis() - message.getTimestamp());
    }
}
