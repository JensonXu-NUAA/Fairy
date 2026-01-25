package cn.nuaa.jensonxu.fairy.service.file;

import cn.nuaa.jensonxu.fairy.repository.minio.MinioProperties;

import io.minio.*;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传文件到 MinIO
     * @param file 文件
     * @param objectName 对象名称（文件在MinIO中的路径）
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        ensureBucketExists();  // 确保 bucket 存在

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("[minio] 文件上传成功, 文件id: {}", objectName);
        return objectName;
    }

    /**
     * 上传分片到 MinIO
     * @param chunkFile 分片文件
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @return 分片在 MinIO 中的存储路径
     */
    public String uploadChunk(MultipartFile chunkFile, String userId, String fileMd5, int chunkIndex) throws Exception {
        ensureBucketExists();  // 确保 bucket 存在
        String chunkPath = String.format("chunks/%s/%s/chunk_%d", userId, fileMd5, chunkIndex);  // 构建分片存储路径

        // 上传分片到 MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(chunkPath)
                        .stream(chunkFile.getInputStream(), chunkFile.getSize(), -1)
                        .contentType("application/octet-stream")  // 分片使用二进制流
                        .build()
        );

        log.info("[minio] 分片上传成功, 路径: {}, 大小: {}字节", chunkPath, chunkFile.getSize());
        return chunkPath;
    }

    /**
     * 合并分片文件为完整文件
     * @param fileMd5 文件MD5
     * @param fileName 目标文件名
     * @param totalChunks 总分片数
     * @return 合并后的文件路径
     */
    public String mergeChunks(String userId, String fileMd5, String fileName, int totalChunks) throws Exception {
        ensureBucketExists();  // 确保 bucket 存在

        // 1. 构建分片路径列表
        List<ComposeSource> sources = new ArrayList<>();
        for(int i = 0; i < totalChunks; i++) {
            if(!chunkExists(userId, fileMd5, i)) {
                throw new RuntimeException(String.format("分片 %d 不存在，无法合并", i));  // 验证分片是否存在
            }

            String chunkPath = String.format("chunks/%s/%s/chunk_%d", userId, fileMd5, i);
            sources.add(
                    ComposeSource.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(chunkPath)
                            .build()
            );
        }


        String finalFilePath = String.format("files/%s/%s", userId, fileName);  // 2. 构建最终文件路径
        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(finalFilePath)
                        .sources(sources)
                        .build()
        );

        log.info("[minio] 文件合并成功, user id: {}, MD5: {}, 最终路径: {}, 分片数: {}", userId, fileMd5, finalFilePath, totalChunks);
        return finalFilePath;
    }

        /**
         * 检查分片文件是否存在
         * @param userId 用户id
         * @param fileMd5 文件MD5
         * @param chunkIndex 分片索引
         * @return 是否存在
         */
    public boolean chunkExists(String userId, String fileMd5, int chunkIndex) {
        String chunkPath = String.format("chunks/%s/%s/chunk_%d", userId, fileMd5, chunkIndex);
        return fileExists(chunkPath);
    }

    /**
     * 删除分片文件夹（合并完成后清理）
     * @param userId 用户id
     * @param fileMd5 文件MD5
     * @return 删除的分片数量
     */
    public Integer deleteChunkFolder(String userId, String fileMd5) {
        int deletedCount = 0;
        String chunkPrefix = String.format("chunks/%s/%s/", userId, fileMd5);

        try {
            // 列出该文件夹下的所有分片
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(chunkPrefix)
                            .recursive(true)
                            .build()
            );

            for(Result<Item> result : results) {
                Item item = result.get();
                deleteFile(item.objectName());
                deletedCount++;
            }
            log.info("[minio] 分片文件夹清理完成, user id: {}, MD5: {}, 删除数量: {}", userId, fileMd5, deletedCount);
        } catch (Exception e) {
            log.error("[minio] 分片文件夹清理失败, user id: {}, MD5: {}", userId, fileMd5, e);
        }

        return deletedCount;
    }

    /**
     * 获取文件大小
     * @param objectName 对象名称
     * @return 文件大小（字节）
     */
    public long getFileSize(String objectName) throws Exception {
        StatObjectResponse metadata = getFileMetadata(objectName);
        return metadata.size();
    }

    /**
     * 删除分片文件
     * @param userId 用户id
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @return 是否删除成功
     */
    public boolean deleteChunk(String userId, String fileMd5, int chunkIndex) {
        String chunkPath = String.format("chunks/%s/%s/chunk_%d", userId, fileMd5, chunkIndex);
        return deleteFile(chunkPath);
    }

    /**
     * 从 MinIO 获取文件流
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream getFileStream(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 获取文件的元数据（包括文件大小、类型等）
     * @param objectName 对象名称
     * @return 文件元数据
     */
    public StatObjectResponse getFileMetadata(String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    public boolean deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 确保 bucket 存在，不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );
            log.info("[minio] bucket不存在, 创建bucket: {}", minioProperties.getBucketName());
        }
    }
}
