# 📝 JavaFX Word Processor

A modern, lightweight, and feature-rich word processor built with **Java** and **JavaFX**. This application allows users to create formatted documents with dynamic content handling, including resizable images, interactive tables, and PDF export capabilities.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

## ✨ Features

* **Rich Text Editing:** Full support for standard text formatting (Bold, Italic, Underline, Fonts, Colors).
* **🖼️ Resizable Images:** Insert images and resize them dynamically within the editor using drag handles.
* **📊 Interactive Table Builder:**
    * **Visual Grid Picker:** Create tables by hovering over a grid (similar to MS Word).
    * **Context Menu:** Right-click support to **Add/Delete Rows**, **Columns**, or the entire table.
    * **Resizable:** Adjust table dimensions directly in the editor.
* **📄 PDF Export:** Seamlessly export your documents to PDF format while preserving layout and images.
* **Clean UI:** Custom CSS styling for a polished user experience.

## 🚀 Getting Started

### Prerequisites

* **Java JDK** (17 or higher recommended)
* **Maven** (Build tool)

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/burakshaus/Word.git](https://github.com/burakshaus/Word.git)
    cd Word
    ```

2.  **Build the project:**
    ```bash
    mvn clean install
    ```

3.  **Run the application:**
    ```bash
    mvn clean javafx:run
    ```

## 🏗️ Project Structure

The project follows a standard Maven structure with the `bte` (Better Text Editor) package:

* **`MainApp.java`**: Entry point of the application.
* **`CustomEditor.java`**: The core editor component extending JavaFX functionalities.
* **`ContentInserter.java`**: Manages insertion logic for images and tables.
* **`TablePickerPopup.java`**: *[New]* A custom UI component for visual table selection.
* **`ResizableImageView.java` & `ResizableTableView.java`**: Custom wrappers allowing content resizing.
* **`PDFExporter.java`**: Handles the conversion of the editor content to PDF files.

## 🎮 Usage Guide

### Inserting a Table
1.  Click the **"Add Table"** button on the toolbar.
2.  A 10x10 grid will appear. Move your mouse to select the desired size (e.g., 4x3).
3.  Click to insert the table.

### Editing a Table
* **Right-click** on any cell in the table to open the context menu.
* Choose options like **"Delete Row"**, **"Delete Column"**, or **"Delete Table"**.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1.  Fork the project
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Developed by Burak.*
