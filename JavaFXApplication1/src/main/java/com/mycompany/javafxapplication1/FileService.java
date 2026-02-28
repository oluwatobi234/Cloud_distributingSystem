package com.mycompany.javafxapplication1;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import java.util.function.Consumer;

public class FileService {
    
    private static FileService instance;
    private final String LOAD_BALANCER_URL = System.getenv().getOrDefault("LOAD_BALANCER_URL", "http://localhost:8081");
    private final String username;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    public static synchronized FileService getInstance(String username) {
        if (instance == null) {
            instance = new FileService(username);
        }
        return instance;
    }
    
    private FileService(String username) {
        this.username = username;
        System.out.println("FileService connecting to: " + LOAD_BALANCER_URL);
    }
    
    private String sendRequest(String endpoint, String method, String body) throws IOException {
        URL url = new URL(LOAD_BALANCER_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(90000); 
        
        if (body != null && !body.isEmpty()) {
            conn.setRequestProperty("Content-Type", "text/plain");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    public void createFile(String filename, String content, Runnable onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                String requestBody = filename + "|" + content;
                String response = sendRequest("/upload", "POST", requestBody);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
    
    public void appendToFile(String filename, String content, Runnable onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                // Download existing
                String existing = sendRequest("/download?filename=" + filename, "GET", null);
                
                // Upload combined
                String newContent = existing + content;
                String requestBody = filename + "|" + newContent;
                sendRequest("/upload", "POST", requestBody);
                
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
    
    public void deleteFile(String filename, Runnable onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                sendRequest("/delete?filename=" + filename, "DELETE", null);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
    
    public void readFile(String filename, Consumer<String> onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                String content = sendRequest("/download?filename=" + filename, "GET", null);
                Platform.runLater(() -> onSuccess.accept(content));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
    
    public List<String> listFiles() {
        return new ArrayList<>();
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}