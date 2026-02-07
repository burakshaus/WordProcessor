
package bte;

import java.util.List;

public class TOCManager {

    public static class HeadingInfo {
        String text;
        int level;
        int paragraphIndex;
        String id;

        public HeadingInfo(String text, int level, int paragraphIndex, String id) {
            this.text = text;
            this.level = level;
            this.paragraphIndex = paragraphIndex;
            this.id = id;
        }

        public static List<HeadingInfo> scanHeadings(CustomEditor editor) {
            List<HeadingInfo> headings = new java.util.ArrayList<>();

            // Editor'daki tüm paragrafları tara
            int numParagraphs = editor.getParagraphs().size();

            for (int i = 0; i < numParagraphs; i++) {
                // Paragrafın metnini al
                int paraStart = editor.getAbsolutePosition(i, 0);
                int paraEnd = paraStart + editor.getParagraphLength(i);
                String text = editor.getText(paraStart, paraEnd).trim();

                // Boş paragrafları atla
                if (text.isEmpty()) {
                    continue;
                }

                // Paragraftaki stil bilgisini al (ilk karakterden)
                String segmentStyle = "";
                try {
                    if (paraEnd > paraStart) {
                        segmentStyle = editor.getStyleAtPosition(paraStart);
                        if (segmentStyle == null) {
                            segmentStyle = "";
                        }
                    }
                } catch (Exception e) {
                    segmentStyle = "";
                }

                // Font boyutunu stil string'inden çıkar
                int fontSize = extractFontSize(segmentStyle);

                // Başlık seviyesini belirle (font boyutuna göre)
                // Heading 1: 32px, Heading 2: 24px, Heading 3: 18px, vb.
                int level = -1;
                if (fontSize >= 32) {
                    level = 1; // Heading 1
                } else if (fontSize >= 24) {
                    level = 2; // Heading 2
                } else if (fontSize >= 18) {
                    level = 3; // Heading 3
                }

                // Eğer başlık ise listeye ekle
                if (level > 0) {
                    String id = "heading-" + i; // Basit bir ID oluştur
                    headings.add(new HeadingInfo(text, level, i, id));
                }
            }

            return headings;
        }

        // Yardımcı metod: CSS stil string'inden font-size'ı çıkar
        private static int extractFontSize(String style) {
            if (style == null || style.isEmpty()) {
                return 14; // Varsayılan font boyutu
            }

            // "-fx-font-size: 24px;" formatından 24'ü çıkar
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

    /**
     * İçindekiler tablosu için bir VBox UI bileşeni oluşturur
     * 
     * @param headings Başlık listesi
     * @param editor   İçerik editörü (navigasyon için)
     * @return Formatlanmış içindekiler tablosu VBox'ı
     */
    public static javafx.scene.layout.VBox generateTOC(List<HeadingInfo> headings, CustomEditor editor) {
        javafx.scene.layout.VBox tocBox = new javafx.scene.layout.VBox(5);
        tocBox.setStyle(
                "-fx-padding: 10; -fx-background-color: #F5F5F5; -fx-border-color: #CCCCCC; -fx-border-width: 1;");

        // Başlık ekle
        javafx.scene.text.Text title = new javafx.scene.text.Text("İçindekiler");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        tocBox.getChildren().add(title);

        // Ayırıcı çizgi
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        tocBox.getChildren().add(separator);

        // Her başlık için link oluştur
        for (HeadingInfo heading : headings) {
            javafx.scene.control.Hyperlink link = new javafx.scene.control.Hyperlink(heading.text);

            // Seviyeye göre girintileme
            double leftPadding = (heading.level - 1) * 20;
            link.setStyle(String.format("-fx-padding: 2 0 2 %.0fpx; -fx-font-size: %dpx;",
                    leftPadding,
                    14 - (heading.level * 2)));

            // Tıklandığında ilgili başlığa git
            link.setOnAction(e -> navigateToHeading(editor, heading.paragraphIndex));

            tocBox.getChildren().add(link);
        }

        return tocBox;
    }

    /**
     * İçindekiler tablosunu editörün cursor pozisyonuna ekler
     * 
     * @param editor İçerik editörü
     * @param tocBox İçindekiler VBox'ı
     */
    public static void insertTOCAtCursor(CustomEditor editor, javafx.scene.layout.VBox tocBox) {
        int pos = editor.getCaretPosition();

        // Yeni satır ekle (eğer cursor başlangıçta değilse)
        if (pos > 0) {
            editor.insertText(pos, "\n");
            pos++;
        }

        // TOC'yi ResizableTOCView içine sar (300x400 başlangıç boyutu)
        ResizableTOCView resizableTOC = new ResizableTOCView(tocBox, 300, 400);

        // Resizable TOC'yi Node olarak ekle
        editor.replace(pos, pos, org.reactfx.util.Either.right(resizableTOC), "");

        // TOC'den sonra yeni satır ekle
        editor.insertText(pos + 1, "\n");
    }

    /**
     * Editor'da belirtilen paragraf indeksine yönlendirir
     * 
     * @param editor         İçerik editörü
     * @param paragraphIndex Hedef paragraf indeksi
     */
    public static void navigateToHeading(CustomEditor editor, int paragraphIndex) {
        if (paragraphIndex >= 0 && paragraphIndex < editor.getParagraphs().size()) {
            int position = editor.getAbsolutePosition(paragraphIndex, 0);
            editor.moveTo(position);
            editor.requestFocus();
            editor.requestFollowCaret();
        }
    }

    /**
     * Dokümandaki başlıkları tarar ve otomatik olarak içindekiler tablosu oluşturur
     * 
     * @param editor İçerik editörü
     * @return Oluşturulan içindekiler VBox'ı veya başlık yoksa null
     */
    public static javafx.scene.layout.VBox createAndInsertTOC(CustomEditor editor) {
        List<HeadingInfo> headings = HeadingInfo.scanHeadings(editor);

        if (headings.isEmpty()) {
            return null; // Başlık yoksa null döndür
        }

        javafx.scene.layout.VBox tocBox = generateTOC(headings, editor);
        insertTOCAtCursor(editor, tocBox);

        return tocBox;
    }
}
