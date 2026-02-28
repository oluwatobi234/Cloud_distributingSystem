/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.filepartitioner;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
/**
 *
 * @author ntu-user
 */
public class FilePartitioner {

    private static final int PORT = 8084;
    private static final List<String> STORAGE_CONTAINERS = Arrays.asList(
        "http://localhost:8082",  // File Storage 1
        "http://localhost:8083"   // File Storage 2
    );
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private int roundRobinIndex = 0;
    
    public static void main(String[] args) {
        new FilePartitioner().start();
    }
    
    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/upload", new UploadHandler());
            server.createContext("/download", new DownloadHandler());
            server.createContext("/delete", new DeleteHandler());
            server.createContext("/health", new HealthHandler());
            
            server.setExecutor(executor);
            server.start();
            
            System.out.println("File Partitioner started on port " + PORT);
            System.out.println("Encrypts, chunks, and distributes files");
            
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
                // Read request
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String[] parts = body.split("\\|", 2);
                String filename = parts[0];
                String content = parts.length > 1 ? parts[1] : "";
                
                System.out.println("Processing upload: " + filename);
                
                // STEP 1: Lock file (Req #11 - Concurrency Control)
                LockManager.lock(filename);
                
                try {
                    // STEP 2: Encrypt content (Req #6)
                    String encrypted = EncryptionUtil.encrypt(content);
                    byte[] encryptedBytes = encrypted.getBytes(StandardCharsets.UTF_8);
                    
                    // STEP 3: Split into chunks (Req #20)
                    List<ChunkUtil.Chunk> chunks = ChunkUtil.split(encryptedBytes);
                    System.out.println("Split into " + chunks.size() + " chunks");
                    /*for (int i = 0; i < chunks.size(); i++) {
                        String container = STORAGE_CONTAINERS.get(roundRobinIndex);
                        roundRobinIndex = (roundRobinIndex + 1) % STORAGE_CONTAINERS.size();

                        // Convert chunk to string for sending
                        String chunkData = Base64.getEncoder().encodeToString(chunks.get(i).data);
                        String requestBody = filename + "_chunk" + i + "|" + chunkData;

                        // Send HTTP POST to File Storage
                        try {
                            URL url = new URL(container + "/upload");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setDoOutput(true);

                            try (OutputStream os = conn.getOutputStream()) {
                                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                            }

                            int responseCode = conn.getResponseCode();
                            System.out.println("Chunk " + i + " sent to " + container + " - Response: " + responseCode);

                        } catch (Exception e) {
                            System.out.println("Failed to send chunk " + i + ": " + e.getMessage());
                        }
                    }*/
                    // STEP 4: Distribute chunks to storage containers
                    for (int i = 0; i < chunks.size(); i++) {
                        String container = STORAGE_CONTAINERS.get(roundRobinIndex);
                        roundRobinIndex = (roundRobinIndex + 1) % STORAGE_CONTAINERS.size();
                        
                        // Send chunk to container (simplified - just print for now)
                        System.out.println("Sending chunk " + i + " to " + container);
                        System.out.println("  - Size: " + chunks.get(i).data.length + " bytes");
                        System.out.println("  - Checksum: " + chunks.get(i).checksum);
                        System.out.println("  - Target: " + container);
                        System.out.println("  - Status: Would send HTTP POST to /upload");
                        
                       
                    }
                    System.out.println("All " + chunks.size() + " chunks processed");
                    sendResponse(exchange, 200, "{\"status\":\"success\",\"chunks\":" + chunks.size() + "}");
                    
                } finally {
                    // STEP 5: Always unlock
                    LockManager.unlock(filename);
                }
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // For demo - would fetch chunks, reassemble, decrypt
            sendResponse(exchange, 200, "{\"status\":\"download_not_implemented\"}");
        }
    }
    
    private class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // For demo - would delete chunks from all containers
            sendResponse(exchange, 200, "{\"status\":\"delete_not_implemented\"}");
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
