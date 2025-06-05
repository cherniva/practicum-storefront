# Storefront Web Application

A web storefront application built with Spring Boot (3.4.5), using a reactive stack with Spring WebFlux and R2DBC. Written in Java 21, and built with Maven. Uses a MySQL database for production and H2 for testing.

## 🚀 Technologies

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring WebFlux** - Reactive web framework
- **Spring Data R2DBC** - Reactive database access
- **MySQL** - Production database
- **H2 Database** - In-memory database for testing and docker
- **Thymeleaf**
- **JUnit 5**
- **Lombok**
- **Maven**
- **Project Reactor** - Reactive streams implementation

## 📋 Prerequisites

- Java 21 or later
- MySQL 8.0 or later
- Maven 3.6 or later

## 🛠️ Setup

### Running from Console

1. Clone the repository
2. Configure your MySQL database connection in `src/main/resources/application.properties`
3. Build the application using Maven:
   ```bash
   ./mvnw clean install
   ```
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or use the generated JAR:
   ```bash
   java -jar target/storefront-0.0.1-SNAPSHOT.jar

## 🧪 Testing

Run the tests with:

```bash
./mvnw test
```

The test environment uses:
- H2 in-memory database
- Test-specific configuration in `src/test/resources/application-test.properties`
- Spring Boot's test support
- Reactor Test for testing reactive streams

## 📦 Project Structure

```
src/
├── main/
│   ├── java/          # Java source code
│   ├── resources/     # Configuration files
└── test/             # Test source code
```

## 🔧 Configuration

The application uses Spring Boot's configuration system. Key configuration files:
- `src/main/resources/application.properties` - Main configuration
- `src/test/resources/application-test.properties` - Test configuration