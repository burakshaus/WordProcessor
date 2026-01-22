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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import org.fxmisc.richtext.InlineCssTextArea;

import javafx.print.PrinterJob;
import javafx.scene.effect.DropShadow;

import java.io.*;

public class MainApp extends Application {

    private static final double PAGE_WIDTH = 595;
    private static final double PAGE_HEIGHT = 842;
    private static final double MARGIN = 50;

    private InlineCssTextArea editor;
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

    @Override
    public void start(Stage stage) {
        // Create the rich text editor
        editor = new InlineCssTextArea();
        editor.setWrapText(true);
        editor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        // Track changes
        editor.textProperty().addListener((obs, oldText, newText) -> {
            isDirty = true;
            updateWordCount();
        });

        VBox pageContainer = createPageView();
        // Layout
        BorderPane root = new BorderPane();

        // Menu Bar
        MenuBar menuBar = createMenuBar(stage);

        // Toolbar
        ToolBar toolBar = createToolBar();

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

        scrollPane.setStyle("-fx-background: #e0e0e0;");
        // Keyboard shortcuts
        setupKeyboardShortcuts(scene, stage);

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

        fileMenu.getItems().addAll(newItem, openItem, separator, saveItem, saveAsItem, printItem,
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

        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        // Bold button
        boldBtn = new ToggleButton("B");
        boldBtn.setStyle("-fx-font-weight: bold;");
        boldBtn.setTooltip(new Tooltip("Bold (Ctrl+B)"));
        boldBtn.setOnAction(e -> applyStyle());

        // Italic button
        italicBtn = new ToggleButton("I");
        italicBtn.setStyle("-fx-font-style: italic;");
        italicBtn.setTooltip(new Tooltip("Italic (Ctrl+I)"));
        italicBtn.setOnAction(e -> applyStyle());

        // Underline button
        underlineBtn = new ToggleButton("U");
        underlineBtn.setStyle("-fx-underline: true;");
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

        // Text color
        textColorPicker = new ColorPicker(Color.BLACK);
        textColorPicker.setTooltip(new Tooltip("Text Color"));
        textColorPicker.setOnAction(e -> applyStyle());

        alignLeftBtn = new ToggleButton("L");
        alignLeftBtn.setTooltip(new Tooltip("Align Left"));
        alignLeftBtn.setOnAction(e -> applyAlignment("LEFT"));

        alignCenterBtn = new ToggleButton("C");
        alignCenterBtn.setTooltip(new Tooltip("Align Center"));
        alignCenterBtn.setOnAction(e -> applyAlignment("CENTER"));

        alignRightBtn = new ToggleButton("R");
        alignRightBtn.setTooltip(new Tooltip("Align Right"));
        alignRightBtn.setOnAction(e -> applyAlignment("RIGHT"));

        alignJustifyBtn = new ToggleButton("J");
        alignJustifyBtn.setTooltip(new Tooltip("Align Justify"));
        alignJustifyBtn.setOnAction(e -> applyAlignment("JUSTIFY"));

        toolBar.getItems().addAll(
                boldBtn, italicBtn, underlineBtn,
                new Separator(),
                fontFamilyCombo, fontSizeCombo,
                new Separator(),
                textColorPicker,
                new Separator(),
                alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn);

        return toolBar;
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
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

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
        IndexRange selection = editor.getSelection();
        if (selection.getLength() == 0)
            return;

        String style = buildStyleString();
        editor.setStyle(selection.getStart(), selection.getEnd(), style);
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

        // Color
        Color color = textColorPicker.getValue();
        style.append("-fx-fill: ").append(toRgbString(color)).append("; ");

        return style.toString();
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d, %d, %d)",
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
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(editor.getText());
        try {
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

    public static void main(String[] args) {
        launch(args);
    }
}