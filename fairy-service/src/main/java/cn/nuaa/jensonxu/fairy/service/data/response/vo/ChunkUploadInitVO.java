package cn.nuaa.jensonxu.fairy.service.data.response.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadInitVO {

    /**
     * 文件MD5（文件唯一标识）
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
     * 是否需要上传（false表示文件已存在，可秒传）
     */
    private Boolean needUpload;

    /**
     * 已上传的分片数量
     */
    private Long uploadedChunks;

    /**
     * 上传进度（百分比，0-100）
     */
    private Double uploadProgress;
}
