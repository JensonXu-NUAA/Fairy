package cn.nuaa.jensonxu.fairy.integration.mcp.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import lombok.extern.slf4j.Slf4j;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaperSearchService {

    private static final String ARXIV_QUERY_API = "https://export.arxiv.org/api/query";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final OkHttpClient httpClient;

    public PaperSearchService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行HTTP请求到arXiv API
     */
    private SyndFeed executeArxivRequest(String urlString) throws Exception {
        log.debug("[Paper Search Service] Request URL: {}", urlString);

        Request request = new Request.Builder()
                .url(urlString)
                .addHeader("User-Agent", "MCP-Paper-Search-Tool/1.0")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("[Paper Search Service] HTTP error code: {}", response.code());
                throw new Exception(String.format("HTTP error: %d", response.code()));
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                log.error("[Paper Search Service] Response body is null");
                throw new Exception("Empty response from arXiv");
            }

            byte[] responseBytes = responseBody.bytes();
            InputStream inputStream = new ByteArrayInputStream(responseBytes);
            SyndFeedInput input = new SyndFeedInput();
            return input.build(new XmlReader(inputStream));
        }
    }

    /**
     * 格式化作者信息
     */
    private String formatAuthors(SyndEntry entry) {
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
    private String formatAbstract(String description) {
        if (description == null) {
            return "";
        }
        
        return description
                .replaceAll("<[^>]*>", "") // 移除HTML标签
                .replaceAll("\\s+", " ") // 合并多个空格
                .trim();
    }

    /**
     * 构建基本论文信息字符串
     */
    private void appendBasicPaperInfo(StringBuilder result, SyndEntry entry, int index) {
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

    @Tool(description = "Search for academic papers on arXiv by keywords. Returns paper titles, authors, abstracts, and links. If you want to use this method, please use English.")
    public String searchPapers(
            @ToolParam(description = "Search keywords, e.g., 'machine learning', 'quantum computing'") String query,
            @ToolParam(description = "Maximum number of results to return, default is 10") Integer maxResults) {

        if (maxResults == null || maxResults <= 0) {
            maxResults = 10;
        }

        log.info("[Paper Search Service] Searching arXiv for: {}, max results: {}", query, maxResults);

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlString = String.format("%s?search_query=all:%s&start=0&max_results=%d&sortBy=submittedDate&sortOrder=descending", ARXIV_QUERY_API, encodedQuery, maxResults);

            log.info("[Paper Search Service] Request URL: {}", urlString);

            SyndFeed feed = executeArxivRequest(urlString);
            List<SyndEntry> entries = feed.getEntries();

            if (entries.isEmpty()) {
                log.info("[Paper Search Service] No papers found for query: {}", query);
                return String.format("No papers found for query: '%s'", query);
            }

            log.info("[Paper Search Service] Found {} papers", entries.size());

            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d papers for query '%s':\n\n", entries.size(), query));

            for (int i = 0; i < entries.size(); i++) {
                SyndEntry entry = entries.get(i);
                appendBasicPaperInfo(result, entry, i);

                // 摘要
                String description = entry.getDescription() != null ? entry.getDescription().getValue() : null;
                String abstractText = formatAbstract(description);
                if (!abstractText.isEmpty()) {
                    result.append(String.format("    Abstract: %s\n", abstractText));
                }

                result.append("\n");
            }

            result.append("Thank you to arXiv for use of its open access interoperability.");
            return result.toString();

        } catch (Exception e) {
            log.error("[Paper Search Service] Error searching papers: {}", e.getMessage(), e);
            return String.format("Error searching papers: %s", e.getMessage());
        }
    }

    @Tool(description = "Search for academic papers on arXiv by author name. If you want to use this method, please use English.")
    public String searchPapersByAuthor(
            @ToolParam(description = "Author name, e.g., 'Geoffrey Hinton'") String authorName,
            @ToolParam(description = "Maximum number of results to return, default is 10") Integer maxResults) {

        if (maxResults == null || maxResults <= 0) {
            maxResults = 10;
        }

        log.info("[Paper Search Service] Searching arXiv for author: {}, max results: {}", authorName, maxResults);

        try {
            // 构建作者查询URL
            String encodedAuthor = URLEncoder.encode(authorName, StandardCharsets.UTF_8);
            String urlString = String.format("%s?search_query=au:%s&start=0&max_results=%d&sortBy=submittedDate&sortOrder=descending",
                    ARXIV_QUERY_API, encodedAuthor, maxResults);

            log.debug("[Paper Search Service] Request URL: {}", urlString);

            SyndFeed feed = executeArxivRequest(urlString);
            List<SyndEntry> entries = feed.getEntries();

            if (entries.isEmpty()) {
                log.info("[Paper Search Service] No papers found for author: {}", authorName);
                return String.format("No papers found for author: '%s'", authorName);
            }

            log.info("[Paper Search Service] Found {} papers by author {}", entries.size(), authorName);

            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d papers by '%s':\n\n", entries.size(), authorName));

            for (int i = 0; i < entries.size(); i++) {
                SyndEntry entry = entries.get(i);
                result.append(String.format("[%d] %s\n", i + 1, entry.getTitle()));

                if (entry.getPublishedDate() != null) {
                    result.append(String.format("    Published: %s\n", DATE_FORMAT.format(entry.getPublishedDate())));
                }

                if (entry.getLink() != null) {
                    result.append(String.format("    Link: %s\n", entry.getLink()));
                }

                result.append("\n");
            }

            result.append("Thank you to arXiv for use of its open access interoperability.");
            return result.toString();

        } catch (Exception e) {
            log.error("[Paper Search Service] Error searching papers by author: {}", e.getMessage(), e);
            return String.format("Error searching papers by author: %s", e.getMessage());
        }
    }

    @Tool(description = "Get detailed information about a specific paper by its arXiv ID.")
    public String getPaperById(
            @ToolParam(description = "arXiv paper ID, e.g., '2301.12345' or 'cs/0501001'") String arxivId) {

        log.info("[Paper Search Service] Getting paper details for arXiv ID: {}", arxivId);

        try {
            // 构建ID查询URL
            String urlString = String.format("%s?id_list=%s", ARXIV_QUERY_API, arxivId);

            log.debug("[Paper Search Service] Request URL: {}", urlString);

            SyndFeed feed = executeArxivRequest(urlString);
            List<SyndEntry> entries = feed.getEntries();

            if (entries.isEmpty()) {
                log.info("[Paper Search Service] Paper not found for ID: {}", arxivId);
                return String.format("Paper not found for arXiv ID: '%s'", arxivId);
            }

            SyndEntry entry = entries.get(0);
            log.info("[Paper Search Service] Found paper: {}", entry.getTitle());

            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("Paper Details for arXiv ID: %s\n\n", arxivId));
            result.append(String.format("Title: %s\n\n", entry.getTitle()));

            // 作者信息
            String authors = formatAuthors(entry);
            if (!authors.isEmpty()) {
                result.append(String.format("Authors: %s\n\n", authors));
            }

            // 发布日期
            if (entry.getPublishedDate() != null) {
                result.append(String.format("Published: %s\n", DATE_FORMAT.format(entry.getPublishedDate())));
            }

            // 更新日期
            if (entry.getUpdatedDate() != null) {
                result.append(String.format("Updated: %s\n\n", DATE_FORMAT.format(entry.getUpdatedDate())));
            }

            // 分类
            if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
                List<String> categories = new ArrayList<>();
                entry.getCategories().forEach(category -> categories.add(category.getName()));
                result.append(String.format("Categories: %s\n\n", String.join(", ", categories)));
            }

            // 链接
            if (entry.getLink() != null) {
                result.append(String.format("Link: %s\n\n", entry.getLink()));
            }

            // 完整摘要
            String description = entry.getDescription() != null ? entry.getDescription().getValue() : null;
            String abstractText = formatAbstract(description);
            if (!abstractText.isEmpty()) {
                result.append(String.format("Abstract:\n%s\n\n", abstractText));
            }

            result.append("Thank you to arXiv for use of its open access interoperability.");
            return result.toString();

        } catch (Exception e) {
            log.error("[Paper Search Service] Error getting paper details: {}", e.getMessage(), e);
            return String.format("Error getting paper details: %s", e.getMessage());
        }
    }
}
