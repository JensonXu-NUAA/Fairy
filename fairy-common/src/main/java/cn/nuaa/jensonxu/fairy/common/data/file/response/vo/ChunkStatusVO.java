package cn.nuaa.jensonxu.fairy.common.data.file.response.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分片状态查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStatusVO {

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传的分片索引列表
     */
    private List<Integer> uploadedChunks;

    /**
     * 未上传的分片索引列表（缺失的分片）
     */
    private List<Integer> missingChunks;

    /**
     * 已上传的分片数量
     */
    private Integer uploadedCount;

    /**
     * 上传进度（百分比，0-100）
     */
    private Double uploadProgress;

    /**
     * 是否全部上传完成
     */
    private Boolean isComplete;
}
