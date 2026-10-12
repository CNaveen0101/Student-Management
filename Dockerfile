FROM eclipse-temurin:17-jre-noble
WORKDIR /app
COPY /target/student-info-1.0.0.jar .
EXPOSE 8080
CMD ["java", "-jar", "student-info-1.0.0.jar"]
