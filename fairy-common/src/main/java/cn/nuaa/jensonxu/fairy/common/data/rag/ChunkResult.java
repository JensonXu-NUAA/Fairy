package cn.nuaa.jensonxu.fairy.common.data.rag;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 分块结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {

    /** 分块列表 */
    private List<EnhancedDocumentChunk> chunks;

    /** 原始文档元数据 */
    private Map<String, Object> sourceMetadata;

    /** 分块耗时（ms） */
    private long duration;

    /** 分块数量 */
    private int chunkCount;

    /** 总token数 */
    private int totalTokens;

    /** 平均token数 */
    private double avgTokens;

    /** 是否包含图片 */
    private boolean hasImages;

    /** 是否包含表格 */
    private boolean hasTables;
}
