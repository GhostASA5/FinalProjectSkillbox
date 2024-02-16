# Используем образ OpenJDK
FROM openjdk:17

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR-файл вашего приложения в контейнер
COPY src/main/java /app

# Копируем JAR-файл вашего приложения в контейнер
COPY target/SearchEngine-1.0-SNAPSHOT.jar /app/your-app.jar

# Устанавливаем переменные окружения для подключения к MySQL
ENV DB_URL=jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
ENV DB_USERNAME=root
ENV DB_PASSWORD=svist7890


# Ожидаем подключения к MySQL перед запуском приложения
CMD ["sh", "-c", "while ! nc -z mysql 3306; do sleep 2; done && java -jar your-app.jar"]
