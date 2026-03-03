package cn.nuaa.jensonxu.fairy.integration.service.rag;

import cn.nuaa.jensonxu.fairy.common.data.rag.ChunkResult;
import cn.nuaa.jensonxu.fairy.common.data.rag.EnhancedDocumentChunk;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParserFactory;

import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ChunkerConfig;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunkerFactory;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.TokenCounter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分块服务
 */
@Service
public class DocumentChunkService {

    private final DocumentChunkerFactory chunkerFactory;
    private final TokenCounter tokenCounter;

    public DocumentChunkService(DocumentChunkerFactory chunkerFactory,
                                TokenCounter tokenCounter) {
        this.chunkerFactory = chunkerFactory;
        this.tokenCounter = tokenCounter;
    }

    public ChunkResult chunkFile(MultipartFile file, ChunkerConfig config) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        final String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名为空，无法识别文档类型");
        }

        DocumentParseResult parseResult;
        try (InputStream inputStream = file.getInputStream()) {
            parseResult = DocumentParserFactory.parse(inputStream, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败: " + e.getMessage(), e);
        }

        if (parseResult == null || !parseResult.isSuccess()) {
            String message = parseResult == null ? "文档解析结果为空" : parseResult.getErrorMessage();
            throw new IllegalStateException("文档解析失败: " + message);
        }

        Map<String, Object> metadata = new HashMap<>();
        if (parseResult.getMetadata() != null) {
            metadata.putAll(parseResult.getMetadata());
        }
        metadata.put("file_name", fileName);
        metadata.put("content_type", parseResult.getContentType());
        metadata.put("page_count", parseResult.getPageCount());
        metadata.put("char_count", parseResult.getCharCount());
        metadata.put("parse_duration", parseResult.getParseDuration());
        metadata.putIfAbsent("has_images", false);

        Document sourceDoc = new Document(parseResult.getContent(), metadata);
        return chunkDocuments(List.of(sourceDoc), config);
    }

    public ChunkResult chunkDocuments(List<Document> documents, ChunkerConfig config) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("待分块文档列表为空");
        }

        DocumentChunker chunker = chunkerFactory.getChunker(documents.get(0));
        chunker.setConfig(config);
        long start = System.currentTimeMillis();
        List<Document> chunkDocs = chunker.chunk(documents);
        long duration = System.currentTimeMillis() - start;

        List<EnhancedDocumentChunk> enhancedChunks = convertToEnhancedChunks(chunkDocs);
        int totalTokens = enhancedChunks.stream().mapToInt(EnhancedDocumentChunk::getTokenCount).sum();

        Map<String, Object> sourceMetadata = documents.get(0).getMetadata() == null
                ? Map.of()
                : documents.get(0).getMetadata();

        return ChunkResult.builder()
                .chunks(enhancedChunks)
                .sourceMetadata(sourceMetadata)
                .duration(duration)
                .chunkCount(enhancedChunks.size())
                .totalTokens(totalTokens)
                .avgTokens(enhancedChunks.isEmpty() ? 0 : (double) totalTokens / enhancedChunks.size())
                .hasImages(Boolean.TRUE.equals(sourceMetadata.get("has_images")))
                .hasTables(false)
                .build();
    }

    private List<EnhancedDocumentChunk> convertToEnhancedChunks(List<Document> documents) {
        return documents.stream().map(doc -> {
            Map<String, Object> metadata = doc.getMetadata() == null ? new HashMap<>() : new HashMap<>(doc.getMetadata());
            String text = doc.getText();
            return EnhancedDocumentChunk.builder()
                    .text(text)
                    .metadata(metadata)
                    .chunkType(Boolean.TRUE.equals(metadata.get("has_images")) ? "image" : "text")
                    .tokenCount(tokenCounter.countTokens(text))
                    .build();
        }).toList();
    }
}
