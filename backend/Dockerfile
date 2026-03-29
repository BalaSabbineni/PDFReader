# ── Build stage ───────────────────────────────────────────────────────────────
FROM amazoncorretto:17-alpine AS build

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM amazoncorretto:17-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 50000

ENTRYPOINT ["java", "-jar", "app.jar"]
