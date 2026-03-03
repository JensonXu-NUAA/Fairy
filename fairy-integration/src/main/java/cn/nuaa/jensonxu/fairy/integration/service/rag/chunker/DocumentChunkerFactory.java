package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * 分块器工厂
 * 根据文档特征选择纯文本分块器或图文混合分块器
 */
@Component
@RequiredArgsConstructor
public class DocumentChunkerFactory {

    private final SpringAiChunkerAdapter springAiChunker;
    private final ImageTextMixChunker imageTextMixChunker;

    public DocumentChunker getChunker(Document document) {
        if (isImageTextDocument(document)) {
            return imageTextMixChunker;
        }
        return springAiChunker;
    }

    public DocumentChunker getChunker(String documentType) {
        if (isImageTextType(documentType)) {
            return imageTextMixChunker;
        }
        return springAiChunker;
    }

    public DocumentChunker getDefaultChunker() {
        return springAiChunker;
    }

    private boolean isImageTextDocument(Document document) {
        Object hasImages = document.getMetadata().get("has_images");
        return Boolean.TRUE.equals(hasImages);
    }

    private boolean isImageTextType(String documentType) {
        return "pdf_with_images".equals(documentType) || "docx_with_images".equals(documentType);
    }
}