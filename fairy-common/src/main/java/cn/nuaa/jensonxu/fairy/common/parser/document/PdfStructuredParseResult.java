package cn.nuaa.jensonxu.fairy.common.parser.document;

import cn.nuaa.jensonxu.fairy.common.data.rag.ImageSection;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * PDF 结构化解析结果
 * 用于承载逐页提取后的文本段和图片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfStructuredParseResult {

    /** 是否成功 */
    private boolean success;

    /** 文本段列表（带位置） */
    private List<TextSection> textSections;

    /** 图片段列表（带位置） */
    private List<ImageSection> imageSections;

    /** 文档元数据 */
    private Map<String, String> metadata;

    /** 页数 */
    private int pageCount;

    /** 字符数 */
    private long charCount;

    /** 解析耗时（ms） */
    private long parseDuration;

    /** 错误信息 */
    private String errorMessage;
}

