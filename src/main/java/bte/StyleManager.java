package bte;

import javafx.scene.paint.Color;
import javafx.scene.control.IndexRange;

/**
 * Microsoft Word benzeri metin stillerini yöneten sınıf.
 * Heading 1-3, Normal, Title, Subtitle, Quote ve Code stillerini içerir.
 */
public class StyleManager {

    /**
     * Metin stili bilgilerini tutan sınıf
     */
    public static class TextStyle {
        private String name;
        private String displayName;
        private int fontSize;
        private boolean isBold;
        private boolean isItalic;
        private String fontFamily;
        private Color textColor;
        private String additionalStyles; // Ek CSS stilleri

        public TextStyle(String name, String displayName, int fontSize, boolean isBold,
                boolean isItalic, String fontFamily, Color textColor, String additionalStyles) {
            this.name = name;
            this.displayName = displayName;
            this.fontSize = fontSize;
            this.isBold = isBold;
            this.isItalic = isItalic;
            this.fontFamily = fontFamily;
            this.textColor = textColor;
            this.additionalStyles = additionalStyles;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getFontSize() {
            return fontSize;
        }

        public boolean isBold() {
            return isBold;
        }

        public boolean isItalic() {
            return isItalic;
        }

        public String getFontFamily() {
            return fontFamily;
        }

        public Color getTextColor() {
            return textColor;
        }

        public String getAdditionalStyles() {
            return additionalStyles;
        }

        /**
         * Bu stilin CSS formatını döndürür
         */
        public String toCSSStyle() {
            StringBuilder css = new StringBuilder();
            css.append("-fx-font-family: '").append(fontFamily).append("'; ");
            css.append("-fx-font-size: ").append(fontSize).append("px; ");

            if (isBold) {
                css.append("-fx-font-weight: bold; ");
            } else {
                css.append("-fx-font-weight: normal; ");
            }

            if (isItalic) {
                css.append("-fx-font-style: italic; ");
            } else {
                css.append("-fx-font-style: normal; ");
            }

            if (textColor != null) {
                String colorHex = String.format("#%02X%02X%02X",
                        (int) (textColor.getRed() * 255),
                        (int) (textColor.getGreen() * 255),
                        (int) (textColor.getBlue() * 255));
                css.append("-fx-fill: ").append(colorHex).append("; ");
            }

            if (additionalStyles != null && !additionalStyles.isEmpty()) {
                css.append(additionalStyles);
            }

            return css.toString().trim();
        }
    }

    // Önceden tanımlanmış stiller
    public static final TextStyle NORMAL = new TextStyle(
            "Normal", "Normal",
            14, false, false, "Segoe UI", Color.BLACK, "");

    public static final TextStyle HEADING_1 = new TextStyle(
            "Heading1", "Heading 1",
            32, true, false, "Segoe UI", Color.web("#2E74B5"), "");

    public static final TextStyle HEADING_2 = new TextStyle(
            "Heading2", "Heading 2",
            24, true, false, "Segoe UI", Color.web("#2E74B5"), "");

    public static final TextStyle HEADING_3 = new TextStyle(
            "Heading3", "Heading 3",
            18, true, false, "Segoe UI", Color.web("#2E74B5"), "");

    public static final TextStyle TITLE = new TextStyle(
            "Title", "Title",
            36, true, false, "Segoe UI", Color.BLACK, "");

    public static final TextStyle SUBTITLE = new TextStyle(
            "Subtitle", "Subtitle",
            16, false, true, "Segoe UI", Color.web("#5A5A5A"), "");

    public static final TextStyle QUOTE = new TextStyle(
            "Quote", "Quote",
            14, false, true, "Georgia", Color.web("#404040"),
            "-fx-border-left-width: 3px; -fx-border-left-color: #CCCCCC;");

    public static final TextStyle CODE = new TextStyle(
            "Code", "Code",
            12, false, false, "Courier New", Color.web("#D14"),
            "-fx-background-color: #F5F5F5;");

    // Tüm stillerin listesi
    public static final TextStyle[] ALL_STYLES = {
            NORMAL, HEADING_1, HEADING_2, HEADING_3,
            TITLE, SUBTITLE, QUOTE, CODE
    };

    /**
     * Stil ismine göre TextStyle döndürür
     */
    public static TextStyle getStyleByName(String name) {
        for (TextStyle style : ALL_STYLES) {
            if (style.getName().equals(name)) {
                return style;
            }
        }
        return NORMAL;
    }

    /**
     * Belirtilen stili CustomEditor'daki seçili metne uygular
     */
    public static void applyStyle(CustomEditor editor, TextStyle style) {
        IndexRange selection = editor.getSelection();

        if (selection.getLength() > 0) {
            // Seçili metin varsa, sadece seçili metne stil uygula
            editor.setStyle(selection.getStart(), selection.getEnd(), style.toCSSStyle());
        } else {
            // Seçili metin yoksa, mevcut paragrafa stil uygula
            int currentParagraph = editor.getCurrentParagraph();
            int paraStart = editor.getAbsolutePosition(currentParagraph, 0);
            int paraEnd = paraStart + editor.getParagraphLength(currentParagraph);

            if (paraEnd > paraStart) {
                editor.setStyle(paraStart, paraEnd, style.toCSSStyle());
            }
        }

        editor.requestFocus();
    }

    /**
     * Mevcut cursor pozisyonundaki/seçili metindeki stilin ismini döndürür
     * Stil tespit edilemezse "Normal" döndürür
     */
    public static String getCurrentStyleName(CustomEditor editor) {
        IndexRange selection = editor.getSelection();
        int position = selection.getStart();

        if (position >= editor.getLength()) {
            position = editor.getLength() - 1;
        }

        if (position < 0) {
            return "Normal";
        }

        try {
            String style = editor.getStyleAtPosition(position);
            if (style == null || style.isEmpty()) {
                return "Normal";
            }

            // Font boyutunu çıkar ve stil tahmin et
            int fontSize = extractFontSize(style);
            boolean isBold = style.contains("bold");

            if (fontSize >= 32 && isBold)
                return "Heading1";
            if (fontSize >= 24 && isBold)
                return "Heading2";
            if (fontSize >= 18 && isBold)
                return "Heading3";
            if (fontSize >= 36)
                return "Title";
            if (style.contains("italic") && fontSize == 16)
                return "Subtitle";
            if (style.contains("Georgia"))
                return "Quote";
            if (style.contains("Courier"))
                return "Code";

        } catch (Exception e) {
            // Hata durumunda Normal döndür
        }

        return "Normal";
    }

    /**
     * CSS stilinden font boyutunu çıkarır
     */
    private static int extractFontSize(String style) {
        if (style == null || style.isEmpty()) {
            return 14;
        }

        String[] parts = style.split(";");
        for (String part : parts) {
            if (part.contains("-fx-font-size:")) {
                String sizeStr = part.split(":")[1].trim();
                sizeStr = sizeStr.replace("px", "").trim();
                try {
                    return Integer.parseInt(sizeStr);
                } catch (NumberFormatException e) {
                    return 14;
                }
            }
        }
        return 14;
    }
}
