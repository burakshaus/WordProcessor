package bte;

/**
 * Manages header and footer content for all pages in the document.
 * Supports different headers/footers for the first page and page numbering.
 */
public class HeaderFooterManager {

    // Header/Footer content
    private String defaultHeaderContent = "";
    private String defaultFooterContent = "";
    private String firstPageHeaderContent = "";
    private String firstPageFooterContent = "";

    // Settings
    private boolean differentFirstPage = false;
    private boolean showPageNumbers = false;
    private PageNumberPosition pageNumberPosition = PageNumberPosition.BOTTOM_CENTER;
    private PageNumberFormat pageNumberFormat = PageNumberFormat.ARABIC;

    /**
     * Page number format options
     */
    public enum PageNumberFormat {
        ARABIC("1, 2, 3"),
        ROMAN("I, II, III"),
        LETTERS("a, b, c");

        private final String displayName;

        PageNumberFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String formatPageNumber(int pageNumber) {
            switch (this) {
                case ARABIC:
                    return String.valueOf(pageNumber);
                case ROMAN:
                    return toRoman(pageNumber);
                case LETTERS:
                    return toLetters(pageNumber);
                default:
                    return String.valueOf(pageNumber);
            }
        }

        private String toRoman(int num) {
            String[] thousands = { "", "M", "MM", "MMM" };
            String[] hundreds = { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" };
            String[] tens = { "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" };
            String[] ones = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };

            return thousands[num / 1000] + hundreds[(num % 1000) / 100] +
                    tens[(num % 100) / 10] + ones[num % 10];
        }

        private String toLetters(int num) {
            if (num <= 0)
                return "";
            StringBuilder result = new StringBuilder();
            num--; // Convert to 0-based index
            while (num >= 0) {
                result.insert(0, (char) ('a' + (num % 26)));
                num = num / 26 - 1;
            }
            return result.toString();
        }
    }

    /**
     * Page number position options
     */
    public enum PageNumberPosition {
        TOP_LEFT("Top Left"),
        TOP_CENTER("Top Center"),
        TOP_RIGHT("Top Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_CENTER("Bottom Center"),
        BOTTOM_RIGHT("Bottom Right");

        private final String displayName;

        PageNumberPosition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isInHeader() {
            return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
        }

        public boolean isInFooter() {
            return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
        }

        public String getAlignment() {
            switch (this) {
                case TOP_LEFT:
                case BOTTOM_LEFT:
                    return "left";
                case TOP_CENTER:
                case BOTTOM_CENTER:
                    return "center";
                case TOP_RIGHT:
                case BOTTOM_RIGHT:
                    return "right";
                default:
                    return "center";
            }
        }
    }

    // Getters and setters

    public void setHeaderContent(String content, boolean isFirstPage) {
        if (isFirstPage) {
            firstPageHeaderContent = content;
        } else {
            defaultHeaderContent = content;
        }
    }

    public void setFooterContent(String content, boolean isFirstPage) {
        if (isFirstPage) {
            firstPageFooterContent = content;
        } else {
            defaultFooterContent = content;
        }
    }

    public String getHeaderForPage(int pageNumber) {
        if (differentFirstPage && pageNumber == 1) {
            return firstPageHeaderContent;
        }
        return defaultHeaderContent;
    }

    public String getFooterForPage(int pageNumber) {
        if (differentFirstPage && pageNumber == 1) {
            return firstPageFooterContent;
        }
        return defaultFooterContent;
    }

    public String getCompleteHeaderForPage(int pageNumber) {
        String header = getHeaderForPage(pageNumber);

        // Add page number if configured for header
        if (showPageNumbers && pageNumberPosition.isInHeader()) {
            String pageNum = pageNumberFormat.formatPageNumber(pageNumber);
            header = insertPageNumber(header, pageNum, pageNumberPosition);
        }

        return header;
    }

    public String getCompleteFooterForPage(int pageNumber) {
        String footer = getFooterForPage(pageNumber);

        // Add page number if configured for footer
        if (showPageNumbers && pageNumberPosition.isInFooter()) {
            String pageNum = pageNumberFormat.formatPageNumber(pageNumber);
            footer = insertPageNumber(footer, pageNum, pageNumberPosition);
        }

        return footer;
    }

    private String insertPageNumber(String content, String pageNum, PageNumberPosition position) {
        // If content is empty, just return the page number
        if (content == null || content.trim().isEmpty()) {
            return pageNum;
        }

        // For now, append page number to content
        // TODO: Support more sophisticated positioning
        return content + " " + pageNum;
    }

    public boolean isDifferentFirstPage() {
        return differentFirstPage;
    }

    public void setDifferentFirstPage(boolean differentFirstPage) {
        this.differentFirstPage = differentFirstPage;
    }

    public boolean isShowPageNumbers() {
        return showPageNumbers;
    }

    public void setShowPageNumbers(boolean showPageNumbers) {
        this.showPageNumbers = showPageNumbers;
    }

    public PageNumberPosition getPageNumberPosition() {
        return pageNumberPosition;
    }

    public void setPageNumberPosition(PageNumberPosition pageNumberPosition) {
        this.pageNumberPosition = pageNumberPosition;
    }

    public PageNumberFormat getPageNumberFormat() {
        return pageNumberFormat;
    }

    public void setPageNumberFormat(PageNumberFormat pageNumberFormat) {
        this.pageNumberFormat = pageNumberFormat;
    }

    public String getDefaultHeaderContent() {
        return defaultHeaderContent;
    }

    public String getDefaultFooterContent() {
        return defaultFooterContent;
    }

    public String getFirstPageHeaderContent() {
        return firstPageHeaderContent;
    }

    public String getFirstPageFooterContent() {
        return firstPageFooterContent;
    }

    /**
     * Clear all header and footer content
     */
    public void clear() {
        defaultHeaderContent = "";
        defaultFooterContent = "";
        firstPageHeaderContent = "";
        firstPageFooterContent = "";
        differentFirstPage = false;
        showPageNumbers = false;
    }
}
