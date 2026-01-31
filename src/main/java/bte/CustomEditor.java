package bte;

import javafx.scene.Node;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.reactfx.util.Either;

import java.util.Optional;

public class CustomEditor extends GenericStyledArea<String, Either<String, Node>, String> {

    public CustomEditor() {
        super(
                "", // initial paragraph style
                (textFlow, pStyle) -> {
                    if (pStyle != null && !pStyle.isEmpty()) {
                        textFlow.setStyle(pStyle);
                    }
                },
                "", // initial segment style
                createTextOps(),
                (styledSegment) -> {
                    Either<String, Node> segment = styledSegment.getSegment();
                    String style = styledSegment.getStyle();
                    if (segment.isLeft()) {
                        TextExt text = new TextExt(segment.getLeft());
                        if (style != null && !style.isEmpty())
                            text.setStyle(style);
                        return text;
                    } else {
                        return segment.getRight();
                    }
                });
    }

    private static TextOps<Either<String, Node>, String> createTextOps() {
        TextOps<String, String> textOps = SegmentOps.styledTextOps();
        SegmentOps<Node, String> nodeOps = new SegmentOps<Node, String>() {
            @Override
            public int length(Node seg) {
                return 1;
            }

            @Override
            public char charAt(Node seg, int index) {
                return '\ufffc';
            }

            @Override
            public String getText(Node seg) {
                return "\ufffc";
            }

            @Override
            public Node subSequence(Node seg, int start, int end) {
                return seg;
            }

            @Override
            public Node subSequence(Node seg, int start) {
                return seg;
            }

            @Override
            public Optional<Node> joinSeg(Node current, Node next) {
                return Optional.empty();
            }

            @Override
            public Node createEmptySeg() {
                return null;
            }
        };

        return TextOps.<String, Node, String>eitherL(textOps, nodeOps, (s1, s2) -> Optional.of(s1));
    }

    public void insertImage(Node image) {
        int pos = this.getCaretPosition();
        this.replace(pos, pos, Either.<String, Node>right(image), "");
    }

    public void insertTable(Node table) {
        int pos = this.getCaretPosition();
        this.replace(pos, pos, Either.<String, Node>right(table), "");
    }

    public void replaceText(int start, int end, String text, String style) {
        replaceText(start, end, text);
        setStyle(start, start + text.length(), style);
    }

    /**
     * Creates a specialized CustomEditor for header/footer areas.
     * These editors have constrained height and special styling.
     */
    public static CustomEditor createHeaderFooterEditor(boolean isEditable) {
        CustomEditor editor = new CustomEditor();
        editor.setMaxHeight(40);
        editor.setMinHeight(40);
        editor.setPrefHeight(40);
        editor.setStyle(
                "-fx-font-size: 10px; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0; -fx-background-color: #FAFAFA;");
        editor.setEditable(isEditable);
        editor.setWrapText(true);
        return editor;
    }

}
