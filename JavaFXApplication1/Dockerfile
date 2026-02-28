FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy your backend JAR
COPY target/JavaFXApplication1-1.0-SNAPSHOT.jar app.jar

ENV LOAD_BALANCER_URL=http://loadbalancer:8081
ENV HOST_MANAGER_URL=http://hostmanager:8085
ENV MYSQL_URL=jdbc:mysql://mysql:3306/comp20081

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
