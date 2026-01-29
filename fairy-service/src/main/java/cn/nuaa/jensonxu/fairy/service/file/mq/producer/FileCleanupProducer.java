package cn.nuaa.jensonxu.fairy.service.file.mq.producer;

import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQProducer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 文件清理消息生产者
 * 用于发送文件清理相关的异步任务消息
 */
@Slf4j
@Component
public class FileCleanupProducer extends AbstractRocketMQProducer {

    public static final String TOPIC_FILE_CLEANUP = "TOPIC_FILE_CLEANUP";

    public FileCleanupProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    /**
     * 发送 Redis 清理消息
     * @param userId 用户ID
     * @param fileMd5 文件MD5
     */
    public void sendRedisCleanupMessage(String userId, String fileMd5) {
        RedisCleanupMessage message = new RedisCleanupMessage(userId, fileMd5);
        asyncSend(TOPIC_FILE_CLEANUP + ":REDIS", message);
        log.info("[producer] 发送Redis清理消息, userId: {}, fileMd5: {}", userId, fileMd5);
    }

    /**
     * 发送分片文件删除消息
     * @param userId 用户ID
     * @param fileMd5 文件MD5
     */
    public void sendChunkDeleteMessage(String userId, String fileMd5) {
        FileCleanupProducer.ChunkDeleteMessage message = new FileCleanupProducer.ChunkDeleteMessage(userId, fileMd5);
        asyncSend(TOPIC_FILE_CLEANUP + ":CHUNK", message);
        log.info("[producer] 发送分片删除消息, userId: {}, fileMd5: {}", userId, fileMd5);
    }

    /**
     * Redis 清理消息实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RedisCleanupMessage {
        private String userId;
        private String fileMd5;
        private Long timestamp = System.currentTimeMillis();

        public RedisCleanupMessage(String userId, String fileMd5) {
            this.userId = userId;
            this.fileMd5 = fileMd5;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 分片删除消息实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChunkDeleteMessage {
        private String userId;
        private String fileMd5;
        private Long timestamp = System.currentTimeMillis();

        public ChunkDeleteMessage(String userId, String fileMd5) {
            this.userId = userId;
            this.fileMd5 = fileMd5;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
