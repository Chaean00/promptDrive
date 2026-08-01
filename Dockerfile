FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon
RUN jar_path=$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1) && test -n "$jar_path" && cp "$jar_path" app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
