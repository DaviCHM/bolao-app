# --- Estagio 1: build do frontend (React + Vite) ---
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Estagio 2: build do backend (Spring Boot), servindo o frontend como estatico ---
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static/
RUN mvn -q package -DskipTests

# --- Estagio 3: imagem final enxuta, apenas JRE + jar ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
