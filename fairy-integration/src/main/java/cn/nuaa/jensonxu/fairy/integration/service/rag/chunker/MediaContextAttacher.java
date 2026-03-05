package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import cn.nuaa.jensonxu.fairy.common.data.rag.PositionInfo;
import lombok.Data;

import cn.nuaa.jensonxu.fairy.common.data.rag.EnhancedDocumentChunk;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 媒体上下文附加器
 */
@Data
@Component
public class MediaContextAttacher {

    private final TokenCounter tokenCounter;

    public MediaContextAttacher(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    /**
     * 当前占位实现
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
        List<EnhancedDocumentChunk> textChunks = chunks.stream()
                .filter(c -> "text".equals(c.getChunkType()))
                .toList();

        if (textChunks.isEmpty()) {
            return chunks;
        }

        // 对每个媒体块附加上下文
        for (EnhancedDocumentChunk chunk : chunks) {
            if (!"image".equals(chunk.getChunkType()) && !"table".equals(chunk.getChunkType())) {
                continue;
            }

            List<EnhancedDocumentChunk> ordered = new java.util.ArrayList<>(textChunks);
            ordered.sort(Comparator.comparingDouble(t -> distanceBetween(chunk, t)));

            // 如果在原列表中没找到（兜底）
            if (ordered.isEmpty()) {
                ordered.addAll(textChunks);
            }

            String context = collectContextByTokenBudget(ordered, contextBudget);
            chunk.setContextText(context);
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

    private double distanceBetween(EnhancedDocumentChunk mediaChunk, EnhancedDocumentChunk textChunk) {
        PositionInfo mediaPos = toPosition(mediaChunk);
        PositionInfo textPos = toPosition(textChunk);

        if (mediaPos == null || textPos == null) {
            return Double.MAX_VALUE;
        }
        return mediaPos.distanceTo(textPos);
    }

    private PositionInfo toPosition(EnhancedDocumentChunk chunk) {
        if (chunk == null || chunk.getPositions() == null || chunk.getPositions().isEmpty()) {
            return null;
        }

        List<Integer> p = chunk.getPositions().get(0);
        if (p == null || p.size() < 3) {
            return null;
        }

        int pageNum = p.get(0);
        double top = p.get(1);
        double bottom = p.get(2);

        if (p.size() >= 5) {
            double left = p.get(3);
            double right = p.get(4);
            return new PositionInfo(pageNum, top, bottom, left, right);
        }

        // 兼容旧的 3 元组格式
        return new PositionInfo(pageNum, top, bottom, 0.0, 0.0);
    }
}