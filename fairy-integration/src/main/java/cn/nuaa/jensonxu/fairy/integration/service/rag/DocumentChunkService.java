package cn.nuaa.jensonxu.fairy.integration.service.rag;

import cn.nuaa.jensonxu.fairy.common.data.rag.ChunkResult;
import cn.nuaa.jensonxu.fairy.common.data.rag.EnhancedDocumentChunk;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParserFactory;
import cn.nuaa.jensonxu.fairy.common.parser.document.PdfStructuredParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.impl.PdfDocumentParser;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.ChunkerConfig;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunker;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.DocumentChunkerFactory;
import cn.nuaa.jensonxu.fairy.integration.service.rag.chunker.TokenCounter;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DocumentChunkService {

    private final DocumentChunkerFactory chunkerFactory;
    private final TokenCounter tokenCounter;

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

    public ChunkResult chunkPdfStructured(MultipartFile file, ChunkerConfig config) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        final String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名为空，无法识别文档类型");
        }

        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("当前结构化分块仅支持 PDF 文件");
        }

        PdfDocumentParser parser = new PdfDocumentParser();
        PdfStructuredParseResult structured;
        try (InputStream inputStream = file.getInputStream()) {
            structured = parser.parseStructured(inputStream, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败: " + e.getMessage(), e);
        }

        if (structured == null || !structured.isSuccess()) {
            String msg = structured == null ? "结构化解析结果为空" : structured.getErrorMessage();
            throw new IllegalStateException("PDF 结构化解析失败: " + msg);
        }

        Map<String, Object> metadata = new HashMap<>();
        if (structured.getMetadata() != null) {
            metadata.putAll(structured.getMetadata());
        }
        metadata.put("file_name", fileName);
        metadata.put("content_type", "application/pdf");
        metadata.put("page_count", structured.getPageCount());
        metadata.put("char_count", structured.getCharCount());
        metadata.put("has_images", structured.getImageSections() != null && !structured.getImageSections().isEmpty());
        // metadata.putIfAbsent("has_images", structured.getImageSections() != null && !structured.getImageSections().isEmpty());

        if (structured.getImageSections() != null && !structured.getImageSections().isEmpty()) {
            metadata.put("image_sections", structured.getImageSections());
        }
        if (structured.getTextSections() != null && !structured.getTextSections().isEmpty()) {
            metadata.put("text_sections", structured.getTextSections());
        }

        // 当前先沿用现有 chunk 流程：将文本段拼接为一个 Document，图片通过 metadata 标记路由
        String mergedText = structured.getTextSections() == null ? "" :
                structured.getTextSections().stream()
                        .map(TextSection::getText)
                        .filter(t -> t != null && !t.isBlank())
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");

        Document sourceDoc = new Document(mergedText, metadata);
        return chunkDocuments(List.of(sourceDoc), config);
    }
}
