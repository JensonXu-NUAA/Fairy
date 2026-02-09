package cn.nuaa.jensonxu.fairy.common.document.parser.test;

import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.document.parser.impl.TikaDocumentParser;

import java.io.File;

/**
 * Tika 通用解析器测试
 * 用于测试 TikaDocumentParser 对多种文件格式的解析能力
 */
public class TikaParserTest {

    public static void main(String[] args) {
        // 测试多个文件类型
        String[] testFiles = {
                "C:\\Users\\15153\\Desktop\\新建 文本文档.txt",
                "D:\\Fairy\\fairy-common\\src\\main\\java\\cn\\nuaa\\jensonxu\\fairy\\common\\repository\\caffeine\\CaffeineConfig.java"
        };

        TikaDocumentParser parser = new TikaDocumentParser();

        // 显示支持的文件扩展名
        System.out.println("========== Tika 解析器支持格式 ==========");
        String[] supportedExtensions = parser.getSupportedExtensions();
        System.out.println("支持的文件扩展名数量: " + supportedExtensions.length);
        System.out.println("支持的扩展名:");
        for (String ext : supportedExtensions) {
            System.out.print(ext + " ");
        }
        System.out.println("\n");

        // 测试每个文件
        for (String filePath : testFiles) {
            testSingleFile(parser, filePath);
        }

        System.out.println("========== 所有测试完成 ==========");
    }

    /**
     * 测试单个文件
     */
    private static void testSingleFile(TikaDocumentParser parser, String filePath) {
        System.out.println("========== 测试文件: " + filePath + " ==========\n");
        File file = new File(filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.out.println("文件不存在，跳过测试\n");
            return;
        }

        // 检查是否支持该文件
        System.out.println("文件名: " + file.getName());
        System.out.println("文件大小: " + file.length() + " bytes");
        System.out.println("是否支持: " + parser.supports(file.getName()));
        System.out.println();

        if (!parser.supports(file.getName())) {
            System.out.println("该文件类型不被支持，跳过解析\n");
            return;
        }

        // 执行解析
        System.out.println("开始解析...");
        long startTime = System.currentTimeMillis();
        DocumentParseResult result = parser.parse(file);
        long endTime = System.currentTimeMillis();

        // 输出结果
        System.out.println("\n---------- 解析结果 ----------");
        System.out.println("解析状态: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("文档类型: " + result.getContentType());
        System.out.println("是否加密: " + result.isEncrypted());
        System.out.println("页数: " + result.getPageCount());
        System.out.println("字符数: " + result.getCharCount());
        System.out.println("实际解析耗时: " + (endTime - startTime) + " ms");
        System.out.println("结果中记录的解析耗时: " + result.getParseDuration() + " ms");

        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            System.out.println("\n---------- 元数据 ----------");
            result.getMetadata().forEach((key, value) ->
                    System.out.println(key + ": " + value));
        }

        if (result.isSuccess()) {
            System.out.println("\n---------- 文本内容预览 (前300字符) ----------");
            String content = result.getContent();
            if (content != null && !content.isEmpty()) {
                if (content.length() > 300) {
                    System.out.println(content.substring(0, 300) + "...");
                } else {
                    System.out.println(content);
                }
            } else {
                System.out.println("(内容为空)");
            }
        } else {
            System.out.println("\n错误信息: " + result.getErrorMessage());
        }

        System.out.println("\n");
    }
}
