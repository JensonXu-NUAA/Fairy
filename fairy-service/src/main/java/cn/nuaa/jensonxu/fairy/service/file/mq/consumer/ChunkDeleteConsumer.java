package cn.nuaa.jensonxu.fairy.service.file.mq.consumer;

import cn.nuaa.jensonxu.fairy.common.rocketmq.AbstractRocketMQConsumer;
import cn.nuaa.jensonxu.fairy.service.file.FileService;
import cn.nuaa.jensonxu.fairy.service.file.mq.producer.FileCleanupProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 分片文件删除消费者
 * 负责异步删除 MinIO 中的分片文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "TOPIC_FILE_CLEANUP",
        selectorExpression = "CHUNK",  // 只消费 CHUNK 标签的消息
        consumerGroup = "fairy-chunk-delete-consumer-group"
)
public class ChunkDeleteConsumer extends AbstractRocketMQConsumer<FileCleanupProducer.ChunkDeleteMessage> implements RocketMQListener<FileCleanupProducer.ChunkDeleteMessage> {

    private final FileService fileService;

    @Override
    public void onMessage(FileCleanupProducer.ChunkDeleteMessage message) {
        // 调用模板方法
        consume(message);
    }

    @Override
    protected Boolean validateMessage(FileCleanupProducer.ChunkDeleteMessage message) {
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
    protected boolean doConsume(FileCleanupProducer.ChunkDeleteMessage message) {
        String userId = message.getUserId();
        String fileMd5 = message.getFileMd5();
        log.info("[consumer] 开始删除分片文件, userId: {}, fileMd5: {}", userId, fileMd5);

        try {
            int deletedCount = fileService.deleteChunkFolder(userId, fileMd5);  // 调用 FileService 删除分片文件夹
            log.info("[consumer] 分片文件删除完成, userId: {}, fileMd5: {}, 删除数量: {}", userId, fileMd5, deletedCount);
            return true;
        } catch (Exception e) {
            log.error("[consumer] 分片文件删除失败, userId: {}, fileMd5: {}", userId, fileMd5, e);
            return false;
        }
    }

    @Override
    protected void afterConsumeSuccess(FileCleanupProducer.ChunkDeleteMessage message) {
        log.info("[ChunkDeleteConsumer] 分片文件删除成功, userId: {}, fileMd5: {}, 耗时: {}ms",
                message.getUserId(), message.getFileMd5(),
                System.currentTimeMillis() - message.getTimestamp());
    }
}
