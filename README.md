A distributed file storage platform built with Java, JavaFX, and Docker, designed using a microservices architecture. The system demonstrates load balancing, horizontal scalability, database synchronization, and secure file sharing across containerized storage nodes.

Key Features

Distributed Microservices Architecture
Containerized backend services orchestrated with Docker Compose, separating authentication, load balancing, storage, and monitoring layers.

Intelligent Load Balancing: Round-robin request distribution across multiple storage containers with automated health checks and failure handling.

Hybrid Database Strategy
Local SQLite database for low-latency authentication and session validation, synchronized with a centralized MySQL database for persistent storage and cross-instance consistency.

Secure File Sharing & Access Control
Role-based permissions with read/write access control lists (ACLs) and enforced validation before every file operation.

Encryption & Data Integrity
AES encryption of file content before distribution, file chunking with CRC32 validation, and concurrency locking to prevent data corruption.

Session & Authentication Management
Token-based session handling with automatic expiration and PBKDF2 password hashing.

Observability & Metrics
Real-time request tracking and performance monitoring exposed through HTTP metrics endpoints.

Terminal Emulation Layer
Simulated Unix-style command interface (ls, mkdir, cp, mv, tree, etc.) operating within user storage boundaries