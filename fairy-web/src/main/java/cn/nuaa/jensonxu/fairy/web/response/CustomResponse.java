package cn.nuaa.jensonxu.fairy.web.response;

import lombok.Data;

@Data
public class CustomResponse<T> {

    /**
     * 响应码（200成功，其他失败）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public CustomResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public CustomResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> CustomResponse<T> success(T data) {
        return new CustomResponse<>(200, "操作成功", data);
    }

    /**
     * 成功响应（不带数据）
     */
    public static <T> CustomResponse<T> success(String message) {
        return new CustomResponse<>(200, message, null);
    }

    /**
     * 失败响应
     */
    public static <T> CustomResponse<T> error(String message) {
        return new CustomResponse<>(500, message, null);
    }

    /**
     * 自定义响应
     */
    public static <T> CustomResponse<T> build(Integer code, String message, T data) {
        return new CustomResponse<>(code, message, data);
    }
}
