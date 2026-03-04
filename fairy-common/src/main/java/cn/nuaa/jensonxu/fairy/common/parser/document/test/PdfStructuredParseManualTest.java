package cn.nuaa.jensonxu.fairy.common.parser.document.test;

import cn.nuaa.jensonxu.fairy.common.parser.document.PdfStructuredParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.impl.PdfDocumentParser;

import java.io.FileInputStream;
import java.io.InputStream;

public class PdfStructuredParseManualTest {

    public static void main(String[] args) throws Exception {
        String pdfPath = "/Users/jensonxu/Desktop/test1.pdf"; // 换成你的真实 PDF 路径

        PdfDocumentParser parser = new PdfDocumentParser();
        try (InputStream in = new FileInputStream(pdfPath)) {
            PdfStructuredParseResult result = parser.parseStructured(in, "sample.pdf");
            System.out.println("success=" + result.isSuccess());
            System.out.println("pageCount=" + result.getPageCount());
            System.out.println("textSections=" + (result.getTextSections() == null ? 0 : result.getTextSections().size()));
            System.out.println("imageSections=" + (result.getImageSections() == null ? 0 : result.getImageSections().size()));
            System.out.println("metadata=" + result.getMetadata());

            if (!result.isSuccess()) {
                System.out.println("error=" + result.getErrorMessage());
            }

            if (result.getTextSections() != null) {
                int limit = Math.min(5, result.getTextSections().size());
                for (int i = 0; i < limit; i++) {
                    var s = result.getTextSections().get(i);
                    System.out.println("textSection[" + i + "] page=" + (s.getPosition() == null ? "null" : s.getPosition().getPageNum())
                            + ", top=" + (s.getPosition() == null ? "null" : s.getPosition().getTop())
                            + ", bottom=" + (s.getPosition() == null ? "null" : s.getPosition().getBottom())
                            + ", text=" + s.getText());
                }
            }

        }
    }
}

