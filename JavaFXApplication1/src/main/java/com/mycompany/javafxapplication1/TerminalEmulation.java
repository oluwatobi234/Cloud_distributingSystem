/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/**
 *
 * @author ntu-user
 */
public class TerminalEmulation {
    
    private final String username;
    private final Path userHome;
    
    public TerminalEmulation(String username) {
        this.username = username;
        this.userHome = Paths.get("storage", username).toAbsolutePath().normalize();
        try {
            Files.createDirectories(userHome);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String executeCommand(String input) {
        String[] allowedCommands = {"mv", "ls", "cp", "mkdir", "ps", "whoami", "tree", "nano"};
        
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Check if command is allowed
        String[] parts = input.trim().split("\\s+");
        String cmd = parts[0];
        
        boolean valid = false;
        for (String allowed : allowedCommands) {
            if (allowed.equals(cmd)) {
                valid = true;
                break;
            }
        }
        
        if (!valid) {
            return "Error: Command '" + cmd + "' not allowed. Allowed: mv, ls, cp, mkdir, ps, whoami, tree, nano";
        }
        
        switch (cmd) {
            case "ls":
                return cmdLs(parts);
            case "mkdir":
                return cmdMkdir(parts);
            case "cp":
                return cmdCp(parts);
            case "mv":
                return cmdMv(parts);
            case "ps":
                return cmdPs();
            case "whoami":
                return username;
            case "tree":
                return cmdTree(userHome, 0);
            case "nano":
                return "Error: nano requires interactive mode. Use GUI file manager instead.";
            default:
                return "Error: Unknown command";
        }
    }
    
    private String cmdLs(String[] parts) {
        try {
            Path target = userHome;
            if (parts.length > 1) {
                target = userHome.resolve(parts[1]).normalize();
            }
            
            if (!target.startsWith(userHome)) {
                return "Error: Access denied";
            }
            
            if (!Files.exists(target)) {
                return "Error: Directory not found";
            }
            
            StringBuilder sb = new StringBuilder();
            try (var stream = Files.list(target)) {
                stream.forEach(path -> {
                    sb.append(path.getFileName().toString());
                    if (Files.isDirectory(path)) {
                        sb.append("/");
                    }
                    sb.append("\n");
                });
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String cmdMkdir(String[] parts) {
        if (parts.length < 2) {
            return "Error: mkdir requires directory name";
        }
        try {
            Path newDir = userHome.resolve(parts[1]).normalize();
            if (!newDir.startsWith(userHome)) {
                return "Error: Access denied";
            }
            Files.createDirectories(newDir);
            return "Directory created: " + parts[1];
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String cmdCp(String[] parts) {
        if (parts.length < 3) {
            return "Error: cp requires source and destination";
        }
        try {
            Path source = userHome.resolve(parts[1]).normalize();
            Path dest = userHome.resolve(parts[2]).normalize();
            
            if (!source.startsWith(userHome) || !dest.startsWith(userHome)) {
                return "Error: Access denied";
            }
            
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return "Copied: " + parts[1] + " -> " + parts[2];
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String cmdMv(String[] parts) {
        if (parts.length < 3) {
            return "Error: mv requires source and destination";
        }
        try {
            Path source = userHome.resolve(parts[1]).normalize();
            Path dest = userHome.resolve(parts[2]).normalize();
            
            if (!source.startsWith(userHome) || !dest.startsWith(userHome)) {
                return "Error: Access denied";
            }
            
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return "Moved: " + parts[1] + " -> " + parts[2];
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String cmdPs() {
        return "PID\tUSER\tCOMMAND\n" + 
               "1001\t" + username + "\tjava\n" +
               "1002\t" + username + "\tfilemanager";
    }
    
    private String cmdTree(Path path, int level) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(level);
        
        try {
            sb.append(indent).append(path.getFileName()).append("/\n");
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    stream.forEach(child -> {
                        if (Files.isDirectory(child)) {
                            sb.append(cmdTree(child, level + 1));
                        } else {
                            sb.append(indent).append("  ").append(child.getFileName()).append("\n");
                        }
                    });
                }
            }
        } catch (IOException e) {
            sb.append("Error reading directory");
        }
        return sb.toString();
    }
}
