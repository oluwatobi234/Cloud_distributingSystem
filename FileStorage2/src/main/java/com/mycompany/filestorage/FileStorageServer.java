/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.filestorage;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
/**
 *
 * @author ntu-user
 */
public class FileStorageServer {

    private static final int PORT = 8083;
    private static final String STORAGE_DIR = "storage";
    
    public static void main(String[] args) {
        new FileStorageServer().start();
    }
    
    public void start() {
        try {
            // Create storage directory
            Files.createDirectories(Paths.get(STORAGE_DIR));
            
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/upload", new UploadHandler());
            server.createContext("/download", new DownloadHandler());
            server.createContext("/delete", new DeleteHandler());
            server.createContext("/health", new HealthHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("File Storage Server started on port " + PORT);
            System.out.println("Storage directory: " + STORAGE_DIR);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String[] parts = body.split("\\|", 2);
                String filename = parts[0];
                String content = parts.length > 1 ? parts[1] : "";
                
                // Save file
                Path filePath = Paths.get(STORAGE_DIR, filename).normalize();
                if (!filePath.startsWith(STORAGE_DIR)) {
                    sendResponse(exchange, 403, "Invalid filename");
                    return;
                }
                
                Files.writeString(filePath, content);
                System.out.println("Saved: " + filename);
                
                sendResponse(exchange, 200, "{\"status\":\"success\",\"file\":\"" + filename + "\"}");
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }
    
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                String filename = exchange.getRequestURI().getQuery();
                if (filename == null || filename.isEmpty()) {
                    sendResponse(exchange, 400, "Filename required");
                    return;
                }
                
                Path filePath = Paths.get(STORAGE_DIR, filename).normalize();
                if (!filePath.startsWith(STORAGE_DIR) || !Files.exists(filePath)) {
                    sendResponse(exchange, 404, "File not found");
                    return;
                }
                
                String content = Files.readString(filePath);
                sendResponse(exchange, 200, content);
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }
    
    private class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"DELETE".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                String filename = exchange.getRequestURI().getQuery();
                if (filename == null || filename.isEmpty()) {
                    sendResponse(exchange, 400, "Filename required");
                    return;
                }
                
                Path filePath = Paths.get(STORAGE_DIR, filename).normalize();
                if (!filePath.startsWith(STORAGE_DIR)) {
                    sendResponse(exchange, 403, "Invalid filename");
                    return;
                }
                
                Files.deleteIfExists(filePath);
                System.out.println("Deleted: " + filename);
                
                sendResponse(exchange, 200, "{\"status\":\"deleted\"}");
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }
    
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "{\"status\":\"healthy\"}");
        }
    }
    
    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
