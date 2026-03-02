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
        return delegate.split(documents);
    }

    @Override
    public List<Document> chunkSingle(Document document) {
        return delegate.split(document);
    }

    @Override
    public boolean supports(Document document) {
        Object hasImages = document.getMetadata().get("has_images");
        return !Boolean.TRUE.equals(hasImages);
    }
}

