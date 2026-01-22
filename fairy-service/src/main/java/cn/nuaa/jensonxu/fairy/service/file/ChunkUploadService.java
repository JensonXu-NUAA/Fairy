package cn.nuaa.jensonxu.fairy.service.file;

import cn.nuaa.jensonxu.fairy.repository.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final RedisUtil redisUtil;
    private static final String CHUNK_STATUS_PREFIX = "upload:chunk:";
    private static final String FILE_META_PREFIX = "upload:meta:";
    private static final long DEFAULT_EXPIRE_HOURS = 24; // 默认过期时间24小时

    /**
     * 标记某个分片已上传
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     */
    public void markChunkUploaded(String fileMd5, int chunkIndex) {
        String key = CHUNK_STATUS_PREFIX + fileMd5;
        redisUtil.setBit(key, chunkIndex, true);
        // 设置过期时间，防止垃圾数据堆积
        redisUtil.expire(key, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 检查某个分片是否已上传
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @return true 表示已上传
     */
    public Boolean isChunkUploaded(String fileMd5, int chunkIndex) {
        String key = CHUNK_STATUS_PREFIX + fileMd5;
        return redisUtil.getBit(key, chunkIndex);
    }

    /**
     * 获取已上传的分片数量
     * @param fileMd5 文件MD5
     * @return 已上传分片数
     */
    public Long getUploadedChunkCount(String fileMd5) {
        String key = CHUNK_STATUS_PREFIX + fileMd5;
        return redisUtil.bitCount(key);
    }

    /**
     * 保存文件上传元数据
     * @param fileMd5 文件MD5
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @param fileSize 文件大小
     */
    public void saveFileMetadata(String fileMd5, String fileName, int totalChunks, long fileSize) {
        String key = FILE_META_PREFIX + fileMd5;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("totalChunks", totalChunks);
        metadata.put("fileSize", fileSize);
        metadata.put("uploadTime", System.currentTimeMillis());

        redisUtil.hSetAll(key, metadata);
        redisUtil.expire(key, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取文件上传元数据
     * @param fileMd5 文件MD5
     * @return 元数据 Map
     */
    public Map<Object, Object> getFileMetadata(String fileMd5) {
        String key = FILE_META_PREFIX + fileMd5;
        return redisUtil.hGetAll(key);
    }

    /**
     * 清理文件相关的 Redis 数据（合并完成后调用）
     * @param fileMd5 文件 MD5
     */
    public void cleanupFileData(String fileMd5) {
        redisUtil.delete(CHUNK_STATUS_PREFIX + fileMd5);
        redisUtil.delete(FILE_META_PREFIX + fileMd5);
    }
}
