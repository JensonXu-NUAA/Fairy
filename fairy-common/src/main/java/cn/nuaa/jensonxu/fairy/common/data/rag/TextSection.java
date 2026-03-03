package cn.nuaa.jensonxu.fairy.common.data.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本段落模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSection {

    /** 文本内容 */
    private String text;

    /** 位置信息 */
    private PositionInfo position;

    /** 是否有标题 */
    private boolean title;

    /** 标题级别 */
    private int titleLevel;

    /** 是否有表格文本 */
    private boolean table;

    /** token 数量（估算） */
    private int tokenCount;
}
