package bte;

import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.PdfWriter;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Either;
import javafx.scene.Node;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFExporter {

    /**
     * Export all editors (pages) to PDF
     */
    public static void export(List<CustomEditor> editors, File file) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        for (int i = 0; i < editors.size(); i++) {
            CustomEditor editor = editors.get(i);

            // Add content from each editor
            exportEditorContent(editor, document);

            // Add page break between pages (except after the last page)
            if (i < editors.size() - 1) {
                document.newPage();
            }
        }

        document.close();
    }

    /**
     * Export a single editor to PDF (backward compatibility)
     */
    public static void export(CustomEditor editor, File file) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        exportEditorContent(editor, document);

        document.close();
    }

    /**
     * Helper method to export content from a single editor to an existing PDF
     * document
     */
    private static void exportEditorContent(CustomEditor editor, Document document) throws Exception {
        for (Paragraph<String, Either<String, Node>, String> p : editor.getParagraphs()) {
            org.openpdf.text.Paragraph pdfParagraph = new org.openpdf.text.Paragraph();

            // Handle Paragraph Alignment
            String pStyle = p.getParagraphStyle();
            if (pStyle != null) {
                if (pStyle.contains("center"))
                    pdfParagraph.setAlignment(Element.ALIGN_CENTER);
                else if (pStyle.contains("right"))
                    pdfParagraph.setAlignment(Element.ALIGN_RIGHT);
                else if (pStyle.contains("justify"))
                    pdfParagraph.setAlignment(Element.ALIGN_JUSTIFIED);
                else
                    pdfParagraph.setAlignment(Element.ALIGN_LEFT);
            }

            for (StyledSegment<Either<String, Node>, String> styledSegment : p.getStyledSegments()) {
                Either<String, Node> segment = styledSegment.getSegment();
                String style = styledSegment.getStyle();

                if (segment.isLeft()) {
                    String text = segment.getLeft();
                    Font font = parseFont(style);
                    Chunk chunk = new Chunk(text, font);

                    // Handle Highlight (Background Color)
                    Color bgColor = parseHighlight(style);
                    if (bgColor != null) {
                        chunk.setBackground(bgColor);
                    }

                    pdfParagraph.add(chunk);
                } else {
                    // Placeholder for images/tables in PDF
                    pdfParagraph.add(new Chunk("[IMAGE/TABLE]"));
                }
            }
            document.add(pdfParagraph);
        }
    }

    private static Font parseFont(String style) {
        if (style == null || style.isEmpty())
            return new Font(Font.HELVETICA, 12);

        int styleFlags = Font.NORMAL;
        if (style.contains("bold"))
            styleFlags |= Font.BOLD;
        if (style.contains("italic"))
            styleFlags |= Font.ITALIC;
        if (style.contains("underline"))
            styleFlags |= Font.UNDERLINE;
        if (style.contains("strikethrough"))
            styleFlags |= Font.STRIKETHRU;

        float size = 12;
        Pattern sizePattern = Pattern.compile("-fx-font-size:\\s*(\\d+)px");
        Matcher sizeMatcher = sizePattern.matcher(style);
        if (sizeMatcher.find()) {
            size = Float.parseFloat(sizeMatcher.group(1));
        }

        Color color = Color.BLACK;
        Pattern colorPattern = Pattern.compile("-fx-fill:\\s*rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
        Matcher colorMatcher = colorPattern.matcher(style);
        if (colorMatcher.find()) {
            color = new Color(
                    Integer.parseInt(colorMatcher.group(1)),
                    Integer.parseInt(colorMatcher.group(2)),
                    Integer.parseInt(colorMatcher.group(3)));
        }

        int family = Font.HELVETICA;
        if (style.contains("Times New Roman") || style.contains("Georgia"))
            family = Font.TIMES_ROMAN;
        else if (style.contains("Courier New"))
            family = Font.COURIER;

        return new Font(family, size, styleFlags, color);
    }

    private static Color parseHighlight(String style) {
        if (style == null)
            return null;
        Pattern highlightPattern = Pattern.compile("-rtfx-background-color:\\s*rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
        Matcher matcher = highlightPattern.matcher(style);
        if (matcher.find()) {
            return new Color(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return null;
    }
}
