# ---------------------------------------------------------------------------
# Multi-stage build for the Slotify backend.
#   docker build -t slotify-backend .
#   docker run -p 8080:8080 --env-file .env slotify-backend
# ---------------------------------------------------------------------------

# ---- Stage 1: build the fat jar -------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Cache dependencies first so that source changes do not re-download them.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B clean package -DskipTests

# ---- Stage 2: minimal runtime image ---------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S slotify && adduser -S slotify -G slotify
USER slotify

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
