package cn.nuaa.jensonxu.fairy.common.data.file.response.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 分片合并结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMergeResultVO {

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 合并后文件在 MinIO 中的最终路径
     */
    private String finalFilePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 是否合并成功
     */
    private Boolean mergeSuccess;

    /**
     * 合并耗时（毫秒）
     */
    private Long mergeDuration;

    /**
     * 文件访问URL（可选）
     */
    private String fileUrl;

    /**
     * 合并完成时间戳
     */
    private Long mergeTime;
}
