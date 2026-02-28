/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;


/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class RegisterController {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;
    
    @FXML
    private Text fileText;
    
    @FXML
    private Button selectBtn;
    
    @FXML
    private ComboBox<String> roleComboBox;
    
    
    
    @FXML
    private void selectBtnHandler(ActionEvent event) throws IOException {
        Stage primaryStage = (Stage) selectBtn.getScene().getWindow();
        primaryStage.setTitle("Select a File");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        
        if(selectedFile!=null){
            fileText.setText((String)selectedFile.getCanonicalPath());
        }
        
    }
    
    public void initialize() {
    // Initialize role dropdown
    roleComboBox.getItems().addAll("standard", "admin");
    roleComboBox.setValue("standard"); // Default to standard
    }

    private void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        Optional<ButtonType> result = alert.showAndWait();
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            DB myObj = new DB();

            // Get role from ComboBox
            String role = roleComboBox.getValue();
            System.out.println("DEBUG Register: Selected role = '" + role + "'");

            if (passPasswordField.getText().equals(rePassPasswordField.getText())) {
                // Save user with role
                myObj.addDataToDB(userTextField.getText(), passPasswordField.getText(), role);
                dialogue("Adding information to the database", "Successful!");
                
                try {
                    MySQLSync sync = new MySQLSync();
                    sync.syncUserToMySQL(userTextField.getText(), passPasswordField.getText(), role);
                    sync.close();
                    System.out.println("MySQL sync completed");
                } catch (Exception e) {
                    System.out.println("MySQL sync failed (optional): " + e.getMessage());
                    
                }

                // Get username and check role
                String username = userTextField.getText();
                String userRole = myObj.getUserRole(username);
                System.out.println("DEBUG: User registered as '" + userRole + "'");

                // Route based on role
                if ("admin".equals(userRole)) {
                    // Load Admin Dashboard
                    loader.setLocation(getClass().getResource("Admin.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    secondaryStage.setScene(scene);

                    AdminController controller = loader.getController();
                    SessionManager sm = new SessionManager();
                    sm.ensureSessionTable();
                    controller.setSessionManager(sm);

                    secondaryStage.setTitle("Admin Dashboard");
                    secondaryStage.show();
                    primaryStage.close();

                } else {
                    // Load Standard User View
                    String[] credentials = {username, passPasswordField.getText()};
                    loader.setLocation(getClass().getResource("secondary.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    secondaryStage.setScene(scene);

                    SecondaryController controller = loader.getController();
                    SessionManager sm = new SessionManager();
                    sm.ensureSessionTable();
                    FileService fs = FileService.getInstance(username);
                    controller.setSessionManager(sm);
                    controller.setFileService(fs);
                    controller.init_Data(credentials);

                    secondaryStage.setTitle("Show users");
                    secondaryStage.show();
                    primaryStage.close();
                }

            } else {
                // Passwords don't match - reload register page
                loader.setLocation(getClass().getResource("register.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 640, 480);
                secondaryStage.setScene(scene);
                secondaryStage.setTitle("Register a new User");
                secondaryStage.show();
                primaryStage.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
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
}
