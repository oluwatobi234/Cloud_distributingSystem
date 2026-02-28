CREATE DATABASE IF NOT EXISTS comp20081;
USE comp20081;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    token VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255),
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_metadata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS access_control (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT,
    username VARCHAR(255),
    can_read BOOLEAN DEFAULT FALSE,
    can_write BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (file_id) REFERENCES file_metadata(id)
);

-- Sync test user (matches SQLite)
INSERT INTO users (username, password, role) VALUES ('admin', 'hashed_password', 'admin');