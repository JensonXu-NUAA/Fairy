package cn.nuaa.jensonxu.fairy.common.parser.document.impl;

import cn.nuaa.jensonxu.fairy.common.data.rag.ImageSection;
import cn.nuaa.jensonxu.fairy.common.data.rag.PositionInfo;
import cn.nuaa.jensonxu.fairy.common.data.rag.TextSection;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParser;
import cn.nuaa.jensonxu.fairy.common.parser.document.PdfStructuredParseResult;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * PDF 文档解析器
 * 使用 Apache PDFBox 实现
 */
@Slf4j
public class PdfDocumentParser implements DocumentParser {

    private static final double TEXT_LINE_MERGE_THRESHOLD = 2.0;
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf"};


    @Override
    public DocumentParseResult parse(File file) {
        return parseDocument(file, null, null);
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName) {
        return parseDocument(null, inputStream, null);
    }

    @Override
    public DocumentParseResult parseWithPassword(File file, String password) {
        return parseDocument(file, null, password);
    }

    @Override
    public DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        return parseDocument(null, inputStream, password);
    }

    @Override
    public boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    /**
     * 结构化解析 PDF（文本段 + 图片段）
     * 当前为骨架实现：先复用现有解析流程，返回空的 sections/images。
     */
    public PdfStructuredParseResult parseStructured(InputStream inputStream, String fileName) {
        long startTime = System.currentTimeMillis();
        try (PDDocument document = loadDocumentForStructured(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);

            Map<String, String> metadata = extractMetadata(document);
            List<TextSection> textSections = extractTextSections(document);
            List<ImageSection> imageSections = extractImageSections(document);

            if (ObjectUtils.isEmpty(metadata)) {
                metadata = new HashMap<>();
            }
            metadata.put("has_images", String.valueOf(!imageSections.isEmpty()));

            return PdfStructuredParseResult.builder()
                    .success(true)
                    .textSections(textSections)
                    .imageSections(imageSections)
                    .metadata(metadata)
                    .pageCount(document.getNumberOfPages())
                    .charCount(content == null ? 0 : content.length())
                    .parseDuration(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            return PdfStructuredParseResult.builder()
                    .success(false)
                    .errorMessage("PDF 结构化解析失败: " + e.getMessage())
                    .parseDuration(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 统一的解析逻辑
     */
    private DocumentParseResult parseDocument(File file, InputStream inputStream, String password) {
        long startTime = System.currentTimeMillis();
        try {
            PDDocument document;
            if(file != null) {
                document = password != null
                        ? Loader.loadPDF(file, password)
                        : Loader.loadPDF(file);
            } else {
                document = password != null
                        ? Loader.loadPDF(new RandomAccessReadBuffer(inputStream), password)
                        : Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
            }

            try (document) {
                if (document.isEncrypted() && password == null) {
                    return DocumentParseResult.passwordRequired();  // 检查是否加密且未解锁
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String content = stripper.getText(document);  // 提取文本
                Map<String, String> metaData = extractMetadata(document);  // 提取元数据
                int pageCount = document.getNumberOfPages();  // 获取页数

                return DocumentParseResult.builder()
                        .success(true)
                        .content(content)
                        .contentType(CONTENT_TYPE)
                        .metadata(metaData)
                        .pageCount(pageCount)
                        .charCount(content.length())
                        .encrypted(document.isEncrypted())
                        .parseDuration(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (InvalidPasswordException e) {
            log.warn("[pdf] PDF 密码错误或文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[pdf] PDF 解析失败", e);
            return DocumentParseResult.failure("PDF 解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取 PDF 元数据
     */
    private Map<String, String> extractMetadata(PDDocument document) {
        Map<String, String> metadata = new HashMap<>();
        PDDocumentInformation info = document.getDocumentInformation();

        if (info != null) {
            putIfNotNull(metadata, "title", info.getTitle());
            putIfNotNull(metadata, "author", info.getAuthor());
            putIfNotNull(metadata, "subject", info.getSubject());
            putIfNotNull(metadata, "keywords", info.getKeywords());
            putIfNotNull(metadata, "creator", info.getCreator());
            putIfNotNull(metadata, "producer", info.getProducer());

            if (info.getCreationDate() != null) {
                metadata.put("creationDate", info.getCreationDate().getTime().toString());
            }
            if (info.getModificationDate() != null) {
                metadata.put("modificationDate", info.getModificationDate().getTime().toString());
            }
        }

        return metadata;
    }

    /**
     * 逐页提取文本段
     * 当前策略：每页提取为一个 TextSection，位置先用占位值，后续再替换为真实 bbox。
     */
    private List<TextSection> extractTextSections(PDDocument document) throws Exception {
        PositionAwareTextStripper stripper = new PositionAwareTextStripper();

        int pageCount = document.getNumberOfPages();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int pageNo = pageIndex + 1;
            stripper.setStartPage(pageNo);
            stripper.setEndPage(pageNo);
            stripper.setCurrentPageIndex(pageIndex);
            stripper.getText(document);
        }

        List<TextFragment> fragments = stripper.getFragments();
        if (fragments.isEmpty()) {
            return List.of();
        }

        // 先按页、top排序
        fragments.sort((a, b) -> {
            if (a.getPageIndex() != b.getPageIndex()) {
                return Integer.compare(a.getPageIndex(), b.getPageIndex());
            }
            return Double.compare(a.getTop(), b.getTop());
        });

        List<TextSection> sections = new ArrayList<>();
        int currPage = -1;
        double currTop = 0;
        double currBottom = 0;
        StringBuilder currText = new StringBuilder();

        for (TextFragment fragment : fragments) {
            if (fragment.getText() == null || fragment.getText().isBlank()) {
                continue;
            }

            boolean startNew = false;
            if (currPage == -1) {
                startNew = true;
            } else if (fragment.getPageIndex() != currPage) {
                startNew = true;
            } else if (Math.abs(fragment.getTop() - currTop) > TEXT_LINE_MERGE_THRESHOLD) {
                startNew = true;
            }

            if (startNew) {
                if (!currText.isEmpty()) {
                    sections.add(TextSection.builder()
                            .text(currText.toString().trim())
                            .position(new PositionInfo(currPage, currTop, currBottom, 0, 1000))
                            .title(false)
                            .titleLevel(0)
                            .table(false)
                            .tokenCount(0)
                            .build());
                }

                currPage = fragment.getPageIndex();
                currTop = fragment.getTop();
                currBottom = fragment.getBottom();
                currText.setLength(0);
                currText.append(fragment.getText());
            } else {
                currBottom = Math.max(currBottom, fragment.getBottom());
                currText.append(" ").append(fragment.getText());
            }
        }

        if (!currText.isEmpty()) {
            sections.add(TextSection.builder()
                    .text(currText.toString().trim())
                    .position(new PositionInfo(currPage, currTop, currBottom, 0, 1000))
                    .title(false)
                    .titleLevel(0)
                    .table(false)
                    .tokenCount(0)
                    .build());
        }

        return sections;
    }

    /**
     * 逐页提取图片段
     */
    private List<ImageSection> extractImageSections(PDDocument document) {
        List<ImageSection> images = new ArrayList<>();

        try {
            int pageIndex = 0;
            for (PDPage page : document.getPages()) {
                int currentPageIndex = pageIndex;
                PDFStreamEngine engine = new PDFStreamEngine() {
                    @Override
                    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
                        String opName = operator.getName();
                        if ("Do".equals(opName) && operands != null && !operands.isEmpty()) {
                            COSName objectName = (COSName) operands.get(0);
                            PDXObject xObject = getResources().getXObject(objectName);
                            if (xObject instanceof PDImageXObject imageXObject) {
                                BufferedImage buffered = imageXObject.getImage();
                                if (buffered != null) {
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    String format = imageXObject.getSuffix() == null ? "png" : imageXObject.getSuffix();
                                    ImageIO.write(buffered, format, stream);
                                    String base64 = Base64.getEncoder().encodeToString(stream.toByteArray());

                                    // 从当前图形状态获取图片放置矩阵
                                    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                                    double x = ctm.getTranslateX();
                                    double y = ctm.getTranslateY();
                                    double w = ctm.getScalingFactorX();
                                    double h = ctm.getScalingFactorY();

                                    // 归一化坐标，避免旋转/负缩放导致上下左右颠倒
                                    double left = Math.min(x, x + w);
                                    double right = Math.max(x, x + w);
                                    double top = Math.min(y, y + h);
                                    double bottom = Math.max(y, y + h);

                                    ImageSection imageSection = ImageSection.builder()
                                            .imageId("pdf-p" + currentPageIndex + "-" + System.nanoTime())
                                            .imageData(base64)
                                            .position(new PositionInfo(currentPageIndex, top, bottom, left, right))
                                            .width(buffered.getWidth())
                                            .height(buffered.getHeight())
                                            .mimeType("image/" + format.toLowerCase())
                                            .build();

                                    images.add(imageSection);
                                }
                            }
                        }
                        super.processOperator(operator, operands);
                    }
                };

                engine.processPage(page);
                pageIndex++;
            }
        } catch (Exception e) {
            log.warn("[pdf] 提取图片失败: {}", e.getMessage());
        }

        return images;
    }

    private PDDocument loadDocumentForStructured(InputStream inputStream) throws Exception {
        return Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    @Data
    private static class TextFragment {
        private final String text;
        private final int pageIndex;
        private final double top;
        private final double bottom;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class PositionAwareTextStripper extends PDFTextStripper {
        private final List<TextFragment> fragments = new ArrayList<>();
        private int currentPageIndex = 0;

        private PositionAwareTextStripper() throws IOException {
            super();
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (text == null || text.isBlank() || textPositions == null || textPositions.isEmpty()) {
                return;
            }

            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;

            for (TextPosition tp : textPositions) {
                if (tp == null) {
                    continue;
                }
                double y = tp.getYDirAdj();
                double h = tp.getHeightDir();
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y + h);
            }

            if (minY == Double.MAX_VALUE) {
                return;
            }

            fragments.add(new TextFragment(text.trim(), currentPageIndex, minY, maxY));
        }
    }

}
