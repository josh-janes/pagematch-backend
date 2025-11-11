#FROM gradle:8.10.1-jdk17 AS build
#
#WORKDIR /home/gradle/project
#COPY . .
#
#RUN gradle clean bootJar --no-daemon
#
## Stage 2: Run
#FROM eclipse-temurin:17-jdk-focal
#WORKDIR /app
#COPY --from=build /home/gradle/project/build/libs/PageMatch-0.0.1.jar app.jar

##ENV SPRING_PROFILES_ACTIVE=prod


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Copy the pre-built JAR from your local machine into the image.
# IMPORTANT: The path on the left ('build/libs/PageMatch-0.0.1.jar') must
# match the location of your JAR file relative to this Dockerfile.
COPY build/libs/PageMatch-0.0.1.jar app.jar

EXPOSE 5000

ENV PORT=5000

# Entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]