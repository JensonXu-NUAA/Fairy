package cn.nuaa.jensonxu.fairy.service.data.response.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResultVO {

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 当前上传的分片索引
     */
    private Integer chunkIndex;

    /**
     * MinIO 中的存储路径
     */
    private String chunkPath;

    /**
     * 是否已上传（true表示之前已上传，本次跳过）
     */
    private Boolean alreadyUploaded;

    /**
     * 已上传的分片总数
     */
    private Long uploadedChunks;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 上传进度（百分比，0-100）
     */
    private Double uploadProgress;

    /**
     * 是否全部上传完成
     */
    private Boolean isComplete;
}
