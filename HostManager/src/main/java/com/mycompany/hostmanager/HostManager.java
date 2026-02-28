/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.hostmanager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author ntu-user
 */
public class HostManager {

    private static final int PORT = 8085;
    private final List<String> systemLogs = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> metrics = new ConcurrentHashMap<>();
    private final Map<String, Boolean> containerHealth = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    
    public static void main(String[] args) {
        new HostManager().start();
    }
    
    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/log", new LogHandler());
            server.createContext("/logs", new LogsHandler());
            server.createContext("/metrics", new MetricsHandler());
            server.createContext("/health", new HealthHandler());
            server.createContext("/dashboard", new DashboardHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("Host Manager started on port " + PORT);
            System.out.println("Monitoring and logging service active");
            
            // Start monitoring
            startMonitoring();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private class LogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            String logEntry = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String timestamped = "[" + Instant.now() + "] " + logEntry;
            systemLogs.add(timestamped);
            System.out.println("LOG: " + logEntry);
            
            // Update metrics
            String[] parts = logEntry.split(":");
            if (parts.length > 0) {
                metrics.merge(parts[0], 1, Integer::sum);
            }
            
            sendResponse(exchange, 200, "{\"status\":\"logged\"}");
        }
    }
    
    private class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("=== SYSTEM LOGS ===\n\n");
            
            // Show last 50 logs
            int start = Math.max(0, systemLogs.size() - 50);
            for (int i = start; i < systemLogs.size(); i++) {
                sb.append(systemLogs.get(i)).append("\n");
            }
            
            sendResponse(exchange, 200, sb.toString());
        }
    }
    
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"timestamp\": \"").append(Instant.now()).append("\",\n");
            sb.append("  \"metrics\": {\n");
            
            int i = 0;
            for (Map.Entry<String, Integer> entry : metrics.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
                if (++i < metrics.size()) sb.append(",");
                sb.append("\n");
            }
            
            sb.append("  },\n");
            sb.append("  \"total_logs\": ").append(systemLogs.size()).append(",\n");
            sb.append("  \"containers_monitored\": ").append(containerHealth.size()).append("\n");
            sb.append("}");
            
            sendResponse(exchange, 200, sb.toString());
        }
    }
    
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("{\n  \"containers\": [\n");
            
            int i = 0;
            for (Map.Entry<String, Boolean> entry : containerHealth.entrySet()) {
                sb.append("    {\n");
                sb.append("      \"name\": \"").append(entry.getKey()).append("\",\n");
                sb.append("      \"healthy\": ").append(entry.getValue()).append("\n");
                sb.append("    }");
                if (++i < containerHealth.size()) sb.append(",");
                sb.append("\n");
            }
            
            sb.append("  ]\n}");
            sendResponse(exchange, 200, sb.toString());
        }
    }
    
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Host Manager Dashboard</title></head><body>");
            html.append("<h1>Cloud Infrastructure Monitor</h1>");
            html.append("<h2>System Status</h2>");
            html.append("<p>Total Logs: ").append(systemLogs.size()).append("</p>");
            html.append("<p>Containers: ").append(containerHealth.size()).append("</p>");
            html.append("<h2>Recent Activity</h2><ul>");
            
            int start = Math.max(0, systemLogs.size() - 10);
            for (int i = systemLogs.size() - 1; i >= start; i--) {
                html.append("<li>").append(systemLogs.get(i)).append("</li>");
            }
            
            html.append("</ul></body></html>");
            
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
    
    private void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Monitor other containers
            checkContainer("load-balancer", "http://localhost:8081/health");
            checkContainer("file-partitioner", "http://localhost:8084/health");
            checkContainer("file-storage-1", "http://localhost:8082/health");
            checkContainer("file-storage-2", "http://localhost:8083/health");
            checkContainer("mysql", "http://localhost:3306"); // MySQL doesn't have HTTP, would need different check
        }, 5, 10, TimeUnit.SECONDS);
    }
    
    private void checkContainer(String name, String url) {
        try {
            // Simple HTTP check (for HTTP endpoints)
            if (url.contains("3306")) {
                // MySQL check would be different (TCP connection)
                containerHealth.put(name, true); // Assume up for now
                return;
            }
            
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            int code = conn.getResponseCode();
            containerHealth.put(name, code == 200);
        } catch (Exception e) {
            containerHealth.put(name, false);
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
