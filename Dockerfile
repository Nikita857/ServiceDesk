# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

#Создаем директории для загрзуки вложений вручную
RUN mkdir -p /app/uploads && chmod -R 7777 /app/uploads

# Кешируем зависимости Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Копируем исходники и собираем fat jar
COPY src src
RUN ./gradlew clean bootJar --no-daemon


# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Best practice: non-root user
RUN useradd -m spring
USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
