package cn.nuaa.jensonxu.fairy.common.parser.document.impl;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParser;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel 文档解析器
 * 使用 Apache POI 实现，支持 .xls 和 .xlsx 格式
 */
@Slf4j
public class ExcelDocumentParser implements DocumentParser {

    private static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";
    private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String[] SUPPORTED_EXTENSIONS = {".xls", ".xlsx"};

    private static final String CELL_SEPARATOR = "\t";  // 单元格分隔符
    private static final String ROW_SEPARATOR = "\n";  // 行分隔符
    private static final String SHEET_SEPARATOR = "\n\n========== %s ==========\n";  // 工作表分隔符

    @Override
    public DocumentParseResult parse(File file) {
        return parseDocument(file, null, null);
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName) {
        return parseDocument(null, inputStream, fileName);
    }

    @Override
    public DocumentParseResult parseWithPassword(File file, String password) {
        return parseDocument(file, null, password);
    }

    @Override
    public DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        return DocumentParseResult.failure("暂不支持加密 Excel 文档的流式解析，请使用文件方式");
    }

    /**
     * 统一的解析逻辑
     */
    private DocumentParseResult parseDocument(File file, InputStream inputStream, String fileNameOrPassword) {
        long startTime = System.currentTimeMillis();

        try {
            String fileName;
            InputStream is;

            if (file != null) {
                fileName = file.getName();
                is = new BufferedInputStream(new FileInputStream(file));
            } else {
                fileName = fileNameOrPassword;
                is = new BufferedInputStream(inputStream);
            }

            Workbook workbook;
            boolean isXlsx = fileName.toLowerCase().endsWith(".xlsx");
            if (isXlsx) {
                workbook = new XSSFWorkbook(is);
            } else {
                workbook = new HSSFWorkbook(is);
            }

            try (workbook) {
                String content = extractContent(workbook);  // 提取文本内容
                Map<String, String> metadata = extractMetadata(workbook, isXlsx);  // 提取元数据
                int sheetCount = workbook.getNumberOfSheets();  // 获取工作表数量
                long duration = System.currentTimeMillis() - startTime;

                return DocumentParseResult.builder()
                        .success(true)
                        .content(content)
                        .contentType(isXlsx ? CONTENT_TYPE_XLSX : CONTENT_TYPE_XLS)
                        .metadata(metadata)
                        .pageCount(sheetCount)
                        .charCount(content.length())
                        .encrypted(false)
                        .parseDuration(duration)
                        .build();
            }
        } catch (org.apache.poi.EncryptedDocumentException e) {
            log.warn("[excel] Excel 文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[excel] Excel 解析失败", e);
            return DocumentParseResult.failure("Excel 解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取 Excel 内容
     */
    private String extractContent(Workbook workbook) {
        StringBuilder content = new StringBuilder();
        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            content.append(String.format(SHEET_SEPARATOR, sheetName));  // 添加工作表标题

            // 遍历行
            for (Row row : sheet) {
                StringBuilder rowContent = new StringBuilder();
                boolean hasContent = false;

                // 遍历单元格
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String cellValue = "";

                    if (cell != null) {
                        cellValue = getCellValueAsString(cell, dataFormatter);
                        if (!cellValue.isEmpty()) {
                            hasContent = true;
                        }
                    }

                    if (cellIndex > 0) {
                        rowContent.append(CELL_SEPARATOR);
                    }
                    rowContent.append(cellValue);
                }

                // 只添加有内容的行
                if (hasContent) {
                    content.append(rowContent).append(ROW_SEPARATOR);
                }
            }
        }

        return content.toString().trim();
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(cell.getDateCellValue());
                }
                return dataFormatter.formatCellValue(cell);

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                try {
                    return dataFormatter.formatCellValue(cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }

            case BLANK:
                return "";

            default:
                return dataFormatter.formatCellValue(cell);
        }
    }

    /**
     * 提取元数据
     */
    private Map<String, String> extractMetadata(Workbook workbook, boolean isXlsx) {
        Map<String, String> metadata = new HashMap<>();

        try {
            if (isXlsx && workbook instanceof XSSFWorkbook) {
                XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
                POIXMLProperties props = xssfWorkbook.getProperties();
                POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();

                putIfNotNull(metadata, "title", coreProps.getTitle());
                putIfNotNull(metadata, "creator", coreProps.getCreator());
                putIfNotNull(metadata, "subject", coreProps.getSubject());
                putIfNotNull(metadata, "description", coreProps.getDescription());
                putIfNotNull(metadata, "keywords", coreProps.getKeywords());
                putIfNotNull(metadata, "lastModifiedBy", coreProps.getLastModifiedByUser());

                if (coreProps.getCreated() != null) {
                    metadata.put("created", coreProps.getCreated().toString());
                }
                if (coreProps.getModified() != null) {
                    metadata.put("modified", coreProps.getModified().toString());
                }
            } else if (workbook instanceof HSSFWorkbook) {
                HSSFWorkbook hssfWorkbook = (HSSFWorkbook) workbook;
                var summaryInfo = hssfWorkbook.getSummaryInformation();
                if (summaryInfo != null) {
                    putIfNotNull(metadata, "title", summaryInfo.getTitle());
                    putIfNotNull(metadata, "author", summaryInfo.getAuthor());
                    putIfNotNull(metadata, "subject", summaryInfo.getSubject());
                    putIfNotNull(metadata, "keywords", summaryInfo.getKeywords());
                    putIfNotNull(metadata, "lastAuthor", summaryInfo.getLastAuthor());

                    if (summaryInfo.getCreateDateTime() != null) {
                        metadata.put("createDate", summaryInfo.getCreateDateTime().toString());
                    }
                }
            }

            // 添加工作表信息
            metadata.put("sheetCount", String.valueOf(workbook.getNumberOfSheets()));
            StringBuilder sheetNames = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (i > 0) {
                    sheetNames.append(", ");
                }
                sheetNames.append(workbook.getSheetName(i));
            }
            metadata.put("sheetNames", sheetNames.toString());

        } catch (Exception e) {
            log.warn("[excel] 提取 Excel 元数据失败", e);
        }

        return metadata;
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
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
}
