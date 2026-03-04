package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import cn.nuaa.jensonxu.fairy.common.data.rag.EnhancedDocumentChunk;
import cn.nuaa.jensonxu.fairy.common.data.rag.ImageSection;
import cn.nuaa.jensonxu.fairy.common.data.rag.PositionInfo;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;

import lombok.Data;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图文混合分块器（正式骨架）
 * 后续补充：图文合并算法、上下文附加细节、二次切分逻辑。
 */
@Data
@Component
public class ImageTextMixChunker implements DocumentChunker {

    private ChunkerConfig config = new ChunkerConfig();
    private final MediaContextAttacher contextAttacher;
    private final TokenCounter tokenCounter;

    public ImageTextMixChunker(MediaContextAttacher contextAttacher,
                               TokenCounter tokenCounter) {
        this.contextAttacher = contextAttacher;
        this.tokenCounter = tokenCounter;
    }

    @Override
    public ChunkerConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(ChunkerConfig config) {
        this.config = config;
    }

    @Override
    public List<Document> chunk(List<Document> documents) {
        return documents.stream()
                .flatMap(doc -> chunkSingle(doc).stream())
                .toList();
    }

    @Override
    public List<Document> chunkSingle(Document document) {
        List<TextSection> sections = extractSections(document);
        List<ImageSection> images = extractImages(document);
        List<EnhancedDocumentChunk> chunks = naiveMergeWithImages(sections, images, config);

        if (config.getTableContextSize() > 0 || config.getImageContextSize() > 0) {
            chunks = contextAttacher.attach(chunks, config);
        }

        if (config.getChildrenDelimiters() != null && !config.getChildrenDelimiters().isEmpty()) {
            chunks = splitByChildrenDelimiters(chunks, config);
        }

        // 骨架阶段兜底：避免返回空结果
        if (chunks == null || chunks.isEmpty()) {
            EnhancedDocumentChunk fallback = EnhancedDocumentChunk.builder()
                    .text(document.getText())
                    .metadata(document.getMetadata())
                    .chunkType(Boolean.TRUE.equals(document.getMetadata().get("has_images")) ? "image" : "text")
                    .tokenCount(tokenCounter.countTokens(document.getText()))
                    .build();
            chunks = List.of(fallback);
        }

        return convertToDocuments(chunks);
    }


    @Override
    public boolean supports(Document document) {
        Object hasImages = document.getMetadata().get("has_images");
        return Boolean.TRUE.equals(hasImages);
    }

    /**
     * 从文档提取文本段落
     */
    private List<TextSection> extractSections(Document document) {
        if (document == null) {
            return List.of();
        }

        Object sectionsObj = document.getMetadata().get("text_sections");
        if (sectionsObj instanceof List<?> rawList && !rawList.isEmpty()) {
            List<TextSection> sections = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof TextSection textSection) {
                    // tokenCount 为空时兜底计算
                    if (textSection.getTokenCount() <= 0 && textSection.getText() != null) {
                        textSection.setTokenCount(tokenCounter.countTokens(textSection.getText()));
                    }
                    sections.add(textSection);
                }
            }
            if (!sections.isEmpty()) {
                return sections;
            }
        }

        // 兼容旧逻辑：按句分割 document.getText()
        if (document.getText() == null || document.getText().isBlank()) {
            return List.of();
        }

        String text = document.getText().trim();
        String[] parts = text.split("(?<=[。！？.!?])");

        List<TextSection> sections = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String sentence = part.trim();
            sections.add(TextSection.builder()
                    .text(sentence)
                    .position(null)
                    .title(false)
                    .titleLevel(0)
                    .table(false)
                    .tokenCount(tokenCounter.countTokens(sentence))
                    .build());
        }

        if (sections.isEmpty()) {
            sections.add(TextSection.builder()
                    .text(text)
                    .position(null)
                    .title(false)
                    .titleLevel(0)
                    .table(false)
                    .tokenCount(tokenCounter.countTokens(text))
                    .build());
        }

        return sections;
    }

    /**
     * 从文档提取图片段落
     */
    private List<ImageSection> extractImages(Document document) {
        if (document == null) {
            return List.of();
        }

        Object sectionsObj = document.getMetadata().get("image_sections");
        if (sectionsObj instanceof List<?> rawList && !rawList.isEmpty()) {
            List<ImageSection> images = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof ImageSection imageSection) {
                    images.add(imageSection);
                }
            }
            if (!images.isEmpty()) {
                return images;
            }
        }

        // 兼容旧逻辑：单图占位
        Object hasImages = document.getMetadata().get("has_images");
        if (!Boolean.TRUE.equals(hasImages)) {
            return List.of();
        }

        Object imageIdObj = document.getMetadata().get("image_id");
        Object imageDataObj = document.getMetadata().get("image_data");

        ImageSection imageSection = ImageSection.builder()
                .imageId(imageIdObj == null ? "unknown-image-id" : String.valueOf(imageIdObj))
                .imageData(imageDataObj == null ? null : String.valueOf(imageDataObj))
                .position(null)
                .width(0)
                .height(0)
                .mimeType("image/unknown")
                .build();

        return List.of(imageSection);
    }

    /**
     * 图文混合分块核心算法
     */
    private List<EnhancedDocumentChunk> naiveMergeWithImages(List<TextSection> sections,
                                                             List<ImageSection> images,
                                                             ChunkerConfig config) {
        List<EnhancedDocumentChunk> result = new java.util.ArrayList<>();
        int target = Math.max(1, config.getChunkTokenSize());
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        cn.nuaa.jensonxu.fairy.common.data.rag.PositionInfo lastSectionPos = null;

        if (sections != null) {
            for (TextSection section : sections) {
                if (section == null || section.getText() == null || section.getText().isBlank()) {
                    continue;
                }

                String text = section.getText().trim();
                int tokens = section.getTokenCount() > 0 ? section.getTokenCount() : tokenCounter.countTokens(text);

                if (currentTokens > 0 && currentTokens + tokens > target) {
                    int overlapTokens = tokenCounter.calculateOverlapTokens(
                            config.getChunkTokenSize(),
                            config.getOverlappedPercent()
                    );

                    String chunkText = current.toString().trim();
                    if (!chunkText.isBlank()) {
                        result.add(EnhancedDocumentChunk.builder()
                                .text(chunkText)
                                .chunkType("text")
                                .positions(wrapPosition(lastSectionPos))
                                .tokenCount(currentTokens)
                                .build());
                    }

                    String overlapSeed = "";
                    if (overlapTokens > 0 && !current.isEmpty()) {
                        String curr = current.toString().trim();
                        String[] sentenceParts = curr.split("(?<=[。！？.!?])");
                        if (sentenceParts.length > 0) {
                            String lastSentence = sentenceParts[sentenceParts.length - 1].trim();
                            int lastSentenceTokens = tokenCounter.countTokens(lastSentence);

                            if (!lastSentence.isBlank() && lastSentenceTokens <= overlapTokens) {
                                overlapSeed = lastSentence;
                            }
                        }
                    }

                    current.setLength(0);
                    currentTokens = 0;

                    if (!overlapSeed.isBlank()) {
                        current.append(overlapSeed);
                        currentTokens = tokenCounter.countTokens(overlapSeed);
                    }
                }

                if (!current.isEmpty()) {
                    current.append('\n');
                }
                current.append(text);
                currentTokens += tokens;
                lastSectionPos = section.getPosition();
            }
        }

        if (currentTokens > 0) {
            String chunkText = current.toString().trim();
            if (!chunkText.isBlank()) {
                result.add(EnhancedDocumentChunk.builder()
                        .text(chunkText)
                        .chunkType("text")
                        .positions(wrapPosition(lastSectionPos))
                        .tokenCount(currentTokens)
                        .build());
            }
        }

        if (images != null) {
            for (ImageSection image : images) {
                if (image == null) {
                    continue;
                }
                result.add(EnhancedDocumentChunk.builder()
                        .text("[image]")
                        .chunkType("image")
                        .positions(wrapPosition(image.getPosition()))
                        .imageId(image.getImageId())
                        .imageData(image.getImageData())
                        .tokenCount(0)
                        .build());
            }
        }

        return result;
    }

    /**
     * 子分隔符二次切分
     */
    private List<EnhancedDocumentChunk> splitByChildrenDelimiters(List<EnhancedDocumentChunk> chunks,
                                                                  ChunkerConfig config) {
        if (chunks == null || chunks.isEmpty() || config.getChildrenDelimiters() == null || config.getChildrenDelimiters().isEmpty()) {
            return chunks;
        }

        String regex = config.getChildrenDelimiters().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(java.util.regex.Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        if (regex.isBlank()) {
            return chunks;
        }

        List<EnhancedDocumentChunk> result = new java.util.ArrayList<>();

        for (EnhancedDocumentChunk chunk : chunks) {
            if (!"text".equals(chunk.getChunkType()) || chunk.getText() == null || chunk.getText().isBlank()) {
                result.add(chunk);
                continue;
            }

            String[] parts = chunk.getText().split(regex);
            if (parts.length <= 1) {
                result.add(chunk);
                continue;
            }

            String parentId = chunk.getParentId() == null ? java.util.UUID.randomUUID().toString() : chunk.getParentId();

            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                result.add(EnhancedDocumentChunk.builder()
                        .text(part.trim())
                        .metadata(chunk.getMetadata())
                        .positions(chunk.getPositions())
                        .imageId(chunk.getImageId())
                        .imageData(chunk.getImageData())
                        .chunkType(chunk.getChunkType())
                        .parentId(parentId)
                        .contextText(chunk.getContextText())
                        .tokenCount(tokenCounter.countTokens(part.trim()))
                        .startOffset(chunk.getStartOffset())
                        .endOffset(chunk.getEndOffset())
                        .build());
            }
        }
        return result;
    }


    /**
     * 转换为 Spring AI Document
     */
    private List<Document> convertToDocuments(List<EnhancedDocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream().map(chunk -> {
            String text = chunk.getText() == null ? "" : chunk.getText();

            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            if (chunk.getMetadata() != null) {
                metadata.putAll(chunk.getMetadata());
            }

            if (chunk.getChunkType() != null) {
                metadata.put("chunk_type", chunk.getChunkType());
            }
            if (chunk.getImageId() != null) {
                metadata.put("image_id", chunk.getImageId());
            }
            if (chunk.getContextText() != null && !chunk.getContextText().isBlank()) {
                metadata.put("context_text", chunk.getContextText());
            }
            if (chunk.getTokenCount() > 0) {
                metadata.put("token_count", chunk.getTokenCount());
            }
            if (chunk.getStartOffset() > 0 || chunk.getEndOffset() > 0) {
                metadata.put("start_offset", chunk.getStartOffset());
                metadata.put("end_offset", chunk.getEndOffset());
            }
            if (chunk.getParentId() != null) {
                metadata.put("parent_id", chunk.getParentId());
            }
            if (chunk.getPositions() != null && !chunk.getPositions().isEmpty()) {
                metadata.put("positions", chunk.getPositions());
            }

            return new Document(text, metadata);
        }).toList();
    }

    private List<Integer> toSimplePosition(PositionInfo pos) {
        if (pos == null) {
            return null;
        }
        return List.of(
                pos.getPageNum(),
                (int) pos.getTop(),
                (int) pos.getBottom(),
                (int) pos.getLeft(),
                (int) pos.getRight()
        );
    }

    private List<List<Integer>> wrapPosition(PositionInfo pos) {
        List<Integer> p = toSimplePosition(pos);
        return p == null ? List.of() : List.of(p);
    }
}
