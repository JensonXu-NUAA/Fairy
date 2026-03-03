package cn.nuaa.jensonxu.fairy.common.data.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片段落模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageSection {

    /** 图片ID */
    private String imageId;

    /** 图片Base64数据（临时） */
    private String imageData;

    /** 位置信息 */
    private PositionInfo position;

    /** 宽度 */
    private int width;

    /** 高度 */
    private int height;

    /** MIME类型 */
    private String mimeType;
}
