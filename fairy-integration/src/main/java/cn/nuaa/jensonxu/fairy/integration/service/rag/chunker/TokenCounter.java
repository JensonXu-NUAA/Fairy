package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Token计数器（轻量估算）
 */
@Component
public class TokenCounter {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("\\p{Script=Han}");
    private static final Pattern ENGLISH_WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * 估算文本 token 数
     * 中文字符约2 token，英文单词约1 token
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseCount = (int) CHINESE_PATTERN.matcher(text).results().count();
        int englishCount = (int) ENGLISH_WORD_PATTERN.matcher(text).results().count();
        return chineseCount * 2 + englishCount;
    }

    /**
     * 根据块大小和重叠比例计算重叠 token 数
     */
    public int calculateOverlapTokens(int chunkTokenSize, double overlappedPercent) {
        return (int) (chunkTokenSize * overlappedPercent);
    }
}
