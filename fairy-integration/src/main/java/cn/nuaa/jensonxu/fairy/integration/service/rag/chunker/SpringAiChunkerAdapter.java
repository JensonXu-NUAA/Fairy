package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

/**
 * Spring AI 分块器适配器
 */
@Component
public class SpringAiChunkerAdapter implements DocumentChunker {

    private ChunkerConfig config;
    private TextSplitter delegate;

    public SpringAiChunkerAdapter() {
        this.config = new ChunkerConfig();
        this.delegate = createDelegateSplitter();
    }

    private TextSplitter createDelegateSplitter() {
        return new TokenTextSplitter(
                config.getChunkTokenSize(),
                config.getMinChunkSize(),
                config.getMaxChunkSize(),
                (int) (config.getChunkTokenSize() * config.getOverlappedPercent()),
                false
        );
    }

    @Override
    public ChunkerConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(ChunkerConfig config) {
        this.config = config;
        this.delegate = createDelegateSplitter();
    }

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> base = delegate.split(documents);
        return splitByChildrenDelimiters(base);
    }

    @Override
    public List<Document> chunkSingle(Document document) {
        List<Document> base = delegate.split(document);
        return splitByChildrenDelimiters(base);
    }

    private List<Document> splitByChildrenDelimiters(List<Document> docs) {
        if (docs == null || docs.isEmpty() || config.getChildrenDelimiters() == null || config.getChildrenDelimiters().isEmpty()) {
            return docs;
        }

        String regex = config.getChildrenDelimiters().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(java.util.regex.Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        if (regex.isBlank()) {
            return docs;
        }

        List<Document> result = new java.util.ArrayList<>();
        for (Document doc : docs) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                result.add(doc);
                continue;
            }

            String[] parts = text.split(regex);
            if (parts.length <= 1) {
                result.add(doc);
                continue;
            }

            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                result.add(new Document(part.trim(), new java.util.HashMap<>(doc.getMetadata())));
            }
        }
        return result;
    }


    @Override
    public boolean supports(Document document) {
        Object hasImages = document.getMetadata().get("has_images");
        return !Boolean.TRUE.equals(hasImages);
    }
}

