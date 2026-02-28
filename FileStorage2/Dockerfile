FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/file-storage-1.0.jar app.jar
RUN mkdir -p /storage
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
