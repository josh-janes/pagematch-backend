FROM gradle:8.10.1-jdk17 AS build

WORKDIR /home/gradle/project
COPY . .

RUN gradle clean bootJar --no-daemon

# Stage 2: Run
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/bookrec.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]