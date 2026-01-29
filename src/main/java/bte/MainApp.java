package bte;

import javafx.application.Application;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import org.reactfx.util.Either;

import javafx.print.PrinterJob;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.SVGPath;

import java.io.*;
import java.net.URL;


// TODO
public class MainApp extends Application {

    private static final double PAGE_WIDTH = 595;
    private static final double PAGE_HEIGHT = 842;
    private static final double MARGIN = 50;

    private CustomEditor editor;
    private File currentFile;
    private boolean isDirty = false;

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

    private final Color[] WORD_COLORS = {
            Color.web("#000000"), Color.web("#44546A"), Color.web("#5B9BD5"), Color.web("#ED7D31"),
            Color.web("#A5A5A5"),
            Color.web("#FFC000"), Color.web("#4472C4"), Color.web("#70AD47"), Color.web("#FF0000"),
            Color.web("#7030A0"),
            Color.web("#F2F2F2"), Color.web("#D6DCE4"), Color.web("#DDEBF7"), Color.web("#FBE5D6"),
            Color.web("#E7E6E6"),
            Color.web("#FFF2CC"), Color.web("#D9E1F2"), Color.web("#E2EFDA"), Color.web("#FFCCCC"), Color.web("#EAD1DC")
    };

    private String currentTypingStyle = "";

    @Override
    public void start(Stage stage) {
        // Create the rich text editor
        editor = new CustomEditor();
        editor.setWrapText(true);
        editor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        VBox pageContainer = createPageView();
        // Layout
        BorderPane root = new BorderPane();

        // Menu Bar
        MenuBar menuBar = createMenuBar(stage);

        // Toolbar
        ToolBar toolBar = createToolBar(stage);

        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Editor with scroll
        ScrollPane scrollPane = new ScrollPane(pageContainer);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
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

        // Yeni yazılan metne stil uygula - RichTextFX için doğru yaklaşım
        editor.multiPlainChanges()
                .subscribe(changes -> {
                    for (var change : changes) {
                        if (!change.getInserted().isEmpty()) {
                            int pos = change.getPosition();
                            int length = change.getInserted().length();
                            // Platform.runLater kullanmadan direkt uygula
                            editor.setStyle(pos, pos + length, currentTypingStyle);
                        }
                    }
                });

        // Metin değişikliklerini takip et
        editor.textProperty().addListener((obs, oldText, newText) -> {
            isDirty = true;
            updateWordCount();
        });

        stage.setTitle("Burak's Word Processor");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (!confirmDiscard(stage)) {
                e.consume();
            }
        });
        stage.show();

        editor.requestFocus();
    }

    private VBox createPageView() {
        VBox pageContainer = new VBox();
        pageContainer.setPrefWidth(PAGE_WIDTH);
        pageContainer.setPrefHeight(PAGE_HEIGHT);
        pageContainer.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1px;");
        pageContainer.setPadding(new Insets(MARGIN));
        pageContainer.getChildren().add(editor);

        editor.setPrefWidth(PAGE_WIDTH - (MARGIN * 2));
        editor.setPrefHeight(PAGE_HEIGHT - (MARGIN * 2));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(15);
        shadow.setOffsetX(5);
        shadow.setOffsetY(5);
        shadow.setColor(Color.gray(0.4));
        pageContainer.setEffect(shadow);

        return pageContainer;
    }

    private void printDocument(Stage stage) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError("Print Error", "No printer found. Please install a printer or use 'Save as PDF' option.");
            return;
        }
        if (job.showPrintDialog(stage)) {
            boolean success = job.printPage(editor);
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
        undoItem.setOnAction(e -> editor.undo());

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> editor.redo());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> editor.cut());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> editor.copy());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> editor.paste());

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> editor.selectAll());

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

        insertMenu.getItems().addAll(imageItem, tableItem);
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
        insertTableBtn.setOnAction(e -> ContentInserter.openTablePicker(editor, insertTableBtn));

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
                wordColorBtn, wordHighlightBtn,
                new Separator(),
                insertImageBtn, insertTableBtn,
                new Separator(),
                alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn

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

        int col = 0;
        int row = 0;
        for (Color color : WORD_COLORS) {
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
                editor.requestFocus();
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
        String content = editor.getText();
        int index = content.indexOf(searchText);
        if (index >= 0) {
            editor.selectRange(index, index + searchText.length());
            editor.requestFollowCaret();

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
        String content = editor.getText();
        int index = content.indexOf(searchText, lastSearchIndex);

        if (index >= 0) {
            editor.selectRange(index, index + searchText.length());
            editor.requestFollowCaret();
            lastSearchIndex = index + 1;
        } else if (lastSearchIndex > 0) {
            // Wrap around to beginning
            lastSearchIndex = 0;
            index = content.indexOf(searchText, 0);
            if (index >= 0) {
                editor.selectRange(index, index + searchText.length());
                editor.requestFollowCaret();
                lastSearchIndex = index + 1;
            } else {
                showNotFound(searchText);
            }
        } else {
            showNotFound(searchText);
        }
    }

    private void replaceCurrentSelection(String searchText, String replaceText) {
        String selectedText = editor.getSelectedText();
        if (selectedText.equals(searchText)) {
            editor.replaceSelection(replaceText);
            findNextText(searchText);
        } else {
            findNextText(searchText);
        }
    }

    private void replaceAllText(String searchText, String replaceText) {
        String content = editor.getText();
        int count = 0;
        int index = 0;

        while ((index = content.indexOf(searchText, index)) >= 0) {
            count++;
            index += searchText.length();
        }

        if (count > 0) {
            String newContent = content.replace(searchText, replaceText);
            editor.replaceText(newContent);

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
        int currentParagraph = editor.getCurrentParagraph();

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
        editor.setParagraphStyle(currentParagraph, alignStyle);
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
        IndexRange selection = editor.getSelection();

        if (selection.getLength() > 0) {
            editor.setStyle(selection.getStart(), selection.getEnd(), style);
        }

        // Her durumda gelecekteki yazılar için stili güncelle
        currentTypingStyle = style;

        // İmleç rengini de değiştir (Word Web gibi)
        Color caretColor = textColorPicker.getValue();
        editor.setStyle(String.format("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; " +
                "caret-color: %s;", toHexString(caretColor)));

        editor.requestFocus();
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
            style.append("-fx-background-color: ").append(toHexString(highlight)).append("; ");
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

    private void updateWordCount() {
        String text = editor.getText();
        int chars = text.length();
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;

        wordCountLabel.setText("Words: " + words);
        charCountLabel.setText("Characters: " + chars);
    }

    private void newDocument(Stage stage) {
        if (!confirmDiscard(stage))
            return;

        editor.clear();
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
                    editor.replaceText(content.toString());
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

            editor.replaceText(content.toString());
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
            run.setText(editor.getText());
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
            writer.write(editor.getText());
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
        ContentInserter.insertImage(editor, stage);
        isDirty = true;
    }

    private void insertTable(Stage stage) {
        ContentInserter.openTablePicker(editor, insertTableBtn);
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
                PDFExporter.export(editor, file);
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

    public static void main(String[] args) {
        launch(args);
    }
}
