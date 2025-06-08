FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY storefront/pom.xml storefront/
COPY payment/pom.xml payment/
RUN mvn dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads && \
    chmod 777 /app/uploads

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="docker"
ENV UPLOAD_DIR="/app/uploads"

COPY --from=build /app/storefront/target/*.jar storefront.jar
COPY --from=build /app/payment/target/*.jar payment.jar

EXPOSE 8080 8081

CMD ["java", "-jar", "storefront.jar"] 