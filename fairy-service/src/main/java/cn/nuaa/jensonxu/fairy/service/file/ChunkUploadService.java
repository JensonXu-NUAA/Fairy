package cn.nuaa.jensonxu.fairy.service.file;

import cn.nuaa.jensonxu.fairy.repository.redis.RedisUtil;
import cn.nuaa.jensonxu.fairy.service.data.request.ChunkInitDTO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkMergeResultVO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkStatusVO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkUploadInitVO;
import cn.nuaa.jensonxu.fairy.service.data.response.vo.ChunkUploadResultVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final RedisUtil redisUtil;
    private final FileService fileService;
    private static final String CHUNK_STATUS_PREFIX = "upload:chunk:";
    private static final String FILE_META_PREFIX = "upload:meta:";
    private static final long DEFAULT_EXPIRE_HOURS = 24;  // 默认过期时间24小时

    /**
     * 初始化分片上传
     * @param request 初始化请求
     * @return 初始化响应
     */
    public ChunkUploadInitVO initChunkUpload(ChunkInitDTO request) {
        String fileMd5 = request.getFileMd5();
        Map<Object, Object> existingMetadata = getFileMetadata(fileMd5);  // 检查是否已有上传记录（支持断点续传）

        if (existingMetadata != null && !existingMetadata.isEmpty()) {
            log.info("[chunk] 检测到文件 {} 存在上传记录，开始断点续传", fileMd5);  // 已有上传记录，返回断点续传信息
            Long uploadedCount = getUploadedChunkCount(fileMd5);
            Integer totalChunks = (Integer) existingMetadata.get("totalChunks");
            Double progress = (uploadedCount * 100.0) / totalChunks;

            return ChunkUploadInitVO.builder()
                    .fileMd5(fileMd5)
                    .fileName((String) existingMetadata.get("fileName"))
                    .fileSize(Long.valueOf(existingMetadata.get("fileSize").toString()))
                    .totalChunks(totalChunks)
                    .needUpload(true)
                    .uploadedChunks(uploadedCount)
                    .uploadProgress(Double.parseDouble(String.format("%.2f", progress)))
                    .build();
        } else {
            log.info("[chunk] 新文件上传初始化, 文件名: {}, MD5: {}", request.getFileName(), fileMd5);  // 新上传，保存元数据到Redis
            saveFileMetadata(
                    fileMd5,
                    request.getFileName(),
                    request.getTotalChunks(),
                    request.getFileSize()
            );

            return ChunkUploadInitVO.builder()
                    .fileMd5(fileMd5)
                    .fileName(request.getFileName())
                    .fileSize(request.getFileSize())
                    .totalChunks(request.getTotalChunks())
                    .needUpload(true)
                    .uploadedChunks(0L)
                    .uploadProgress(0.0)
                    .build();
        }
    }

    /**
     * 上传分片
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @param chunkFile 分片文件
     * @return 上传结果
     */
    public ChunkUploadResultVO uploadChunk(String fileMd5, Integer chunkIndex, MultipartFile chunkFile) {
        try {
            // 1. 获取文件元数据
            Map<Object, Object> metadata = getFileMetadata(fileMd5);
            if (metadata == null || metadata.isEmpty()) {
                throw new RuntimeException("文件元数据不存在，请先调用初始化接口");
            }

            Integer totalChunks = (Integer) metadata.get("totalChunks");

            // 2. 校验分片索引是否合法
            if (chunkIndex < 0 || chunkIndex >= totalChunks) {
                throw new RuntimeException(
                        String.format("分片索引越界，有效范围: 0-%d，当前索引: %d", totalChunks - 1, chunkIndex)
                );
            }

            Boolean alreadyUploaded = isChunkUploaded(fileMd5, chunkIndex);  // 3. 检查分片是否已上传（幂等性处理）
            String chunkPath;

            if (Boolean.TRUE.equals(alreadyUploaded)) {
                chunkPath = String.format("chunks/%s/chunk_%d", fileMd5, chunkIndex);  // 分片已存在，直接返回成功
                log.info("[chunk] 分片已存在，跳过上传, MD5: {}, 分片: {}", fileMd5, chunkIndex);
            } else {
                chunkPath = fileService.uploadChunk(chunkFile, fileMd5, chunkIndex);  // 4. 上传分片到MinIO
                markChunkUploaded(fileMd5, chunkIndex);  // 5. 更新Redis中的分片状态
                log.info("[chunk] 分片上传成功, MD5: {}, 分片: {}, 路径: {}", fileMd5, chunkIndex, chunkPath);
            }

            // 6. 计算上传进度
            Long uploadedCount = getUploadedChunkCount(fileMd5);
            Double progress = (uploadedCount * 100.0) / totalChunks;
            boolean isComplete = uploadedCount.equals(totalChunks.longValue());

            log.info("[chunk] 上传进度, MD5: {}, 已上传: {}/{}, 进度: {}%, 是否完成: {}",
                    fileMd5, uploadedCount, totalChunks,
                    String.format("%.2f", progress), isComplete);

            // 7. 构建返回结果
            return ChunkUploadResultVO.builder()
                    .fileMd5(fileMd5)
                    .chunkIndex(chunkIndex)
                    .chunkPath(chunkPath)
                    .alreadyUploaded(Boolean.TRUE.equals(alreadyUploaded))
                    .uploadedChunks(uploadedCount)
                    .totalChunks(totalChunks)
                    .uploadProgress(Double.parseDouble(String.format("%.2f", progress)))
                    .isComplete(isComplete)
                    .build();

        } catch (Exception e) {
            log.error("[chunk] 分片上传失败, MD5: {}, 分片: {}", fileMd5, chunkIndex, e);
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询分片上传状态
     * @param fileMd5 文件MD5
     * @return 分片状态
     */
    public ChunkStatusVO queryChunkStatus(String fileMd5) {
        // 1. 获取文件元数据
        Map<Object, Object> metadata = getFileMetadata(fileMd5);
        if (metadata == null || metadata.isEmpty()) {
            throw new RuntimeException("文件元数据不存在，请先调用初始化接口");
        }

        // 2. 提取元数据信息
        String fileName = (String) metadata.get("fileName");
        Long fileSize = Long.valueOf(metadata.get("fileSize").toString());
        Integer totalChunks = (Integer) metadata.get("totalChunks");

        // 3. 遍历所有分片，分类为已上传和未上传
        List<Integer> uploadedChunks = new ArrayList<>();
        List<Integer> missingChunks = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            Boolean uploaded = isChunkUploaded(fileMd5, i);
            if (Boolean.TRUE.equals(uploaded)) {
                uploadedChunks.add(i);
            } else {
                missingChunks.add(i);
            }
        }

        // 4. 计算上传进度
        int uploadedCount = uploadedChunks.size();
        Double progress = (uploadedCount * 100.0) / totalChunks;
        boolean isComplete = uploadedCount == totalChunks;
        log.info("[chunk] 分片状态查询，MD5: {}, 已上传: {}/{}, 进度: {}%, 完成: {}", fileMd5, uploadedCount, totalChunks, String.format("%.2f", progress), isComplete);

        // 5. 构建返回结果
        return ChunkStatusVO.builder()
                .fileMd5(fileMd5)
                .fileName(fileName)
                .fileSize(fileSize)
                .totalChunks(totalChunks)
                .uploadedChunks(uploadedChunks)
                .missingChunks(missingChunks)
                .uploadedCount(uploadedCount)
                .uploadProgress(Double.parseDouble(String.format("%.2f", progress)))
                .isComplete(isComplete)
                .build();
    }

    /**
     * 合并分片文件
     * @param fileMd5 文件MD5
     * @return 合并结果
     */
    public ChunkMergeResultVO mergeChunks(String fileMd5) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取文件元数据
            Map<Object, Object> metadata = getFileMetadata(fileMd5);
            if (metadata == null || metadata.isEmpty()) {
                throw new RuntimeException("文件元数据不存在，请先调用初始化接口");
            }

            String fileName = (String) metadata.get("fileName");
            long fileSize = Long.parseLong(metadata.get("fileSize").toString());
            Integer totalChunks = (Integer) metadata.get("totalChunks");

            // 2. 验证所有分片是否已上传
            Long uploadedCount = getUploadedChunkCount(fileMd5);
            if (!uploadedCount.equals(totalChunks.longValue())) {
                throw new RuntimeException(
                        String.format("分片未全部上传，已上传: %d/%d", uploadedCount, totalChunks)
                );
            }

            log.info("[chunk] 开始合并文件, MD5: {}, 文件名: {}, 总分片数: {}",
                    fileMd5, fileName, totalChunks);


            String finalFilePath = fileService.mergeChunks(fileMd5, fileName, totalChunks);  // 3. 调用FileService进行合并
            long actualFileSize = fileService.getFileSize(finalFilePath);  // 4. 获取合并后文件的实际大小
            if (actualFileSize != fileSize) {
                log.warn("[chunk] 文件大小不一致, 预期: {} 字节, 实际: {} 字节", fileSize, actualFileSize);  // 5. 验证文件大小
            }


            cleanupFileData(fileMd5);  // 6. 清理Redis中的临时数据
            log.info("[chunk] Redis临时数据已清理, MD5: {}", fileMd5);

            int deletedChunks = fileService.deleteChunkFolder(fileMd5);  // 7. 异步删除分片文件（可以发送MQ消息，这里先直接删除）
            log.info("[chunk] 分片文件已清理, 删除数量: {}", deletedChunks);


            long mergeDuration = System.currentTimeMillis() - startTime;  // 8. 计算合并耗时
            log.info("[chunk] 文件合并成功, MD5: {}, 最终路径: {}, 耗时: {} ms", fileMd5, finalFilePath, mergeDuration);

            return ChunkMergeResultVO.builder()
                    .fileMd5(fileMd5)
                    .fileName(fileName)
                    .finalFilePath(finalFilePath)
                    .fileSize(actualFileSize)
                    .totalChunks(totalChunks)
                    .mergeSuccess(true)
                    .mergeDuration(mergeDuration)
                    .mergeTime(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            long mergeDuration = System.currentTimeMillis() - startTime;
            log.error("[chunk] 文件合并失败, MD5: {}, 耗时: {}ms", fileMd5, mergeDuration, e);

            return ChunkMergeResultVO.builder()
                    .fileMd5(fileMd5)
                    .mergeSuccess(false)
                    .mergeDuration(mergeDuration)
                    .build();
        }
    }
    
    /**
     * 标记某个分片已上传
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     */
    public void markChunkUploaded(String fileMd5, int chunkIndex) {
        String key = CHUNK_STATUS_PREFIX + fileMd5;
        redisUtil.setBit(key, chunkIndex, true);
        redisUtil.expire(key, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);  // 设置过期时间，防止垃圾数据堆积
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
