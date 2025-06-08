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
- **SpringDoc OpenAPI** - API documentation
- **Redis** - Caching and session management

## 📋 Prerequisites

- Java 21 or later
- MySQL 8.0 or later
- Maven 3.6 or later
- Redis 7.0 or later (for local development)
- Docker and Docker Compose (for containerized setup)

## 🏗️ Project Modules

The project consists of two main modules:

1. **storefront** - Main storefront application
   - Web interface and user interactions
   - Product catalog and shopping cart
   - Order management
   - Integration with payment module

2. **payment** - Payment processing module
   - Payment gateway 

## 🛠️ Setup

### Running with Maven

1. Clone the repository
2. Configure your MySQL database connection in `src/main/resources/application.properties`
3. Ensure Redis is running on the default port (6379)
4. Build all modules using Maven:
   ```bash
   ./mvnw clean install
   ```
5. Start both modules:
   ```bash
   # Start the payment module
   cd payment
   ./mvnw spring-boot:run

   # In a separate terminal, start the storefront module
   cd storefront
   ./mvnw spring-boot:run
   ```

### Running with Docker

1. Build and start all services using Docker Compose:
   ```bash
   docker-compose up --build
   ```
   This will start:
   - MySQL database
   - Redis cache
   - Payment module
   - Storefront module

2. Access the applications:
   - Storefront: `http://localhost:8080`
   - Payment API: `http://localhost:8081`
   - OpenAPI documentation: 
     - Payment: `http://localhost:8081/swagger-ui.html`

## 📚 API Documentation

Both modules use SpringDoc OpenAPI for API documentation. You can access:
- Payment Swagger UI at `http://localhost:8081/swagger-ui.html`
- OpenAPI specifications:
  - Payment: `http://localhost:8081/v3/api-docs`

## 🧪 Testing

Run the tests for all modules with:

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
shop-parent/
├── storefront/        # Main storefront application
│   ├── src/
│   │   ├── main/
│   │   └── test/
└── payment/          # Payment processing module
    ├── src/
    │   ├── main/
    │   └── test/
```

## 🔧 Configuration

Each module uses Spring Boot's configuration system. Key configuration files:
- `src/main/resources/application.properties` - Main configuration
- `src/test/resources/application-test.properties` - Test configuration