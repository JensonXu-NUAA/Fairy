package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import lombok.Data;

import java.util.List;

/**
 * 分块器配置
 */
@Data
public class ChunkerConfig {

    /** 目标分块大小（token） */
    private int chunkTokenSize = 512;

    /** 分块重叠比例 [0,1) */
    private double overlappedPercent = 0.0;

    /** 主分隔符 */
    private List<String> delimiters = List.of("\n");

    /** 子级分隔符（可选） */
    private List<String> childrenDelimiters = List.of();

    /** 表格上下文 token 预算 */
    private int tableContextSize = 0;

    /** 图片上下文 token 预算 */
    private int imageContextSize = 0;

    /** 是否保留位置信息 */
    private boolean preservePosition = false;

    /** 最大单块 token */
    private int maxChunkSize = 1024;

    /** 最小单块 token */
    private int minChunkSize = 50;

    public void setOverlappedPercent(double overlappedPercent) {
        this.overlappedPercent = normalizeOverlappedPercent(overlappedPercent);
    }

    private double normalizeOverlappedPercent(double percent) {
        if (percent < 0) {
            return 0.0;
        }
        if (percent >= 1) {
            return 0.5;
        }
        return percent;
    }
}
