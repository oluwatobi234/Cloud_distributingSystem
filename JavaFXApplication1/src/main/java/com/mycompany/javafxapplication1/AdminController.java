/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.util.Optional;
/**
 *
 * @author ntu-user
 */




public class AdminController {
    
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TextField promoteUserField;
    
    private SessionManager sessionManager;
    
    public void setSessionManager(SessionManager sm) { this.sessionManager = sm; }
    
    @FXML
    public void initialize() {
        userCol.setCellValueFactory(new PropertyValueFactory<>("user"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        refreshUserList();
    }
    
    @FXML
    private void refreshUserList() {
        try {
            DB db = new DB();
            ObservableList<User> users = db.getDataFromTable();
            userTable.setItems(users);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handlePromote(ActionEvent event) {
        String username = promoteUserField.getText().trim();
        if (!username.isEmpty()) {
            DB db = new DB();
            db.promoteToAdmin(username);
            refreshUserList();
            showAlert("Success", "User promoted to admin");
        }
    }
    
    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Add delete method to DB if needed
            String username = selected.getUser();
        
        // Confirm deletion
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete user: " + username + "?");
            confirm.setContentText("This cannot be undone.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                DB db = new DB();
                db.deleteUser(username);
                refreshUserList(); // Refresh table
                showAlert("Success", "User deleted");
            }
        } else{
            showAlert("Warning", "Please select a user to delete");
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        // Go back to login
        try {
            Stage stage = (Stage) userTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
   
};
