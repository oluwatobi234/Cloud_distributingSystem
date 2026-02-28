FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/HostManager-1.0.jar app.jar
EXPOSE 8085
CMD ["java", "-jar", "app.jar"]