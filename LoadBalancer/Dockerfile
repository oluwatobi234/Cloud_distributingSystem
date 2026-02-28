# Use Temurin JRE 11
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/load-balancer-1.0.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]