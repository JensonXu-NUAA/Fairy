package cn.nuaa.jensonxu.fairy.integration.mcp.utils;

import com.rometools.rome.feed.synd.SyndEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ArxivUtils {

    public static final String ARXIV_QUERY_API = "https://export.arxiv.org/api/query";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 格式化作者信息
     */
    public static String formatAuthors(SyndEntry entry) {
        if (entry.getAuthors() == null || entry.getAuthors().isEmpty()) {
            return "";
        }

        List<String> authorNames = new ArrayList<>();
        entry.getAuthors().forEach(author -> authorNames.add(author.getName()));
        return String.join(", ", authorNames);
    }

    /**
     * 清理和格式化摘要文本
     */
    public static String formatAbstract(String description) {
        if (description == null) {
            return "";
        }

        return description.replaceAll("<[^>]*>", "") // 移除 HTML 标签
                .replaceAll("\\s+", " ") // 合并多个空格
                .trim();
    }

    /**
     * 构建基本论文信息
     */
    public static void buildPaperInfo(StringBuilder result, SyndEntry entry, int index) {
        result.append(String.format("[%d] Title: %s\n", index + 1, entry.getTitle()));

        String authors = formatAuthors(entry);
        if (!authors.isEmpty()) {
            result.append(String.format("    Authors: %s\n", authors));
        }

        if (entry.getPublishedDate() != null) {
            result.append(String.format("    Published: %s\n", DATE_FORMAT.format(entry.getPublishedDate())));
        }

        if (entry.getLink() != null) {
            result.append(String.format("    Link: %s\n", entry.getLink()));
        }
    }
}
