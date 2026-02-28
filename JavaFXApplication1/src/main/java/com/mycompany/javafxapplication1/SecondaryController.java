package com.mycompany.javafxapplication1;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextArea;

public class SecondaryController {
    
    @FXML private SessionManager sessionManager;
    @FXML private TextField userTextField;
    @FXML private TableView<FileRecord> fileTable;
    @FXML private TableColumn<FileRecord, String> fileNameColumn;
    @FXML private TableColumn<FileRecord, String> ownerColumn;
    @FXML private Button secondaryButton;
    @FXML private TextField customTextField;
    @FXML private TextArea terminalOutput;
    @FXML private TextField terminalInput;
    
    private TerminalEmulation terminal;
    
    private FileService fileService;
    private String currentUsername;
    private ObservableList<FileRecord> fileList = FXCollections.observableArrayList();
    
    public void setSessionManager(SessionManager sm) { 
        this.sessionManager = sm; 
    }
    
    public void setFileService(FileService fs) { 
        this.fileService = fs; 
        refreshFileList();
    }
    
    @FXML
    public void initialize() { 
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName")); 
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("owner")); 
        fileTable.setItems(fileList);
    }
    private boolean hasWritePermission(String filename, String username) {
        DB db = new DB();
        return db.checkPermission(filename, username, "write");
    }
    private void refreshFileList() {
        
        if (fileService != null) {
            fileList.clear();
            DB db = new DB();
            
            List<FileRecord> sharedFiles = db.getSharedFilesForUser(currentUsername);
            fileList.addAll(sharedFiles);
            List<String> files = fileService.listFiles();
            for (String file : files) {
                fileList.add(new FileRecord(file, currentUsername));
            }
        }
    }
    
    @FXML
    private void handleUpload(ActionEvent event) {
        // Create new file dialog
        TextInputDialog nameDialog = new TextInputDialog("newfile.txt");
        nameDialog.setTitle("Create New File");
        nameDialog.setHeaderText("Enter filename");
        nameDialog.setContentText("Filename:");
        
        Optional<String> nameResult = nameDialog.showAndWait();
        if (!nameResult.isPresent()) return;
        
        final String filename = nameResult.get().trim();
        if (filename.isEmpty()) return;

        // Get content
        TextInputDialog contentDialog = new TextInputDialog("");
        contentDialog.setTitle("File Content");
        contentDialog.setHeaderText("Enter content for " + filename);
        contentDialog.setContentText("Content:");
        
        Optional<String> contentResult = contentDialog.showAndWait();
        if (!contentResult.isPresent()) return;
        
        final String content = contentResult.get();

        // Create file
        fileService.createFile(filename, content, 
            () -> {
                fileList.add(new FileRecord(filename, currentUsername));
                showAlert("Success", "File created: " + filename);
            },
            error -> showAlert("Error", "Failed to create file: " + error)
        );
    }
    
    @FXML
    private void handleUpdate(ActionEvent event) {
        FileRecord selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a file to update");
            return;
        }
        // Before updating/deleting, check if user has permission
        if (!selected.getOwner().equals(currentUsername)) {
            // Check if they have write permission
            if (!hasWritePermission(selected.getFileName(), currentUsername)) {
                showAlert("Error", "You don't have permission to modify this file");
                return;
            }
        }
        final String filename = selected.getFileName();
        
        // Get content to append
        TextInputDialog contentDialog = new TextInputDialog("");
        contentDialog.setTitle("Update File");
        contentDialog.setHeaderText("Enter content to append to " + filename);
        contentDialog.setContentText("Content to append:");
        
        Optional<String> contentResult = contentDialog.showAndWait();
        if (!contentResult.isPresent()) return;
        
        final String content = contentResult.get();

        fileService.appendToFile(filename, content,
            () -> showAlert("Success", "File updated: " + filename),
            error -> showAlert("Error", "Failed to update file: " + error)
        );
    }
    
    @FXML
    private void handleDelete(ActionEvent event) { 
        FileRecord selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a file to delete");
            return;
        }
        // Before updating/deleting, check if user has permission
        if (!selected.getOwner().equals(currentUsername)) {
            // Check if they have write permission
            if (!hasWritePermission(selected.getFileName(), currentUsername)) {
                showAlert("Error", "You don't have permission to modify this file");
                return;
            }
        }
        final String filename = selected.getFileName();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + filename + "?");
        confirm.setContentText("This cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            fileService.deleteFile(filename,
                () -> {
                    fileList.remove(selected);
                    showAlert("Success", "File deleted");
                },
                error -> showAlert("Error", "Failed to delete: " + error)
            );
        }
    }
    
    @FXML
    private void handleShare(ActionEvent event) {
        FileRecord selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a file to share");
            return;
        }

        // Get username to share with
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle("Share File");
        userDialog.setHeaderText("Share " + selected.getFileName());
        userDialog.setContentText("Enter username to share with:");

        Optional<String> userResult = userDialog.showAndWait();
        if (!userResult.isPresent()) return;

        String shareWith = userResult.get().trim();
        if (shareWith.isEmpty()) return;

        // Get permission type
        ChoiceDialog<String> permDialog = new ChoiceDialog<>("read", "read", "write");
        permDialog.setTitle("Share Permission");
        permDialog.setHeaderText("Select permission for " + shareWith);
        permDialog.setContentText("Permission:");

        Optional<String> permResult = permDialog.showAndWait();
        if (!permResult.isPresent()) return;

        String permission = permResult.get();

        // Save to database
        DB db = new DB();
        db.shareFile(selected.getFileName(), currentUsername, shareWith, permission);
        showAlert("Success", "Shared '" + selected.getFileName() + "' with " + shareWith + " (" + permission + " access)");
    }
    @FXML
    private void RefreshBtnHandler(ActionEvent event) {
        refreshFileList();
    }
    @FXML
    private void handleView(ActionEvent event) {
        FileRecord selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a file to view");
            return;
        }

        final String filename = selected.getFileName();

        fileService.readFile(filename,
            content -> {
                // Show content in a dialog
                TextArea textArea = new TextArea(content);
                textArea.setEditable(false);
                textArea.setPrefRowCount(20);
                textArea.setPrefColumnCount(50);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("View File: " + filename);
                alert.setHeaderText(null);
                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
            },
            error -> showAlert("Error", "Failed to read file: " + error)
        );
    }
    
    @FXML
    private void handleTerminalCommand() {
        if (terminal == null) {
            terminal = new TerminalEmulation(currentUsername);
        }

        String command = terminalInput.getText();
        if (command.trim().isEmpty()) return;

        // Show command in output
        terminalOutput.appendText("terminal> " + command + "\n");

        // Execute and show result
        String result = terminal.executeCommand(command);
        if (!result.isEmpty()) {
            terminalOutput.appendText(result + "\n");
        }

        // Clear input
        terminalInput.clear();
    }
    @FXML
    private void switchToPrimary() {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init_Data(String[] credentials) {
        String username = (credentials != null && credentials.length > 0) ? credentials[0] : "unknown";
        String token = (credentials != null && credentials.length > 1) ? credentials[1] : null;
        userTextField.setText(username);
        this.currentUsername = username;
        
        // Initialize FileService for this user
        this.fileService = FileService.getInstance(username);
        refreshFileList();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}