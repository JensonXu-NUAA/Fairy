package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import lombok.Data;

import cn.nuaa.jensonxu.fairy.common.data.rag.EnhancedDocumentChunk;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 媒体上下文附加器（骨架）
 * 后续实现：按位置距离为图片/表格块附加附近文本上下文
 */
@Data
@Component
public class MediaContextAttacher {

    private final TokenCounter tokenCounter;

    public MediaContextAttacher(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    /**
     * 当前占位实现：直接返回原 chunks
     */
    public List<EnhancedDocumentChunk> attach(List<EnhancedDocumentChunk> chunks, ChunkerConfig config) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }

        int contextBudget = Math.max(config.getImageContextSize(), config.getTableContextSize());
        if (contextBudget <= 0) {
            return chunks;
        }

        // 预收集文本块
        java.util.List<EnhancedDocumentChunk> textChunks = chunks.stream()
                .filter(c -> "text".equals(c.getChunkType()))
                .toList();

        if (textChunks.isEmpty()) {
            return chunks;
        }

        // 对每个媒体块附加上下文
        for (int i = 0; i < chunks.size(); i++) {
            EnhancedDocumentChunk c = chunks.get(i);
            if (!"image".equals(c.getChunkType()) && !"table".equals(c.getChunkType())) {
                continue;
            }

            // 简化“最近”规则：先取媒体块前面的最近文本，再补后面的文本
            java.util.List<EnhancedDocumentChunk> ordered = new java.util.ArrayList<>();

            // 前向最近（倒序）
            for (int j = i - 1; j >= 0; j--) {
                EnhancedDocumentChunk candidate = chunks.get(j);
                if ("text".equals(candidate.getChunkType())) {
                    ordered.add(candidate);
                }
            }
            // 后向最近（正序）
            for (int j = i + 1; j < chunks.size(); j++) {
                EnhancedDocumentChunk candidate = chunks.get(j);
                if ("text".equals(candidate.getChunkType())) {
                    ordered.add(candidate);
                }
            }

            // 如果在原列表中没找到（兜底）
            if (ordered.isEmpty()) {
                ordered.addAll(textChunks);
            }

            String context = collectContextByTokenBudget(ordered, contextBudget);
            c.setContextText(context);
        }

        return chunks;
    }


    private String collectContextByTokenBudget(List<EnhancedDocumentChunk> orderedTextChunks, int tokenBudget) {
        if (orderedTextChunks == null || orderedTextChunks.isEmpty() || tokenBudget <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int used = 0;

        for (EnhancedDocumentChunk chunk : orderedTextChunks) {
            if (chunk == null || chunk.getText() == null || chunk.getText().isBlank()) {
                continue;
            }

            String text = chunk.getText().trim();
            int t = tokenCounter.countTokens(text);

            if (used + t <= tokenBudget) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text);
                used += t;
                continue;
            }

            // 超预算时做句级裁剪
            String partial = trimByTokenBudget(text, tokenBudget - used);
            if (!partial.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(partial);
            }
            break;
        }

        return sb.toString().trim();
    }

    private String trimByTokenBudget(String text, int remainBudget) {
        if (text == null || text.isBlank() || remainBudget <= 0) {
            return "";
        }

        String[] sentences = text.split("(?<=[。！？.!?])");
        java.util.LinkedList<String> selected = new java.util.LinkedList<>();
        int used = 0;

        for (int i = sentences.length - 1; i >= 0; i--) {
            String s = sentences[i];
            if (s == null || s.isBlank()) {
                continue;
            }
            String sentence = s.trim();
            int t = tokenCounter.countTokens(sentence);
            if (used + t > remainBudget) {
                continue;
            }
            selected.addFirst(sentence); // 保持原顺序输出
            used += t;
        }

        return String.join("", selected).trim();
    }

}