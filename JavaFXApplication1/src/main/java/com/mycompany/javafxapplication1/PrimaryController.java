package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class PrimaryController {

    @FXML
    private Button registerBtn;

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField;
   
    private SessionManager sessionManager;

    public void setSessionManager(SessionManager sm) { 
        this.sessionManager = sm; 
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Register a new User");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);

        Optional<ButtonType> result = alert.showAndWait();
    }

    @FXML
    private void switchToSecondary() {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            DB myObj = new DB();
            String[] credentials = {userTextField.getText(), passPasswordField.getText()};
            System.out.println("Checking login for: " + userTextField.getText());

            if(myObj.validateUser(userTextField.getText(), passPasswordField.getText())){
                System.out.println("DEBUG: Login validation PASSED");
                String username = userTextField.getText();
                String token = null;
                System.out.println("Login success!");

                if (this.sessionManager != null) {
                    token = this.sessionManager.createSession(username, 30);
                    sessionManager.setUsername(username);
                }
                
                String role = myObj.getUserRole(username);
                System.out.println("User role: " + role);
                System.out.println("DEBUG: Retrieved role = '" + role + "'");
                if ("admin".equals(role)) {
                    System.out.println("DEBUG: Looking for Admin.fxml at: " + getClass().getResource("Admin.fxml"));
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("Admin.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    secondaryStage.setScene(scene);
                    
                    AdminController controller = loader.getController();
                    controller.setSessionManager(sessionManager);
                    
                    secondaryStage.setTitle("Admin Dashboard");
                    secondaryStage.show();
                    primaryStage.close();
                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    secondaryStage.setScene(scene);

                    SecondaryController controller = loader.getController();
                    controller.setSessionManager(sessionManager);
                    controller.init_Data(new String[] { username, token });

                    secondaryStage.setTitle("File Management");
                    secondaryStage.show();
                    primaryStage.close();
                }

            } else {
                System.out.println("DEBUG: Login validation FAILED");
                dialogue("Invalid User Name / Password","Please try again!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
