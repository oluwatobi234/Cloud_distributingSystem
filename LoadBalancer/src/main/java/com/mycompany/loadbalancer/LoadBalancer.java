/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.loadbalancer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;
import java.time.Duration;

/**
 *
 * @author ntu-user
 */
//tr
public class LoadBalancer {   

  private static final int PORT = 8081;
    private static final List<String> STORAGE_CONTAINERS = Arrays.asList(
            // send request to file storages
        System.getenv().getOrDefault("STORAGE_1_URL", "http://localhost:8082"),
        System.getenv().getOrDefault("STORAGE_2_URL", "http://localhost:8083")
    );
    
    private int roundRobinIndex = 0;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Map<String, Integer> requestCount = new ConcurrentHashMap<>();
    private final Map<String, Boolean> containerHealth = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public static void main(String[] args) {
        new LoadBalancer().start();
    }
    
    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/upload", new RequestHandler("UPLOAD", "POST"));
            server.createContext("/download", new RequestHandler("DOWNLOAD", "GET"));
            server.createContext("/delete", new RequestHandler("DELETE", "DELETE"));
            server.createContext("/health", new HealthHandler());
            server.createContext("/metrics", new MetricsHandler());
            
            server.setExecutor(executor);
            server.start();
            
            System.out.println("Load Balancer started on port " + PORT);
            System.out.println("Available containers: " + STORAGE_CONTAINERS);
            
            startHealthChecks();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private class RequestHandler implements HttpHandler {
        private final String operation;
        private final String method;
        
        public RequestHandler(String operation, String method) {
            this.operation = operation;
            this.method = method;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestId = UUID.randomUUID().toString();
            long startTime = System.currentTimeMillis();
            
            System.out.println("[" + Instant.now() + "] Request " + requestId + ": " + operation);
            
            // Artificial delay (30-60 seconds as per requirements)
            int delay = 30 + new Random().nextInt(31);
            System.out.println("Simulating delay: " + delay + " seconds");
            
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Select healthy container using Round Robin
            String selectedContainer = selectHealthyContainer();
            if (selectedContainer == null) {
                sendResponse(exchange, 503, "{\"error\":\"No healthy containers available\"}");
                return;
            }
            
            try {
                // Forward request to selected container
                String targetUrl = selectedContainer + exchange.getRequestURI();
                System.out.println("Forwarding to: " + targetUrl);
                
                // Build request to forward
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30));
                
                // Add body for POST/DELETE
                if ("POST".equals(method) || "DELETE".equals(method)) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.GET();
                }
                
                HttpRequest forwardRequest = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(forwardRequest, 
                    HttpResponse.BodyHandlers.ofString());
                
                // Return response from container
                sendResponse(exchange, response.statusCode(), response.body());
                
                // Update metrics
                requestCount.merge(operation, 1, Integer::sum);
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("Request " + requestId + " completed in " + duration + "ms via " + selectedContainer);
                
            } catch (Exception e) {
                System.out.println("Error forwarding request: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    private String selectHealthyContainer() {
        // Try each container in round-robin fashion
        for (int i = 0; i < STORAGE_CONTAINERS.size(); i++) {
            String container = STORAGE_CONTAINERS.get(roundRobinIndex);
            roundRobinIndex = (roundRobinIndex + 1) % STORAGE_CONTAINERS.size();
            
            if (containerHealth.getOrDefault(container, true)) {
                return container;
            }
        }
        return null; // No healthy containers
    }
    
    private void startHealthChecks() {
        ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();
        healthChecker.scheduleAtFixedRate(() -> {
            for (String container : STORAGE_CONTAINERS) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(container + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    boolean healthy = response.statusCode() == 200;
                    containerHealth.put(container, healthy);
                    
                    if (!healthy) {
                        System.out.println("WARNING: " + container + " is unhealthy!");
                    }
                } catch (Exception e) {
                    containerHealth.put(container, false);
                    System.out.println("WARNING: " + container + " is unreachable!");
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }
    
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("{\"containers\":[");
            for (int i = 0; i < STORAGE_CONTAINERS.size(); i++) {
                String container = STORAGE_CONTAINERS.get(i);
                boolean healthy = containerHealth.getOrDefault(container, true);
                sb.append("{\"url\":\"").append(container).append("\",\"healthy\":").append(healthy).append("}");
                if (i < STORAGE_CONTAINERS.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendResponse(exchange, 200, sb.toString());
        }
    }
    
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("{\"requests\":{");
            int i = 0;
            for (Map.Entry<String, Integer> entry : requestCount.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                if (++i < requestCount.size()) sb.append(",");
            }
            sb.append("}}");
            sendResponse(exchange, 200, sb.toString());
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
