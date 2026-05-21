# Passo 1: Compilar o Java usando o Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Passo 2: Rodar a aplicação com o Java leve
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copia o arquivo .jar gerado automaticamente independente do nome dele
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]