# Используем образ OpenJDK
FROM openjdk:19

 #Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR-файл вашего приложения в контейнер
COPY src/main/java /app
COPY target/libs /app/lib

# Копируем JAR-файл вашего приложения в контейнер
COPY target/SearchEngine.jar /app/your-app.jar

# Устанавливаем переменные окружения для подключения к MySQL
#ENV DB_URL=jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
#ENV DB_USERNAME=root
#ENV DB_PASSWORD=svist7890


# Ожидаем подключения к MySQL перед запуском приложения
CMD ["java", "-cp", ".:/app/lib/*:/app/your-app.jar", "searchengine/Application.java"]

#ARG JAR_FILE=/target/SearchEngine.jar
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]

