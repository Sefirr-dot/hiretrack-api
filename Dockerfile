FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S hiretrack && adduser -S hiretrack -G hiretrack
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p uploads && chown -R hiretrack:hiretrack /app
USER hiretrack
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
