package bte;

import javafx.application.Application;

import javafx.collections.FXCollections;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import org.reactfx.util.Either;

import javafx.print.PrinterJob;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;

import java.io.*;
import java.net.URL;

// TODO
public class MainApp extends Application {

    private static final double PAGE_WIDTH = 595;
    private static final double PAGE_HEIGHT = 842;
    private static final double MARGIN = 50;

    private List<CustomEditor> editors = new ArrayList<>();
    private List<CustomEditor> headerEditors = new ArrayList<>();
    private List<CustomEditor> footerEditors = new ArrayList<>();
    private int currentEditorIndex = 0;
    private File currentFile;
    private boolean isDirty = false;

    // Header/Footer management
    private HeaderFooterManager headerFooterManager = new HeaderFooterManager();

    private Label wordCountLabel;
    private Label charCountLabel;

    private ToggleButton boldBtn;
    private ToggleButton italicBtn;
    private ToggleButton underlineBtn;
    private ComboBox<String> fontFamilyCombo;
    private ComboBox<Integer> fontSizeCombo;
    private ColorPicker textColorPicker;
    private ToggleButton alignLeftBtn;
    private ToggleButton alignCenterBtn;
    private ToggleButton alignRightBtn;
    private ToggleButton alignJustifyBtn;
    private ToggleButton strikeBtn;
    private ToggleButton superBtn;
    private ToggleButton subBtn;
    private ColorPicker highlightPicker;
    private Button insertImageBtn;
    private Button insertTableBtn;
    private ToggleButton bulletListBtn;
    private ToggleButton numberedListBtn;

    // Microsoft Word Standard Color Palette (4 rows x 5 colors = 20 total)
    // Row 1: Theme Colors - Dark variants
    private final Color[] WORD_TEXT_COLORS = {
            Color.web("#C00000"), // Dark Red
            Color.web("#FF0000"), // Red
            Color.web("#FFC000"), // Orange
            Color.web("#FFFF00"), // Yellow
            Color.web("#92D050"), // Light Green

            // Row 2: Theme Colors - Standard
            Color.web("#00B050"), // Green
            Color.web("#00B0F0"), // Light Blue
            Color.web("#0070C0"), // Blue
            Color.web("#002060"), // Dark Blue
            Color.web("#7030A0"), // Purple

            // Row 3: Basic Colors
            Color.web("#000000"), // Black
            Color.web("#FFFFFF"), // White
            Color.web("#FF0000"), // Red
            Color.web("#FFC000"), // Orange
            Color.web("#FFFF00"), // Yellow

            // Row 4: More variants
            Color.web("#00FF00"), // Bright Green
            Color.web("#00FFFF"), // Cyan
            Color.web("#0000FF"), // Blue
            Color.web("#FF00FF"), // Magenta
            Color.web("#800080") // Purple
    };

    // Microsoft Word Highlight Color Palette (3 rows x 5 colors = 15 total)
    private final Color[] WORD_HIGHLIGHT_COLORS = {
            // Row 1: Bright highlights
            Color.web("#FFFF00"), // Yellow (most common highlight)
            Color.web("#00FF00"), // Bright Green
            Color.web("#00FFFF"), // Cyan
            Color.web("#FF00FF"), // Magenta
            Color.web("#0000FF"), // Blue

            // Row 2: Standard highlights
            Color.web("#FF0000"), // Red
            Color.web("#00008B"), // Dark Blue
            Color.web("#008080"), // Teal
            Color.web("#008000"), // Green
            Color.web("#800080"), // Violet

            // Row 3: Dark highlights
            Color.web("#800000"), // Dark Red
            Color.web("#808000"), // Dark Yellow
            Color.web("#808080"), // Dark Gray
            Color.web("#C0C0C0"), // Light Gray
            Color.web("#000000") // Black
    };

    private String currentTypingStyle = "";

    private static final double MAX_CONTENT_HEIGHT = PAGE_HEIGHT - (MARGIN * 2);

    private VBox documentBackground;
    private ScrollPane scrollPane;

    // Helper method to get the current active editor
    private CustomEditor getCurrentEditor() {
        if (editors.isEmpty()) {
            return null;
        }
        return editors.get(currentEditorIndex);
    }

    private void checkPageOverflow() {
        // Layout güncellendikten SONRA ölçüm yapması için runLater kullanıyoruz
        javafx.application.Platform.runLater(() -> {
            CustomEditor currentEditor = getCurrentEditor();
            if (currentEditor == null)
                return;

            double contentHeight = currentEditor.getTotalHeightEstimate();

            // Debug için konsola yüksekliği yazdıralım
            System.out.println("Yükseklik: " + contentHeight + " / Sınır: " + MAX_CONTENT_HEIGHT);

            if (contentHeight > MAX_CONTENT_HEIGHT) {
                System.out.println("Taşma gerçekleşti! Yeni sayfa açılıyor...");
                addNewPage();
            }
        });
    }

    private void addNewPage() {
        // Make the current (overflowing) editor read-only
        CustomEditor oldEditor = getCurrentEditor();
        if (oldEditor != null) {
            oldEditor.setEditable(false); // Eski sayfayı read-only yap
        }

        CustomEditor newEditor = new CustomEditor();
        newEditor.setWrapText(true);
        newEditor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        // Add to editors list
        editors.add(newEditor);
        currentEditorIndex = editors.size() - 1;

        setupEditorListeners(newEditor);

        int pageNumber = editors.size();
        VBox newPage = createPage(newEditor, pageNumber);
        documentBackground.getChildren().add(newPage);

        // Forcefully set focus to new editor
        javafx.application.Platform.runLater(() -> {
            newEditor.requestFocus();
            newEditor.moveTo(0); // Cursor'u başa al

            // Scroll to the new page automatically
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0); // Scroll to bottom
            }
        });
    }

    private void setupEditorListeners(CustomEditor targetEditor) {
        targetEditor.textProperty().addListener((obs, oldText, newText) -> {
            isDirty = true;
            updateWordCount();
            checkPageOverflow();

            // Auto-scroll to keep cursor visible
            javafx.application.Platform.runLater(() -> {
                targetEditor.requestFollowCaret();
            });
        });

        // Handle Enter key for automatic bullet/numbered list continuation
        targetEditor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                int currentParagraph = targetEditor.getCurrentParagraph();
                int paraStart = targetEditor.getAbsolutePosition(currentParagraph, 0);
                int paraEnd = paraStart + targetEditor.getParagraphLength(currentParagraph);
                String currentText = targetEditor.getText(paraStart, paraEnd).trim();

                // Check if current line is a bullet list
                if (currentText.startsWith("• ")) {
                    e.consume();
                    // If the bullet is empty (just "• "), remove it and exit list mode
                    if (currentText.equals("•") || currentText.equals("• ")) {
                        targetEditor.replaceText(paraStart, paraEnd, "");
                        bulletListBtn.setSelected(false);
                        targetEditor.setParagraphStyle(currentParagraph, "");
                    } else {
                        // Add new line with bullet
                        targetEditor.insertText(targetEditor.getCaretPosition(), "\n• ");
                        // Apply padding to the new paragraph
                        javafx.application.Platform.runLater(() -> {
                            int newParagraph = targetEditor.getCurrentParagraph();
                            targetEditor.setParagraphStyle(newParagraph, "-fx-padding: 0 0 0 20;");
                        });
                    }
                }
                // Check if current line is a numbered list
                else if (currentText.matches("^\\d+\\.\\s.*")) {
                    e.consume();
                    // Extract the number
                    String numPart = currentText.split("\\.")[0];
                    int currentNum = Integer.parseInt(numPart);

                    // If the numbered item is empty (just "1. "), remove it and exit list mode
                    if (currentText.matches("^\\d+\\.\\s*$")) {
                        targetEditor.replaceText(paraStart, paraEnd, "");
                        numberedListBtn.setSelected(false);
                        targetEditor.setParagraphStyle(currentParagraph, "");
                    } else {
                        // Add new line with incremented number
                        int nextNum = currentNum + 1;
                        targetEditor.insertText(targetEditor.getCaretPosition(), "\n" + nextNum + ". ");
                        // Apply padding to the new paragraph
                        javafx.application.Platform.runLater(() -> {
                            int newParagraph = targetEditor.getCurrentParagraph();
                            targetEditor.setParagraphStyle(newParagraph, "-fx-padding: 0 0 0 20;");
                        });
                    }
                }
            }
        });

        targetEditor.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String character = e.getCharacter();

            if (!character.isEmpty() && Character.isLetter(character.charAt(0))
                    && character.equals(character.toLowerCase())) {
                IndexRange selection = targetEditor.getSelection();
                int checkPos = selection.getStart();
                boolean shouldCapitilazize = false;
                if (checkPos == 0) {
                    shouldCapitilazize = true;
                } else {
                    String prevChar = targetEditor.getText(checkPos - 1, checkPos);
                    if (prevChar.equals("\n")) {
                        shouldCapitilazize = true;
                    } else if (checkPos >= 2 && prevChar.equals(" ")) {
                        String charBeforeSpace = targetEditor.getText(checkPos - 2, checkPos - 1);
                        // Check for sentence ending punctuation
                        if (charBeforeSpace.equals(".") || charBeforeSpace.equals("?") || charBeforeSpace.equals("!")) {
                            shouldCapitilazize = true;
                        }
                        // Check for bullet list marker (• )
                        else if (charBeforeSpace.equals("•")) {
                            shouldCapitilazize = true;
                        }
                        // Check for numbered list marker (e.g., "1. ", "2. ", etc.)
                        else if (checkPos >= 3 && charBeforeSpace.equals(".")) {
                            String charBeforeDot = targetEditor.getText(checkPos - 3, checkPos - 2);
                            if (Character.isDigit(charBeforeDot.charAt(0))) {
                                shouldCapitilazize = true;
                            }
                        }
                    }
                }
                if (shouldCapitilazize) {
                    e.consume();
                    targetEditor.replaceSelection(character.toUpperCase());
                }
            }
        });

        targetEditor.multiPlainChanges().subscribe(changes -> {
            for (var change : changes) {
                if (!change.getInserted().isEmpty()) {
                    int pos = change.getPosition();
                    int length = change.getInserted().length();
                    targetEditor.setStyle(pos, pos + length, currentTypingStyle);
                }
            }
        });
    }

    @Override
    public void start(Stage stage) {
        documentBackground = new VBox(30);
        // Create the first rich text editor
        CustomEditor firstEditor = new CustomEditor();
        firstEditor.setWrapText(true);
        firstEditor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        // Add to editors list
        editors.add(firstEditor);
        currentEditorIndex = 0;

        documentBackground.setStyle("-fx-background-color:#505050;");
        documentBackground.setAlignment(Pos.TOP_CENTER);
        documentBackground.setPadding(new Insets(30));

        VBox firstPage = createPage(firstEditor, 1);
        documentBackground.getChildren().add(firstPage);
        // Layout
        BorderPane root = new BorderPane();

        // Menu Bar
        MenuBar menuBar = createMenuBar(stage);

        // Toolbar
        ToolBar toolBar = createToolBar(stage);

        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Editor with scroll
        scrollPane = new ScrollPane(documentBackground);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background: #505050; -fx-border-color:transparent;");
        root.setCenter(scrollPane);
        // Status Bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        // Scene
        Scene scene = new Scene(root, 1000, 700);

        // Load CSS
        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        scrollPane.getStyleClass().add("scroll-pane");
        // Keyboard shortcuts
        setupKeyboardShortcuts(scene, stage);

        // Başlangıç stilini ayarla
        currentTypingStyle = buildStyleString();

        setupEditorListeners(firstEditor);
        stage.setTitle("Burak's Word Processor");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (!confirmDiscard(stage)) {
                e.consume();
            }
        });
        stage.show();

        getCurrentEditor().requestFocus();
    }

    private void printDocument(Stage stage) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError("Print Error", "No printer found. Please install a printer or use 'Save as PDF' option.");
            return;
        }
        if (job.showPrintDialog(stage)) {
            boolean success = job.printPage(getCurrentEditor());
            if (success) {
                job.endJob();
            }
        }
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        // File Menu
        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newItem.setOnAction(e -> newDocument(stage));

        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> openFile(stage));

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> saveOrSaveAs(stage));

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> saveFile(stage));

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            if (confirmDiscard(stage)) {
                stage.close();
            }
        });

        MenuItem printItem = new MenuItem("Print...");
        printItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
        printItem.setOnAction(e -> printDocument(stage));

        MenuItem exportPdfItem = new MenuItem("Export to PDF...");
        exportPdfItem.setOnAction(e -> exportToPdf(stage));

        fileMenu.getItems().addAll(newItem, openItem, separator, saveItem, saveAsItem, printItem, exportPdfItem,
                new SeparatorMenuItem(),
                exitItem);

        // Edit Menu
        Menu editMenu = new Menu("Edit");

        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> getCurrentEditor().undo());

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> getCurrentEditor().redo());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> getCurrentEditor().cut());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> getCurrentEditor().copy());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> getCurrentEditor().paste());

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> getCurrentEditor().selectAll());

        MenuItem findItem = new MenuItem("Find...");
        findItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        findItem.setOnAction(e -> showFindDialog(stage));

        MenuItem replaceItem = new MenuItem("Replace...");
        replaceItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        replaceItem.setOnAction(e -> showReplaceDialog(stage));

        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(), cutItem, copyItem, pasteItem,
                new SeparatorMenuItem(), selectAllItem, findItem, replaceItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, createInsertMenu(stage), helpMenu);
        return menuBar;
    }

    private Menu createInsertMenu(Stage stage) {
        Menu insertMenu = new Menu("Insert");

        MenuItem imageItem = new MenuItem("Image...");
        imageItem.setOnAction(e -> insertImage(stage));

        MenuItem tableItem = new MenuItem("Table...");
        tableItem.setOnAction(e -> insertTable(stage));

        MenuItem headerItem = new MenuItem("Header...");
        headerItem.setOnAction(e -> showHeaderDialog(stage));

        MenuItem footerItem = new MenuItem("Footer...");
        footerItem.setOnAction(e -> showFooterDialog(stage));

        MenuItem pageNumberItem = new MenuItem("Page Numbers...");
        pageNumberItem.setOnAction(e -> showPageNumberDialog(stage));

        insertMenu.getItems().addAll(imageItem, tableItem, new SeparatorMenuItem(),
                headerItem, footerItem, pageNumberItem);
        return insertMenu;
    }

    private ToolBar createToolBar(Stage stage) {
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("tool-bar");

        textColorPicker = new ColorPicker(Color.BLACK);
        highlightPicker = new ColorPicker(Color.TRANSPARENT);

        // Bold button
        boldBtn = new ToggleButton();
        boldBtn.setGraphic(createIcon(
                "M15.6 10.79c.97-.67 1.65-1.77 1.65-2.79 0-2.26-1.75-4-4-4H7v14h7.04c2.09 0 3.71-1.7 3.71-3.79 0-1.52-.86-2.82-2.15-3.42zM10.21 6.27h2.25c.95 0 1.72.77 1.72 1.72s-.77 1.72-1.72 1.72h-2.25V6.27zm2.85 9.46h-2.85v-3.46h2.85c1.02 0 1.85.83 1.85 1.85s-.83 1.61-1.85 1.61z"));
        boldBtn.setTooltip(new Tooltip("Bold (Ctrl+B)"));
        boldBtn.setOnAction(e -> applyStyle());

        // Italic button
        italicBtn = new ToggleButton();
        italicBtn.setGraphic(createIcon("M10 4v3h2.21l-3.42 8H6v3h8v-3h-2.21l3.42-8H18V4z"));
        italicBtn.setTooltip(new Tooltip("Italic (Ctrl+I)"));
        italicBtn.setOnAction(e -> applyStyle());

        // Underline button
        underlineBtn = new ToggleButton();
        underlineBtn.setGraphic(createIcon(
                "M12 17c3.31 0 6-2.69 6-6V3h-2.5v8c0 1.93-1.57 3.5-3.5 3.5S8.5 12.93 8.5 11V3H6v8c0 3.31 2.69 6 6 6zm-7 2v2h14v-2H5z"));
        underlineBtn.setTooltip(new Tooltip("Underline (Ctrl+U)"));
        underlineBtn.setOnAction(e -> applyStyle());

        // Font family
        fontFamilyCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Segoe UI", "Arial", "Times New Roman", "Calibri", "Verdana", "Georgia", "Courier New",
                "Comic Sans MS"));
        fontFamilyCombo.setValue("Segoe UI");
        fontFamilyCombo.setTooltip(new Tooltip("Font Family"));
        fontFamilyCombo.setOnAction(e -> applyStyle());

        // Font size
        fontSizeCombo = new ComboBox<>(FXCollections.observableArrayList(
                8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72));
        fontSizeCombo.setValue(14);
        fontSizeCombo.setTooltip(new Tooltip("Font Size"));
        fontSizeCombo.setOnAction(e -> applyStyle());

        // Strikethrough
        strikeBtn = new ToggleButton();
        strikeBtn.setGraphic(createIcon("M10 19h4v-3h-4v3zM5 4v3h5v3h4V7h5V4H5zM3 14h18v-2H3v2z"));
        strikeBtn.setTooltip(new Tooltip("Strikethrough"));
        strikeBtn.setOnAction(e -> applyStyle());

        // Superscript
        superBtn = new ToggleButton();
        superBtn.setGraphic(createIcon(
                "M19 9h2V4h-3v2h1v3zM11 6h5v3h-5zM4 6h5v3H4zm7 7h5v3h-5zm-7 0h5v3H4zm7 7h5v3h-5zm-7 0h5v3H4z"));
        superBtn.setTooltip(new Tooltip("Superscript"));
        superBtn.setOnAction(e -> {
            if (superBtn.isSelected())
                subBtn.setSelected(false);
            applyStyle();
        });

        // Subscript
        subBtn = new ToggleButton();
        subBtn.setGraphic(createIcon(
                "M19 14h2v5h-3v-2h1v-3zM11 6h5v3h-5zM4 6h5v3H4zm7 7h5v3h-5zm-7 0h5v3H4zm7 7h5v3h-5zm-7 0h5v3H4z"));
        subBtn.setTooltip(new Tooltip("Subscript"));
        subBtn.setOnAction(e -> {
            if (subBtn.isSelected())
                superBtn.setSelected(false);
            applyStyle();
        });
        // 1. Yazı Rengi Butonu (A harfi ikonu)
        MenuButton wordColorBtn = createWordColorButton(
                "M0 20h24v4H0z M11 3L5.5 17h2.25l1.12-3h6.25l1.12 3h2.25L13 3h-2zm-1.38 11L12 5.67 14.38 14H9.62z", // A
                                                                                                                    // harfi
                                                                                                                    // path
                Color.RED,
                false // Highlight değil
        );
        wordColorBtn.setTooltip(new Tooltip("Yazı Rengi"));

        // 2. Highlight Butonu (Kalem ikonu)
        MenuButton wordHighlightBtn = createWordColorButton(
                "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z", // Kalem ucu path
                Color.YELLOW,
                true // Evet bu highlight
        );
        wordHighlightBtn.setTooltip(new Tooltip("Metin Vurgu Rengi"));

        alignLeftBtn = new ToggleButton();
        alignLeftBtn
                .setGraphic(createIcon("M15 15H3v2h12v-2zm0-8H3v2h12V7zM3 13h18v-2H3v2zm0 8h18v-2H3v2zM3 3v2h18V3H3z"));
        alignLeftBtn.setTooltip(new Tooltip("Align Left"));
        alignLeftBtn.setOnAction(e -> applyAlignment("LEFT"));

        alignCenterBtn = new ToggleButton();
        alignCenterBtn
                .setGraphic(createIcon("M3 21h18v-2H3v2zm4-4h10v-2H7v2zm-4-4h18v-2H3v2zm4-4h10V7H7v2zM3 3v2h18V3H3z"));
        alignCenterBtn.setTooltip(new Tooltip("Align Center"));
        alignCenterBtn.setOnAction(e -> applyAlignment("CENTER"));

        alignRightBtn = new ToggleButton();
        alignRightBtn
                .setGraphic(createIcon("M3 21h18v-2H3v2zm6-4h12v-2H9v2zm-6-4h18v-2H3v2zm6-4h12V7H9v2zM3 3v2h18V3H3z"));
        alignRightBtn.setTooltip(new Tooltip("Align Right"));
        alignRightBtn.setOnAction(e -> applyAlignment("RIGHT"));

        alignJustifyBtn = new ToggleButton();
        alignJustifyBtn
                .setGraphic(createIcon("M3 21h18v-2H3v2zM3 17h18v-2H3v2zm0-4h18v-2H3v2zm0-4h18V7H3v2zm0-6v2h18V3H3z"));
        alignJustifyBtn.setTooltip(new Tooltip("Align Justify"));
        alignJustifyBtn.setOnAction(e -> applyAlignment("JUSTIFY"));

        // Bullet list button
        bulletListBtn = new ToggleButton();
        bulletListBtn.setGraphic(createIcon(
                "M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.68-1.5 1.5s.68 1.5 1.5 1.5 1.5-.68 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z"));
        bulletListBtn.setTooltip(new Tooltip("Bullet List"));
        bulletListBtn.setOnAction(e -> toggleBulletList());

        // Numbered list button
        numberedListBtn = new ToggleButton();
        numberedListBtn.setGraphic(createIcon(
                "M2 17h2v.5H3v1h1v.5H2v1h3v-4H2v1zm1-9h1V4H2v1h1v3zm-1 3h1.8L2 13.1v.9h3v-1H3.2L5 10.9V10H2v1zm5-6v2h14V5H7zm0 14h14v-2H7v2zm0-6h14v-2H7v2z"));
        numberedListBtn.setTooltip(new Tooltip("Numbered List"));
        numberedListBtn.setOnAction(e -> toggleNumberedList());

        // Image button
        insertImageBtn = new Button();
        insertImageBtn.setGraphic(createIcon(
                "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"));
        insertImageBtn.setTooltip(new Tooltip("Insert Image"));
        insertImageBtn.setOnAction(e -> insertImage(stage));

        // Table button
        insertTableBtn = new Button();
        insertTableBtn.setGraphic(createIcon(
                "M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM8 20H4v-4h4v4zm0-6H4v-4h4v4zm0-6H4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4z"));
        insertTableBtn.setTooltip(new Tooltip("Insert Table"));
        // Artık picker'ı çağırıyoruz ve butonu (tableButton) parametre veriyoruz
        insertTableBtn.setOnAction(e -> ContentInserter.openTablePicker(getCurrentEditor(), insertTableBtn));

        javafx.scene.control.ToggleButton themeToggle = new javafx.scene.control.ToggleButton("🌙");
        themeToggle.setStyle("-fx-font-size: 14px; -fx-min-width: 40px;");

        themeToggle.setOnAction(e -> {
            javafx.scene.Scene scene = themeToggle.getScene(); // Sahneyi al

            if (themeToggle.isSelected()) {
                // 🌑 Karanlık Modu Aç
                themeToggle.setText("☀️");
                // CSS dosyasını sahneye ekle
                scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            } else {
                // ☀️ Aydınlık Moda Dön
                themeToggle.setText("🌙");
                // CSS dosyasını sahneden çıkar
                scene.getStylesheets().remove(getClass().getResource("/dark-theme.css").toExternalForm());
            }
        });

        Button growBtn = new Button();
        Button shrinkBtn = new Button();
        try {
            shrinkBtn.setGraphic(createImageView("/icons/shrink.png", 20));
            shrinkBtn.setTooltip(new Tooltip("Yazı Boyutunu Küçült"));
            shrinkBtn.setOnAction(e -> {
                Integer currentSize = fontSizeCombo.getValue();
                if (currentSize != null && currentSize > 8) {
                    fontSizeCombo.setValue(currentSize - 2);
                    applyStyle();
                }
            });

            growBtn.setGraphic(createImageView("/icons/grow.png", 20));
            growBtn.setTooltip(new Tooltip("Yazı Boyutunu Büyüt"));
            growBtn.setOnAction(e -> {
                Integer currentSize = fontSizeCombo.getValue();
                if (currentSize != null && currentSize < 72) {
                    fontSizeCombo.setValue(currentSize + 2);
                    applyStyle();
                }
            });

        } catch (Exception e) {
            growBtn.setText("A+");
            growBtn.setTooltip(new Tooltip("Yazı Boyutunu Büyüt"));
            growBtn.setOnAction(ev -> {
                Integer currentSize = fontSizeCombo.getValue();
                if (currentSize != null && currentSize < 72) {
                    fontSizeCombo.setValue(currentSize + 2);
                    applyStyle();
                }
            });

            shrinkBtn.setText("A-");
            shrinkBtn.setTooltip(new Tooltip("Yazı Boyutunu Küçült"));
            shrinkBtn.setOnAction(ev -> {
                Integer currentSize = fontSizeCombo.getValue();
                if (currentSize != null && currentSize > 8) {
                    fontSizeCombo.setValue(currentSize - 2);
                    applyStyle();
                }
            });
        }

        toolBar.getItems().addAll(
                themeToggle, // <--- BAK BURAYA, EN BAŞA KOYDUK
                new Separator(), // Araya şık bir çizgi çektik

                // Diğerleri sırayla devam ediyor...
                boldBtn, italicBtn, underlineBtn, strikeBtn,
                new Separator(),
                superBtn, subBtn,
                new Separator(),
                fontFamilyCombo, fontSizeCombo,
                new Separator(),
                growBtn, shrinkBtn,
                new Separator(),
                wordColorBtn, wordHighlightBtn,
                new Separator(),
                insertImageBtn, insertTableBtn,
                new Separator(),
                alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn,
                new Separator(),
                bulletListBtn, numberedListBtn

        );
        return toolBar;
    }

    private MenuButton createWordColorButton(String iconPath, Color defaultColor, boolean isHighlight) {
        MenuButton menuBtn = new MenuButton();
        menuBtn.getStyleClass().add("word-color-button"); // CSS için sınıf

        // --- 1. İKON TASARIMI (Üstte ikon, altta renk çubuğu) ---
        VBox iconContainer = new VBox(0); // Arada boşluk yok
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);

        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("icon");

        // Altındaki renk çubuğu (Dikdörtgen)
        javafx.scene.shape.Rectangle colorBar = new javafx.scene.shape.Rectangle(20, 4);
        colorBar.setFill(defaultColor); // Varsayılan renk

        iconContainer.getChildren().addAll(icon, colorBar);
        menuBtn.setGraphic(iconContainer);

        // --- 2. RENK IZGARASI (POPUP MENÜ) ---
        CustomMenuItem customMenuItem = new CustomMenuItem();
        customMenuItem.setHideOnClick(false); // Renk seçince biz kapatacağız

        VBox popupContent = new VBox(5);
        popupContent.setPadding(new Insets(10));
        popupContent.setStyle("-fx-background-color: white; -fx-border-color: #e1dfdd;");

        // Başlık (Theme Colors vb.)
        Label header = new Label(isHighlight ? "Vurgu Renkleri" : "Yazı Renkleri");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #666;");
        popupContent.getChildren().add(header);

        // Otomatik / Renk Yok Seçeneği
        Button autoBtn = new Button(isHighlight ? "Renk Yok" : "Otomatik");
        autoBtn.setMaxWidth(Double.MAX_VALUE);
        autoBtn.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
        autoBtn.setOnAction(e -> {
            Color c = isHighlight ? Color.TRANSPARENT : Color.BLACK;
            colorBar.setFill(isHighlight ? Color.WHITE : Color.BLACK); // Bar rengini güncelle

            // Asıl işlemi yap
            if (isHighlight) {
                // Highlight değişkenini güncelle ve uygula
                highlightPicker.setValue(Color.TRANSPARENT);
            } else {
                textColorPicker.setValue(Color.BLACK);
            }
            applyStyle(); // Stili uygula
            menuBtn.hide(); // Menüyü kapat
        });
        popupContent.getChildren().add(autoBtn);

        // Renk Izgarası (Grid)
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(3);
        colorGrid.setVgap(3);

        // Doğru renk paletini seç (Text için WORD_TEXT_COLORS, Highlight için
        // WORD_HIGHLIGHT_COLORS)
        Color[] colorPalette = isHighlight ? WORD_HIGHLIGHT_COLORS : WORD_TEXT_COLORS;

        int col = 0;
        int row = 0;
        for (Color color : colorPalette) {
            // Renk Kutusu
            Button colorBox = new Button();
            colorBox.setPrefSize(20, 20);
            colorBox.setStyle(
                    "-fx-background-color: " + toHexString(color) + "; -fx-border-color: #e1dfdd; -fx-cursor: hand;");

            colorBox.setOnAction(e -> {
                colorBar.setFill(color); // Butonun altındaki çizgiyi boya

                // Gizli ColorPicker'ları güncelle (Mantık bozulmasın diye)
                if (isHighlight) {
                    highlightPicker.setValue(color);
                } else {
                    textColorPicker.setValue(color);
                }
                getCurrentEditor().requestFocus();
                applyStyle(); // Yazıya uygula
                menuBtn.hide();
            });

            // Hover Efekti (Kenarlık parlasın)
            colorBox.setOnMouseEntered(e -> colorBox.setStyle("-fx-background-color: " + toHexString(color)
                    + "; -fx-border-color: #f2994a; -fx-border-width: 2px;"));
            colorBox.setOnMouseExited(e -> colorBox
                    .setStyle("-fx-background-color: " + toHexString(color) + "; -fx-border-color: #e1dfdd;"));

            colorGrid.add(colorBox, col, row);

            col++;
            if (col > 4) { // Her satırda 5 renk olsun
                col = 0;
                row++;
            }
        }
        popupContent.getChildren().add(colorGrid);

        customMenuItem.setContent(popupContent);
        menuBtn.getItems().add(customMenuItem);

        return menuBtn;
    }

    private VBox createPage(CustomEditor contentEditor, int pageNumber) {
        VBox page = new VBox();

        page.setPrefWidth(PAGE_WIDTH);
        page.setPrefHeight(PAGE_HEIGHT);
        page.setMinHeight(PAGE_HEIGHT);
        page.setMaxHeight(PAGE_HEIGHT);
        page.setStyle("-fx-background-color: white;");
        page.setPadding(new Insets(MARGIN));

        // Create header editor
        CustomEditor headerEditor = CustomEditor.createHeaderFooterEditor(false);
        String headerContent = headerFooterManager.getCompleteHeaderForPage(pageNumber);
        headerEditor.replaceText(0, 0, headerContent);
        headerEditors.add(headerEditor);

        // Create footer editor
        CustomEditor footerEditor = CustomEditor.createHeaderFooterEditor(false);
        String footerContent = headerFooterManager.getCompleteFooterForPage(pageNumber);
        footerEditor.replaceText(0, 0, footerContent);
        footerEditors.add(footerEditor);

        // Calculate adjusted height for content editor
        double headerFooterHeight = 80; // 40px each for header and footer
        double contentHeight = PAGE_HEIGHT - (MARGIN * 2) - headerFooterHeight;

        if (contentEditor != null) {
            contentEditor.setPrefWidth(PAGE_WIDTH - (MARGIN * 2));
            contentEditor.setPrefHeight(contentHeight);
        }

        // Add all components: header, content, footer
        page.getChildren().addAll(headerEditor, contentEditor, footerEditor);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetX(0);
        shadow.setOffsetY(5);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        page.setEffect(shadow);
        return page;
    }

    private void showFindDialog(Stage stage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Find");
        dialog.setHeaderText("Enter text to find:");
        TextField searchField = new TextField();
        searchField.setPromptText("Search text ...");
        dialog.getDialogPane().setContent(searchField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return searchField.getText();
            }
            return null;
        });
        dialog.showAndWait().ifPresent(searchText -> {
            findText(searchText);
        });

    }

    private void findText(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        String content = getCurrentEditor().getText();
        int index = content.indexOf(searchText);
        if (index >= 0) {
            getCurrentEditor().selectRange(index, index + searchText.length());
            getCurrentEditor().requestFollowCaret();

        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Find");
            alert.setHeaderText(null);
            alert.setContentText("Text not found: " + searchText);
            alert.showAndWait();
        }

    }

    private int lastSearchIndex = 0;

    private void showReplaceDialog(Stage stage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Find and Replace");
        dialog.setHeaderText(null);
        dialog.initOwner(stage);

        // Create layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Find field
        HBox findRow = new HBox(10);
        findRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label findLabel = new Label("Find:");
        findLabel.setPrefWidth(70);
        TextField findField = new TextField();
        findField.setPrefWidth(250);
        findRow.getChildren().addAll(findLabel, findField);

        // Replace field
        HBox replaceRow = new HBox(10);
        replaceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label replaceLabel = new Label("Replace:");
        replaceLabel.setPrefWidth(70);
        TextField replaceField = new TextField();
        replaceField.setPrefWidth(250);
        replaceRow.getChildren().addAll(replaceLabel, replaceField);

        // Buttons
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button findNextBtn = new Button("Find Next");
        Button replaceBtn = new Button("Replace");
        Button replaceAllBtn = new Button("Replace All");
        Button closeBtn = new Button("Close");

        findNextBtn.setOnAction(e -> {
            String searchText = findField.getText();
            if (!searchText.isEmpty()) {
                findNextText(searchText);
            }
        });

        replaceBtn.setOnAction(e -> {
            String searchText = findField.getText();
            String replaceText = replaceField.getText();
            if (!searchText.isEmpty()) {
                replaceCurrentSelection(searchText, replaceText);
            }
        });

        replaceAllBtn.setOnAction(e -> {
            String searchText = findField.getText();
            String replaceText = replaceField.getText();
            if (!searchText.isEmpty()) {
                replaceAllText(searchText, replaceText);
            }
        });

        closeBtn.setOnAction(e -> {
            dialog.close();
            lastSearchIndex = 0;
        });

        buttonRow.getChildren().addAll(findNextBtn, replaceBtn, replaceAllBtn, closeBtn);

        content.getChildren().addAll(findRow, replaceRow, buttonRow);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.setOnCloseRequest(e -> lastSearchIndex = 0);
        dialog.show();
    }

    private void findNextText(String searchText) {
        String content = getCurrentEditor().getText();
        int index = content.indexOf(searchText, lastSearchIndex);

        if (index >= 0) {
            getCurrentEditor().selectRange(index, index + searchText.length());
            getCurrentEditor().requestFollowCaret();
            lastSearchIndex = index + 1;
        } else if (lastSearchIndex > 0) {
            // Wrap around to beginning
            lastSearchIndex = 0;
            index = content.indexOf(searchText, 0);
            if (index >= 0) {
                getCurrentEditor().selectRange(index, index + searchText.length());
                getCurrentEditor().requestFollowCaret();
                lastSearchIndex = index + 1;
            } else {
                showNotFound(searchText);
            }
        } else {
            showNotFound(searchText);
        }
    }

    private void replaceCurrentSelection(String searchText, String replaceText) {
        String selectedText = getCurrentEditor().getSelectedText();
        if (selectedText.equals(searchText)) {
            getCurrentEditor().replaceSelection(replaceText);
            findNextText(searchText);
        } else {
            findNextText(searchText);
        }
    }

    private void replaceAllText(String searchText, String replaceText) {
        String content = getCurrentEditor().getText();
        int count = 0;
        int index = 0;

        while ((index = content.indexOf(searchText, index)) >= 0) {
            count++;
            index += searchText.length();
        }

        if (count > 0) {
            String newContent = content.replace(searchText, replaceText);
            getCurrentEditor().replaceText(newContent);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Replace All");
            alert.setHeaderText(null);
            alert.setContentText("Replaced " + count + " occurrence(s).");
            alert.showAndWait();
        } else {
            showNotFound(searchText);
        }
    }

    private void showNotFound(String searchText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Find");
        alert.setHeaderText(null);
        alert.setContentText("Text not found: " + searchText);
        alert.showAndWait();
    }

    private void applyAlignment(String alignment) {
        // Toggle button states
        alignLeftBtn.setSelected(alignment.equals("LEFT"));
        alignCenterBtn.setSelected(alignment.equals("CENTER"));
        alignRightBtn.setSelected(alignment.equals("RIGHT"));
        alignJustifyBtn.setSelected(alignment.equals("JUSTIFY"));

        // Get the current paragraph index
        int currentParagraph = getCurrentEditor().getCurrentParagraph();

        // Build the CSS style for paragraph alignment
        String alignStyle;
        switch (alignment) {
            case "CENTER":
                alignStyle = "-fx-text-alignment: center;";
                break;
            case "RIGHT":
                alignStyle = "-fx-text-alignment: right;";
                break;
            case "JUSTIFY":
                alignStyle = "-fx-text-alignment: justify;";
                break;
            case "LEFT":
            default:
                alignStyle = "-fx-text-alignment: left;";
                break;
        }

        // Apply paragraph style to current paragraph
        getCurrentEditor().setParagraphStyle(currentParagraph, alignStyle);
    }

    private void toggleBulletList() {
        CustomEditor editor = getCurrentEditor();
        int currentParagraph = editor.getCurrentParagraph();

        // Get current paragraph text
        int paraStart = editor.getAbsolutePosition(currentParagraph, 0);
        int paraEnd = paraStart + editor.getParagraphLength(currentParagraph);
        String currentText = editor.getText(paraStart, paraEnd).trim();

        // Check if already a bullet list
        if (currentText.startsWith("• ")) {
            // Remove bullet
            String newText = currentText.substring(2);
            editor.replaceText(paraStart, paraEnd, newText + "\n");
            editor.setParagraphStyle(currentParagraph, "");
            bulletListBtn.setSelected(false);
        } else {
            // Remove numbered list if present
            if (currentText.matches("^\\d+\\.\\s.*")) {
                currentText = currentText.replaceFirst("^\\d+\\.\\s+", "");
                numberedListBtn.setSelected(false);
            }
            // Add bullet
            String newText = "• " + currentText;
            editor.replaceText(paraStart, paraEnd, newText + "\n");
            editor.setParagraphStyle(currentParagraph, "-fx-padding: 0 0 0 20;");
            bulletListBtn.setSelected(true);
        }

        editor.requestFocus();
    }

    private void toggleNumberedList() {
        CustomEditor editor = getCurrentEditor();
        int currentParagraph = editor.getCurrentParagraph();

        // Get current paragraph text
        int paraStart = editor.getAbsolutePosition(currentParagraph, 0);
        int paraEnd = paraStart + editor.getParagraphLength(currentParagraph);
        String currentText = editor.getText(paraStart, paraEnd).trim();

        // Check if already a numbered list
        if (currentText.matches("^\\d+\\.\\s.*")) {
            // Remove numbering
            String newText = currentText.replaceFirst("^\\d+\\.\\s+", "");
            editor.replaceText(paraStart, paraEnd, newText + "\n");
            editor.setParagraphStyle(currentParagraph, "");
            numberedListBtn.setSelected(false);
        } else {
            // Remove bullet list if present
            if (currentText.startsWith("• ")) {
                currentText = currentText.substring(2);
                bulletListBtn.setSelected(false);
            }
            // Add numbering (simplified - all items use "1.")
            String newText = "1. " + currentText;
            editor.replaceText(paraStart, paraEnd, newText + "\n");
            editor.setParagraphStyle(currentParagraph, "-fx-padding: 0 0 0 20;");
            numberedListBtn.setSelected(true);
        }

        editor.requestFocus();
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(30);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5, 10, 5, 10));

        wordCountLabel = new Label("Words: 0");
        charCountLabel = new Label("Characters: 0");

        statusBar.getChildren().addAll(wordCountLabel, charCountLabel);
        return statusBar;
    }

    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case B:
                        boldBtn.setSelected(!boldBtn.isSelected());
                        applyStyle();
                        event.consume();
                        break;
                    case I:
                        italicBtn.setSelected(!italicBtn.isSelected());
                        applyStyle();
                        event.consume();
                        break;
                    case U:
                        underlineBtn.setSelected(!underlineBtn.isSelected());
                        applyStyle();
                        event.consume();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void applyStyle() {
        String style = buildStyleString();
        IndexRange selection = getCurrentEditor().getSelection();

        if (selection.getLength() > 0) {
            getCurrentEditor().setStyle(selection.getStart(), selection.getEnd(), style);
        }

        // Her durumda gelecekteki yazılar için stili güncelle
        currentTypingStyle = style;

        // İmleç rengini de değiştir (Word Web gibi)
        Color caretColor = textColorPicker.getValue();
        getCurrentEditor().setStyle(String.format("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; " +
                "caret-color: %s;", toHexString(caretColor)));

        getCurrentEditor().requestFocus();
    }

    private String buildStyleString() {
        StringBuilder style = new StringBuilder();

        // Font family
        style.append("-fx-font-family: '").append(fontFamilyCombo.getValue()).append("'; ");

        // Font size
        style.append("-fx-font-size: ").append(fontSizeCombo.getValue()).append("px; ");

        // Bold
        if (boldBtn.isSelected()) {
            style.append("-fx-font-weight: bold; ");
        }

        // Italic
        if (italicBtn.isSelected()) {
            style.append("-fx-font-style: italic; ");
        }

        // Underline
        if (underlineBtn.isSelected()) {
            style.append("-fx-underline: true; ");
        }

        // Strikethrough
        if (strikeBtn.isSelected()) {
            style.append("-fx-strikethrough: true; ");
        }

        // Super/Subscript
        if (superBtn.isSelected()) {
            style.append("-fx-font-size: 10px; -fx-translate-y: -4px; ");
        } else if (subBtn.isSelected()) {
            style.append("-fx-font-size: 10px; -fx-translate-y: 2px; ");
        }

        // Color
        Color color = textColorPicker.getValue();
        String hexColor = toHexString(color);
        style.append("-fx-fill: ").append(hexColor).append("; ");
        style.append("-fx-text-fill: ").append(hexColor).append("; ");

        // Highlight
        Color highlight = highlightPicker.getValue();
        if (highlight != null && !highlight.equals(Color.TRANSPARENT)) {
            style.append("-rtfx-background-color: ").append(toRgbString(highlight)).append("; ");
        }

        return style.toString();
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d, %d, %d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private ImageView createImageView(String path, double size) {
        Image image = new Image(getClass().getResourceAsStream(path));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void updateWordCount() {
        // Aggregate text from all editors
        StringBuilder allText = new StringBuilder();
        for (CustomEditor ed : editors) {
            if (ed != null) {
                allText.append(ed.getText());
            }
        }
        String text = allText.toString();
        int chars = text.length();
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;

        wordCountLabel.setText("Words: " + words);
        charCountLabel.setText("Characters: " + chars);
    }

    private void newDocument(Stage stage) {
        if (!confirmDiscard(stage))
            return;

        // Clear all editors and pages
        editors.clear();
        documentBackground.getChildren().clear();

        // Create a new first editor
        CustomEditor firstEditor = new CustomEditor();
        firstEditor.setWrapText(true);
        firstEditor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        editors.add(firstEditor);
        currentEditorIndex = 0;

        VBox firstPage = createPage(firstEditor, 1);
        documentBackground.getChildren().add(firstPage);

        setupEditorListeners(firstEditor);

        currentFile = null;
        isDirty = false;
        stage.setTitle("Burak's Word Processor");
        updateWordCount();
    }

    private void openFile(Stage stage) {
        if (!confirmDiscard(stage))
            return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Document");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            if (file.getName().endsWith(".docx")) {
                openDocx(file);
                currentFile = file;
                isDirty = false;
                stage.setTitle("Burak's Word Processor - " + file.getName());
                updateWordCount();
            } else {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    getCurrentEditor().replaceText(content.toString());
                    currentFile = file;
                    isDirty = false;
                    stage.setTitle("Burak's Word Processor - " + file.getName());
                    updateWordCount();
                } catch (IOException e) {
                    showError("Error opening file", e.getMessage());
                }
            }
        }
    }

    private void saveOrSaveAs(Stage stage) {
        if (currentFile != null) {
            saveToFile(currentFile, stage);
        } else {
            saveFile(stage);
        }
    }

    private void openDocx(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            XWPFDocument document = new XWPFDocument(fis);
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText());
                content.append("\n");
            }

            getCurrentEditor().replaceText(content.toString());
            document.close();
            fis.close();
        } catch (IOException e) {
            showError("Error opening file", e.getMessage());
        }
    }

    private void saveAsDocx(File file) {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(getCurrentEditor().getText());
            document.write(new FileOutputStream(file));
        } catch (IOException e) {
            showError("Error saving file", e.getMessage());
        }
    }

    private void saveFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Document");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"));

        if (currentFile != null) {
            chooser.setInitialFileName(currentFile.getName());
        }

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            if (file.getName().endsWith(".docx")) {
                saveAsDocx(file);
            } else {
                saveToFile(file, stage);
            }
        }
    }

    private void saveToFile(File file, Stage stage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(getCurrentEditor().getText());
            currentFile = file;
            isDirty = false;
            stage.setTitle("Burak's Word Processor - " + file.getName());
        } catch (IOException e) {
            showError("Error saving file", e.getMessage());
        }
    }

    private boolean confirmDiscard(Stage stage) {
        if (!isDirty)
            return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Do you want to discard them?");

        ButtonType saveBtn = new ButtonType("Save");
        ButtonType discardBtn = new ButtonType("Discard");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

        var result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveBtn) {
                saveOrSaveAs(stage);
                return !isDirty; // Return true if save was successful
            } else if (result.get() == discardBtn) {
                return true;
            }
        }
        return false;
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Burak's Word Processor");
        alert.setContentText("A simple word processor built with JavaFX and RichTextFX.\n\nVersion 1.0");
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void insertImage(Stage stage) {
        ContentInserter.insertImage(getCurrentEditor(), stage);
        isDirty = true;
    }

    private void insertTable(Stage stage) {
        ContentInserter.openTablePicker(getCurrentEditor(), insertTableBtn);
        isDirty = true;
    }

    private void exportToPdf(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        if (currentFile != null) {
            String name = currentFile.getName().substring(0, currentFile.getName().lastIndexOf("."));
            chooser.setInitialFileName(name + ".pdf");
        } else {
            chooser.setInitialFileName("document.pdf");
        }

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // Export all pages instead of just the current one
                PDFExporter.export(editors, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Document has been exported to PDF successfully.");
                alert.showAndWait();
            } catch (Exception e) {
                showError("Export Error", "Could not export to PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private SVGPath createIcon(String content) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        icon.getStyleClass().add("icon");
        return icon;
    }

    private void showHeaderDialog(Stage stage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Header");
        dialog.setHeaderText("Enter header content:");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea headerText = new TextArea();
        headerText.setPrefRowCount(3);
        headerText.setPromptText("Type your header text here...");

        CheckBox differentFirstPage = new CheckBox("Different first page");
        differentFirstPage.setSelected(headerFooterManager.isDifferentFirstPage());

        Label firstPageLabel = new Label("First page header:");
        firstPageLabel.setVisible(differentFirstPage.isSelected());
        TextArea firstPageText = new TextArea();
        firstPageText.setPrefRowCount(2);
        firstPageText.setVisible(differentFirstPage.isSelected());

        differentFirstPage.setOnAction(e -> {
            boolean selected = differentFirstPage.isSelected();
            firstPageLabel.setVisible(selected);
            firstPageText.setVisible(selected);
        });

        // Load existing content
        headerText.setText(headerFooterManager.getDefaultHeaderContent());
        firstPageText.setText(headerFooterManager.getFirstPageHeaderContent());

        content.getChildren().addAll(
                new Label("Default header:"),
                headerText,
                differentFirstPage,
                firstPageLabel,
                firstPageText);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                headerFooterManager.setHeaderContent(headerText.getText(), false);
                if (differentFirstPage.isSelected()) {
                    headerFooterManager.setHeaderContent(firstPageText.getText(), true);
                    headerFooterManager.setDifferentFirstPage(true);
                } else {
                    headerFooterManager.setDifferentFirstPage(false);
                }
                updateAllPageHeadersFooters();
                return headerText.getText();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showFooterDialog(Stage stage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Footer");
        dialog.setHeaderText("Enter footer content:");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea footerText = new TextArea();
        footerText.setPrefRowCount(3);
        footerText.setPromptText("Type your footer text here...");

        CheckBox differentFirstPage = new CheckBox("Different first page");
        differentFirstPage.setSelected(headerFooterManager.isDifferentFirstPage());

        Label firstPageLabel = new Label("First page footer:");
        firstPageLabel.setVisible(differentFirstPage.isSelected());
        TextArea firstPageText = new TextArea();
        firstPageText.setPrefRowCount(2);
        firstPageText.setVisible(differentFirstPage.isSelected());

        differentFirstPage.setOnAction(e -> {
            boolean selected = differentFirstPage.isSelected();
            firstPageLabel.setVisible(selected);
            firstPageText.setVisible(selected);
        });

        // Load existing content
        footerText.setText(headerFooterManager.getDefaultFooterContent());
        firstPageText.setText(headerFooterManager.getFirstPageFooterContent());

        content.getChildren().addAll(
                new Label("Default footer:"),
                footerText,
                differentFirstPage,
                firstPageLabel,
                firstPageText);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                headerFooterManager.setFooterContent(footerText.getText(), false);
                if (differentFirstPage.isSelected()) {
                    headerFooterManager.setFooterContent(firstPageText.getText(), true);
                    headerFooterManager.setDifferentFirstPage(true);
                } else {
                    headerFooterManager.setDifferentFirstPage(false);
                }
                updateAllPageHeadersFooters();
                return footerText.getText();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showPageNumberDialog(Stage stage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Page Numbers");
        dialog.setHeaderText("Configure page numbering:");

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        CheckBox enablePageNumbers = new CheckBox("Show page numbers");
        enablePageNumbers.setSelected(headerFooterManager.isShowPageNumbers());

        // Format selection
        Label formatLabel = new Label("Number format:");
        ComboBox<HeaderFooterManager.PageNumberFormat> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(HeaderFooterManager.PageNumberFormat.values());
        formatCombo.setValue(headerFooterManager.getPageNumberFormat());
        formatCombo.setDisable(!enablePageNumbers.isSelected());

        // Position selection
        Label positionLabel = new Label("Position:");
        ComboBox<HeaderFooterManager.PageNumberPosition> positionCombo = new ComboBox<>();
        positionCombo.getItems().addAll(HeaderFooterManager.PageNumberPosition.values());
        positionCombo.setValue(headerFooterManager.getPageNumberPosition());
        positionCombo.setDisable(!enablePageNumbers.isSelected());

        enablePageNumbers.setOnAction(e -> {
            boolean enabled = enablePageNumbers.isSelected();
            formatCombo.setDisable(!enabled);
            positionCombo.setDisable(!enabled);
        });

        content.getChildren().addAll(
                enablePageNumbers,
                formatLabel,
                formatCombo,
                positionLabel,
                positionCombo);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                headerFooterManager.setShowPageNumbers(enablePageNumbers.isSelected());
                headerFooterManager.setPageNumberFormat(formatCombo.getValue());
                headerFooterManager.setPageNumberPosition(positionCombo.getValue());
                updateAllPageHeadersFooters();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void updateAllPageHeadersFooters() {
        for (int i = 0; i < headerEditors.size(); i++) {
            int pageNumber = i + 1;

            // Update header
            CustomEditor headerEditor = headerEditors.get(i);
            String headerContent = headerFooterManager.getCompleteHeaderForPage(pageNumber);
            headerEditor.clear();
            headerEditor.replaceText(0, 0, headerContent);

            // Update footer
            CustomEditor footerEditor = footerEditors.get(i);
            String footerContent = headerFooterManager.getCompleteFooterForPage(pageNumber);
            footerEditor.clear();
            footerEditor.replaceText(0, 0, footerContent);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
