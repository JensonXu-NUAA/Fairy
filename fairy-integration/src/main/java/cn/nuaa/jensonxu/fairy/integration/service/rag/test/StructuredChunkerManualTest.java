package cn.nuaa.jensonxu.fairy.integration.service.rag.test;

import cn.nuaa.jensonxu.fairy.common.data.rag.ImageSection;
import cn.nuaa.jensonxu.fairy.common.data.rag.PositionInfo;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ChunkerConfig;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ImageTextMixChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.MediaContextAttacher;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.TokenCounter;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructuredChunkerManualTest {

    public static void main(String[] args) {
        TokenCounter tokenCounter = new TokenCounter();
        ImageTextMixChunker chunker = new ImageTextMixChunker(
                new MediaContextAttacher(tokenCounter),
                tokenCounter
        );

        ChunkerConfig config = new ChunkerConfig();
        config.setChunkTokenSize(10);
        config.setImageContextSize(60);
        chunker.setConfig(config);

        TextSection t1 = TextSection.builder()
                .text("这是结构化文本段A。")
                .position(new PositionInfo(0, 100, 120, 0, 500))   // 远
                .tokenCount(tokenCounter.countTokens("这是结构化文本段A。"))
                .build();

        TextSection t2 = TextSection.builder()
                .text("这是紧邻图片的说明段B。")
                .position(new PositionInfo(0, 480, 500, 0, 500))   // 近
                .tokenCount(tokenCounter.countTokens("这是紧邻图片的说明段B。"))
                .build();

        ImageSection img1 = ImageSection.builder()
                .imageId("img-structured-001")
                .imageData("base64-placeholder")
                .position(new PositionInfo(0, 510, 700, 0, 500))   // 紧邻 t2
                .width(400)
                .height(300)
                .mimeType("image/png")
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("has_images", true);
        metadata.put("text_sections", List.of(t1, t2));
        metadata.put("image_sections", List.of(img1));

        Document doc = new Document("fallback text should not be used", metadata);

        List<Document> chunks = chunker.chunkSingle(doc);

        System.out.println("chunkCount=" + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document c = chunks.get(i);
            System.out.println("[" + i + "] type=" + c.getMetadata().get("chunk_type")
                    + ", text=" + c.getText()
                    + ", context_text=" + c.getMetadata().get("context_text")
                    + ", image_id=" + c.getMetadata().get("image_id")
                    + ", positions=" + c.getMetadata().get("positions"));
        }

        String mergedText = chunks.stream()
                .filter(c -> "text".equals(String.valueOf(c.getMetadata().get("chunk_type"))))
                .map(Document::getText)
                .findFirst()
                .orElse("");

        if (mergedText.contains("fallback text should not be used")) {
            throw new IllegalStateException("结构化 text_sections 未生效，仍在使用 fallback 文本");
        }
        System.out.println("Structured sections assertion passed.");
    }
}

