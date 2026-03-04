package cn.nuaa.jensonxu.fairy.integration.service.rag.test;

import cn.nuaa.jensonxu.fairy.common.data.rag.ImageSection;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;
import cn.nuaa.jensonxu.fairy.common.parser.document.PdfStructuredParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.impl.PdfDocumentParser;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ChunkerConfig;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ImageTextMixChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.MediaContextAttacher;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.TokenCounter;
import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfMixedChunkStrategyManualTest {


    private static final String PDF_PATH = "/Users/jensonxu/Desktop/test2.pdf";  // 测试文件
    private static final String PROJECT_ROOT = "/Users/jensonxu/Fairy";  // 项目根目录（用于图片落盘）

    public static void main(String[] args) throws Exception {
        PdfDocumentParser parser = new PdfDocumentParser();
        PdfStructuredParseResult structured;

        try (InputStream in = Files.newInputStream(Paths.get(PDF_PATH))) {
            structured = parser.parseStructured(in, "test1.pdf");
        }

        if (structured == null || !structured.isSuccess()) {
            System.out.println("结构化解析失败: " + (structured == null ? "null" : structured.getErrorMessage()));
            return;
        }

        List<TextSection> textSections = structured.getTextSections();
        List<ImageSection> imageSections = structured.getImageSections();

        System.out.println("=== Structured Parse Summary ===");
        System.out.println("success=" + structured.isSuccess());
        System.out.println("pageCount=" + structured.getPageCount());
        System.out.println("textSections=" + (textSections == null ? 0 : textSections.size()));
        System.out.println("imageSections=" + (imageSections == null ? 0 : imageSections.size()));
        System.out.println("metadata=" + structured.getMetadata());

        // 构建分块输入 Document（透传结构化数据）
        Map<String, Object> metadata = new HashMap<>();
        if (structured.getMetadata() != null) {
            metadata.putAll(structured.getMetadata());
        }
        metadata.put("has_images", imageSections != null && !imageSections.isEmpty());
        metadata.put("text_sections", textSections == null ? List.of() : textSections);
        metadata.put("image_sections", imageSections == null ? List.of() : imageSections);

        String mergedText = (textSections == null ? List.<TextSection>of() : textSections).stream()
                .map(TextSection::getText)
                .filter(t -> t != null && !t.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        Document source = new Document(mergedText, metadata);

        // 分块器
        TokenCounter tokenCounter = new TokenCounter();
        ImageTextMixChunker chunker = new ImageTextMixChunker(new MediaContextAttacher(tokenCounter), tokenCounter);

        ChunkerConfig config = new ChunkerConfig();
        config.setChunkTokenSize(120);
        config.setOverlappedPercent(0.1);
        config.setImageContextSize(80);
        config.setChildrenDelimiters(List.of()); // 如需二次切分可改为 List.of("。")
        chunker.setConfig(config);

        List<Document> chunks = chunker.chunkSingle(source);

        System.out.println("\n=== Chunk Result ===");
        System.out.println("chunkCount=" + chunks.size());

        // 建 imageId -> ImageSection 映射，便于落盘图片
        Map<String, ImageSection> imageMap = new HashMap<>();
        if (imageSections != null) {
            for (ImageSection img : imageSections) {
                if (img != null && img.getImageId() != null) {
                    imageMap.put(img.getImageId(), img);
                }
            }
        }

        Path outputDir = Paths.get(PROJECT_ROOT);

        int imageFileIndex = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Document c = chunks.get(i);
            String type = String.valueOf(c.getMetadata().get("chunk_type"));
            String text = c.getText() == null ? "" : c.getText();
            String preview = text.length() <= 120 ? text : text.substring(0, 120) + "...";

            System.out.println("[" + i + "] type=" + type
                    + ", image_id=" + c.getMetadata().get("image_id")
                    + ", token_count=" + c.getMetadata().get("token_count")
                    + ", context_text=" + c.getMetadata().get("context_text"));
            System.out.println("    preview=" + preview);

            if ("image".equals(type)) {
                String imageId = String.valueOf(c.getMetadata().get("image_id"));
                ImageSection src = imageMap.get(imageId);
                if (src != null && src.getImageData() != null && !src.getImageData().isBlank()) {
                    byte[] bytes = Base64.getDecoder().decode(src.getImageData());
                    String ext = guessExt(src.getMimeType());
                    Path out = outputDir.resolve("chunk_image_" + (++imageFileIndex) + "_" + imageId + "." + ext);
                    Files.write(out, bytes);
                    System.out.println("    image_saved=" + out);
                } else {
                    System.out.println("    image_saved=skip (no imageData for image_id=" + imageId + ")");
                }
            }
        }
    }

    private static String guessExt(String mimeType) {
        if (mimeType == null) return "png";
        String m = mimeType.toLowerCase();
        if (m.contains("jpeg") || m.contains("jpg")) return "jpg";
        if (m.contains("png")) return "png";
        if (m.contains("gif")) return "gif";
        if (m.contains("bmp")) return "bmp";
        if (m.contains("webp")) return "webp";
        return "png";
    }
}
