package cn.nuaa.jensonxu.fairy.common.data.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 位置信息
 * 用于计算文本块之间的空间距离
 */
@Data
@AllArgsConstructor
public class PositionInfo {

    /** 页码（从0开始） */
    private int pageNum;

    /** 顶部坐标 */
    private double top;

    /** 底部坐标 */
    private double bottom;

    /** 左侧坐标 */
    private double left;

    /** 右侧坐标 */
    private double right;

    /**
     * 计算与另一个位置的距离
     * 同页：按垂直距离
     * 跨页：页距 + 垂直距离
     */
    public double distanceTo(PositionInfo other) {
        if (this.pageNum == other.pageNum) {
            if (this.bottom <= other.top) {
                return other.top - this.bottom;
            } else if (other.bottom <= this.top) {
                return this.top - other.bottom;
            } else {
                return 0;
            }
        } else {
            int pageDist = Math.abs(this.pageNum - other.pageNum);
            double verticalDist = (this.pageNum < other.pageNum)
                    ? (1000 - this.bottom + other.top)
                    : (1000 - other.bottom + this.top);
            return pageDist * 1000 + verticalDist;
        }
    }
}
