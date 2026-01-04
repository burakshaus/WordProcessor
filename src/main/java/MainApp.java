import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1. The Main Editor (Center)
        TextArea editor = new TextArea();
        editor.setWrapText(true); // Makes text wrap to the next line

        // 2. The Menu Bar (Top - Row 1)
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem exitItem = new MenuItem("Exit");
        fileMenu.getItems().addAll(saveItem, new SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(fileMenu);

        // 3. The Tool Bar (Top - Row 2)
        ToolBar toolBar = new ToolBar();
        Button boldBtn = new Button("B");
        boldBtn.setStyle("-fx-font-weight: bold;");
        Button italicBtn = new Button("I");
        italicBtn.setStyle("-fx-font-style: italic;");
        toolBar.getItems().addAll(boldBtn, italicBtn, new Separator(), new Label("Font Size:"), new TextField("12"));

        // 4. Combine Menu and Toolbar in a Vertical Box (VBox)
        VBox topContainer = new VBox(menuBar, toolBar);

        // 5. Layout Setup
        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(editor);

        // 6. Scene and Stage
        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("BTE Word Processor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}