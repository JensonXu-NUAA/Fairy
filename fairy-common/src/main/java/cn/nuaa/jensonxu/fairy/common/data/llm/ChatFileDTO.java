package cn.nuaa.jensonxu.fairy.common.data.llm;

import lombok.Data;

/**
 * 聊天附件文件引用
 */
@Data
public class ChatFileDTO {

    /**
     * 文件下载 URL
     */
    private String fileId;

    /**
     * 原始文件名（用于类型判断，如 report.pdf、photo.png）
     */
    private String fileName;
}

