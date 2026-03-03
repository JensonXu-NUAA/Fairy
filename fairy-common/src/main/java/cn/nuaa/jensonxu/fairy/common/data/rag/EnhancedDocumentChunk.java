package cn.nuaa.jensonxu.fairy.common.data.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 增强文档块
 * 用于承载图文混合场景下的分块结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedDocumentChunk {

    /** 块文本内容 */
    private String text;

    /** 元数据 */
    private Map<String, Object> metadata;

    /** 位置信息列表，约定格式：[page_num, top, bottom] */
    private List<List<Integer>> positions;

    /** 图片ID */
    private String imageId;

    /** 图片Base64数据（临时字段） */
    private String imageData;

    /** 块类型：text / image / table */
    private String chunkType;

    /** 父块ID（用于二次切分后回溯） */
    private String parentId;

    /** 媒体附加上下文文本 */
    private String contextText;

    /** token数量（估算值） */
    private int tokenCount;

    /** 原文偏移 */
    private long startOffset;
    private long endOffset;
}