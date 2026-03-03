package cn.nuaa.jensonxu.fairy.integration.service.rag.test;

import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ChunkerConfig;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunkerFactory;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ImageTextMixChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.MediaContextAttacher;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.SpringAiChunkerAdapter;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.TokenCounter;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手动运行入口：验证分块器路由和纯文本分块能力
 */
public class ChunkerManualTest {

    public static void main(String[] args) {
        SpringAiChunkerAdapter springAiChunker = new SpringAiChunkerAdapter();
        TokenCounter tokenCounter = new TokenCounter();
        MediaContextAttacher mediaContextAttacher = new MediaContextAttacher(tokenCounter);
        ImageTextMixChunker imageTextMixChunker = new ImageTextMixChunker(mediaContextAttacher, tokenCounter);
        DocumentChunkerFactory factory = new DocumentChunkerFactory(springAiChunker, imageTextMixChunker);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("has_images", false);

        String text = """
                Fairy 是一个用于 RAG 能力集成的项目。
                当前测试目标是验证 DocumentChunkerFactory 的路由逻辑和 SpringAiChunkerAdapter 的分块能力。
                这是一段较长的文本，用于触发文本切分行为，观察输出 chunk 数量与内容摘要。
                """;
        Document document = new Document(text, metadata);
        DocumentChunker chunker = factory.getChunker(document);

        ChunkerConfig config = new ChunkerConfig();
        config.setChunkTokenSize(60);
        config.setMinChunkSize(20);
        config.setMaxChunkSize(50);
        config.setOverlappedPercent(0.1);
        config.setChildrenDelimiters(List.of("。"));
        config.setImageContextSize(30);
        chunker.setConfig(config);

        List<Document> chunks = chunker.chunkSingle(document);

        System.out.println("=== Chunker Manual Test ===");
        System.out.println("Selected chunker: " + chunker.getClass().getSimpleName());
        System.out.println("Chunk count: " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            Document c = chunks.get(i);
            String chunkText = c.getText() == null ? "" : c.getText();
            String preview = chunkText.length() <= 80 ? chunkText : chunkText.substring(0, 80) + "...";
            System.out.println("[" + i + "] len=" + chunkText.length() + ", preview=" + preview);
        }

        if (!(chunker instanceof SpringAiChunkerAdapter)) {
            throw new IllegalStateException("纯文本路由错误，未命中 SpringAiChunkerAdapter");
        }
        if (chunks.size() < 2) {
            throw new IllegalStateException("childrenDelimiters 未生效，期望 pure text 至少切分为2块");
        }
        System.out.println("Pure text split assertion passed.");


        // ===== 验证图文混合路由 =====
        Map<String, Object> imageMetadata = new HashMap<>();
        imageMetadata.put("has_images", true);

        Document imageDoc = new Document("这是一段图文混合文档的占位文本。", imageMetadata);
        DocumentChunker imageChunker = factory.getChunker(imageDoc);

        System.out.println("\n=== Image Route Test ===");
        System.out.println("Selected chunker: " + imageChunker.getClass().getSimpleName());

        // 当前 ImageTextMixChunker 还是骨架实现，先只验证路由，不校验分块结果
        if (!(imageChunker instanceof ImageTextMixChunker)) {
            throw new IllegalStateException("路由错误：has_images=true 时未命中 ImageTextMixChunker");
        }
        System.out.println("Route assertion passed.");

        // ===== 验证图文分块输出（骨架实现）=====
        Map<String, Object> imageChunkMetadata = new HashMap<>();
        imageChunkMetadata.put("has_images", true);
        imageChunkMetadata.put("image_id", "img-001");
        imageChunkMetadata.put("image_data", "base64-placeholder");

        Document imageChunkDoc = new Document("这是图文文档中的说明文字。", imageChunkMetadata);
        imageChunker.setConfig(config);

        List<Document> imageChunks = imageChunker.chunkSingle(imageChunkDoc);
        System.out.println("\n=== Image Chunk Test ===");
        System.out.println("Image chunk count: " + imageChunks.size());

        for (int i = 0; i < imageChunks.size(); i++) {
            Document c = imageChunks.get(i);
            String chunkText = c.getText() == null ? "" : c.getText();
            Object contextText = c.getMetadata().get("context_text");
            System.out.println("[" + i + "] text=" + chunkText
                    + ", context_text=" + contextText
                    + ", metadata=" + c.getMetadata());
        }


        if (imageChunks.isEmpty()) {
            throw new IllegalStateException("图文分块结果为空");
        }

        boolean hasTextChunk = imageChunks.stream()
                .anyMatch(d -> "text".equals(String.valueOf(d.getMetadata().get("chunk_type"))));

        boolean hasImageChunk = imageChunks.stream()
                .anyMatch(d -> "image".equals(String.valueOf(d.getMetadata().get("chunk_type"))));

        if (!hasTextChunk || !hasImageChunk) {
            throw new IllegalStateException("图文分块类型不完整，期望同时包含 text 与 image");
        }

        // ===== 最近优先上下文测试 =====
        System.out.println("\n=== Nearby Context Test ===");
        ChunkerConfig nearbyConfig = new ChunkerConfig();
        nearbyConfig.setChunkTokenSize(100);
        nearbyConfig.setOverlappedPercent(0.0);
        nearbyConfig.setImageContextSize(60);
        nearbyConfig.setChildrenDelimiters(List.of()); // 避免二次切分干扰

        ImageTextMixChunker nearbyChunker = new ImageTextMixChunker(
                new MediaContextAttacher(new TokenCounter()),
                new TokenCounter()
        );
        nearbyChunker.setConfig(nearbyConfig);

        Map<String, Object> nearbyMeta = new HashMap<>();
        nearbyMeta.put("has_images", true);
        nearbyMeta.put("image_id", "img-nearby-001");
        nearbyMeta.put("image_data", "base64-placeholder");

        // 当前骨架会把整段文本作为 text chunk，再加一个 image chunk
        Document nearbyDoc = new Document("""
        这是前置段落A，用于介绍背景信息。
        这是前置段落B，描述系统结构说明。
        这是紧邻图片的说明段落C，强调关键参数和结论。
        """, nearbyMeta);

        List<Document> nearbyChunks = nearbyChunker.chunkSingle(nearbyDoc);
        for (int i = 0; i < nearbyChunks.size(); i++) {
            Document c = nearbyChunks.get(i);
            System.out.println("[" + i + "] type=" + c.getMetadata().get("chunk_type")
                    + ", text=" + c.getText()
                    + ", context_text=" + c.getMetadata().get("context_text"));
        }

        String nearbyImageContext = nearbyChunks.stream()
                .filter(d -> "image".equals(String.valueOf(d.getMetadata().get("chunk_type"))))
                .map(d -> String.valueOf(d.getMetadata().get("context_text")))
                .findFirst()
                .orElse("");

        if (!nearbyImageContext.contains("紧邻图片的说明段落C")) {
            throw new IllegalStateException("最近上下文选择失败，未命中段落C");
        }
        System.out.println("Nearby context assertion passed.");

        System.out.println("Image chunk type assertion passed.");
    }
}

