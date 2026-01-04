package bte;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Initialize the HTMLEditor
        // This component includes Bold, Italic, Underline, and Font tools by default
        HTMLEditor editor = new HTMLEditor();
        editor.setPrefHeight(600);

        // 2. Use a BorderPane to let the editor fill the window
        BorderPane root = new BorderPane();
        root.setCenter(editor);

        // 3. Setup the Scene
        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Burak's Word Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}