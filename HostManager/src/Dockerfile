FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/file-storage-1.0.jar app.jar
RUN mkdir -p /storage
EXPOSE 8085
CMD ["java", "-jar", "app.jar"]
