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
- **Spring Security** - Authentication and authorization
- **OAuth2** - Authorization framework
- **Keycloak** - Identity and access management server

## 🔐 Security & Authorization

The application implements comprehensive security using:
- **Spring Security** for authentication and authorization
- **OAuth2** authorization framework for secure API access
- **Keycloak** as the identity and access management server
- **JWT tokens** for stateless authentication

### Keycloak Integration
- Keycloak server handles user authentication and authorization
- Supports multiple authentication flows (Authorization Code, Client Credentials)
- Provides single sign-on (SSO) capabilities

## 📋 Prerequisites

- Java 21 or later
- MySQL 8.0 or later
- Maven 3.6 or later
- Redis 7.0 or later (for local development)
- Docker and Docker Compose (for containerized setup)
- **Keycloak server** (for production and full functionality)

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
4. **Set up Keycloak authorization and resource server configuration**
5. Build all modules using Maven:
   ```bash
   mvn clean install
   ```
6. Start both modules:
   ```bash
   # Start the payment module
   cd payment
   mvn spring-boot:run

   # In a separate terminal, start the storefront module
   cd storefront
   mvn spring-boot:run
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

   > ⚠️ **Important Note**: The Docker setup starts **without Keycloak server**. This means:
   > - The payment service will **NOT be available** for authentication
   > - OAuth2 authorization flows will not work
   > - You'll need to set up Keycloak separately for full functionality
   > - For development/testing, you can use the Maven setup with a local Keycloak instance

2. Access the applications:
   - Storefront: `http://localhost:8080`
   - Payment API: `http://localhost:8081` (⚠️ **Not available without Keycloak**)
   - OpenAPI documentation: 
     - Payment: `http://localhost:8081/swagger-ui.html` (⚠️ **Not available without Keycloak**)

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