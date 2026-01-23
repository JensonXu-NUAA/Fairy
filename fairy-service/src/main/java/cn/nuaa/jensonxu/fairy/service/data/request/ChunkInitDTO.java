package cn.nuaa.jensonxu.fairy.service.data.request;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ChunkInitDTO {
    /**
     * 文件名（原始文件名）
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件MD5值（前端计算）
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Min(value = 1, message = "总分片数必须大于0")
    private Integer totalChunks;

    /**
     * 分片大小（字节，默认1MB = 1048576字节）
     */
    private Long chunkSize = 1048576L;
}
