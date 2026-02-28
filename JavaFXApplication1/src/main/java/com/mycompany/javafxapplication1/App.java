package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DB myObj = new DB();
        myObj.log("-------- Starting Application ------------");
        
        // Create table if not exists (don't drop)
        try {
            myObj.createTable(myObj.getTableName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        SessionManager sessionManager = new SessionManager();
        sessionManager.ensureSessionTable();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            PrimaryController primaryController = loader.getController();
            primaryController.setSessionManager(sessionManager);

            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}