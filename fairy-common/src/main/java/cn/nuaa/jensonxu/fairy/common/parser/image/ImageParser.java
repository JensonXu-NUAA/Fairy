package cn.nuaa.jensonxu.fairy.common.parser.image;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 图片解析器
 * 将图片文件转换为 Base64 编码，支持 png、jpg、jpeg、svg
 */
@Slf4j
public class ImageParser {

    private static final Map<String, String> SUPPORTED_EXTENSIONS = new LinkedHashMap<>();

    static {
        SUPPORTED_EXTENSIONS.put(".png", "image/png");
        SUPPORTED_EXTENSIONS.put(".jpg", "image/jpeg");
        SUPPORTED_EXTENSIONS.put(".jpeg", "image/jpeg");
        SUPPORTED_EXTENSIONS.put(".svg", "image/svg+xml");
    }

    /**
     * 判断文件是否为支持的图片格式
     */
    public static boolean isImage(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.keySet().stream().anyMatch(lowerName::endsWith);
    }

    /**
     * 根据文件名获取 MIME 类型
     */
    public static String getMimeType(String fileName) {
        if (fileName == null) {
            return null;
        }
        String lowerName = fileName.toLowerCase();
        for (Map.Entry<String, String> entry : SUPPORTED_EXTENSIONS.entrySet()) {
            if (lowerName.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析图片输入流，返回 Base64 编码结果
     */
    public static ImageParseResult parse(InputStream inputStream, String fileName) {
        long startTime = System.currentTimeMillis();

        if (inputStream == null) {
            return ImageParseResult.failure("输入流为空");
        }

        String mimeType = getMimeType(fileName);
        if (mimeType == null) {
            return ImageParseResult.failure("不支持的图片格式: " + fileName);
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }

            byte[] imageBytes = buffer.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[image] 图片解析成功: {}, 大小: {} bytes, 耗时: {}ms", fileName, imageBytes.length, duration);
            return ImageParseResult.success(base64, mimeType, fileName, imageBytes.length, duration);
        } catch (Exception e) {
            log.error("[image] 图片解析失败: {}", fileName, e);
            return ImageParseResult.failure("图片解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有支持的图片扩展名
     */
    public static Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.keySet();
    }
}
