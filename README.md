# Virtual Power Plant (VPP) API

The Virtual Power Plant (VPP) API is a Spring Boot application for managing battery data, including creating batteries and searching for batteries by postcode range and capacity. The API provides a RESTful interface, uses PostgreSQL for persistence, and leverages Testcontainers for integration testing.

## Table of Contents
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Architectural Decisions](#architectural-decisions)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)
- [Contributing](#contributing)

## Features
- Create multiple batteries asynchronously via a POST endpoint.
- Search batteries by postcode range and capacity filters, returning sorted names, total capacity, and average capacity.
- Input validation for battery data and search parameters.
- Exception handling for database errors and invalid inputs.
- Integration with PostgreSQL for data persistence.
- Unit and integration tests with >70% code coverage using JUnit, Mockito, and Testcontainers.

## Prerequisites
- **Java 17**: Ensure JDK 17 is installed.
- **Maven 3.9.x**: For building and dependency management.
- **PostgreSQL 16**: For the database (local or via Docker).
- **Docker**: For running Testcontainers during tests.
- **IntelliJ IDEA Community Edition** (optional): For development and debugging.

## Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd vpp
   ```

2. **Install Java 17**:
   - Download and install JDK 17 from [Adoptium](https://adoptium.net/) or another provider.
   - Verify installation:
     ```bash
     java -version
     ```
     Expected output:
     ```
     openjdk 17.0.12 2024-07-16
     ```

3. **Install Maven**:
   - Download Maven from [Apache Maven](https://maven.apache.org/download.cgi).
   - Add Maven to your PATH and verify:
     ```bash
     mvn -version
     ```

4. **Set Up PostgreSQL**:
   - Install PostgreSQL locally or use Docker:
     ```bash
     docker run -d --name postgres -p 5432:5432 -e POSTGRES_USER=rore_user -e POSTGRES_PASSWORD=R0Re-1nT -e POSTGRES_DB=rore_db postgres:16-alpine
     ```
   - Verify connection:
     ```bash
     psql -h localhost -U rore_user -d rore_db
     ```

5. **Configure Application**:
   - Edit `src/main/resources/application.properties`:
     ```properties
     spring.application.name=vpp
     spring.datasource.url=jdbc:postgresql://localhost:5432/rore_db
     spring.datasource.username=rore_user
     spring.datasource.password=R0Re-1nT
     spring.datasource.driver-class-name=org.postgresql.Driver
     spring.jpa.hibernate.ddl-auto=update
     spring.jpa.show-sql=true
     spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
     spring.datasource.hikari.maximum-pool-size=50
     spring.datasource.hikari.minimum-idle=10
     spring.datasource.hikari.max-lifetime=1800000
     spring.datasource.hikari.idle-timeout=300000
     spring.datasource.hikari.connection-timeout=10000
     logging.level.com.rore_int.vpp=INFO
     ```
   - For tests, ensure `src/test/resources/application.properties` exists:
     ```properties
     spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb
     spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
     spring.datasource.hikari.maximum-pool-size=5
     spring.datasource.hikari.minimum-idle=2
     spring.datasource.hikari.max-lifetime=300000
     spring.datasource.hikari.connection-timeout=10000
     spring.jpa.hibernate.ddl-auto=create-drop
     spring.jpa.show-sql=true
     spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
     testcontainers.reuse.enable=true
     ```

6. **Install Docker**:
   - Install Docker.
   - Start Docker and verify:
     ```bash
     docker --version
     ```

7. **Build the Project**:
   ```bash
   mvn clean install
   ```

## Running the Application
1. **Start the Application**:
   ```bash
   mvn spring-boot:run
   ```
   The API will be available at `http://localhost:8080`.

2. **Test Endpoints**:
   - Create batteries:
     ```bash
     curl -X POST http://localhost:8080/api/batteries -H "Content-Type: application/json" -d '[{"name":"TestBattery","postcode":"6000","capacity":10000}]'
     ```
     Expected response:
     ```json
     [
         {
             "name": "TestBattery",
             "postcode": "6000",
             "capacity": 10000
         }
     ]
     ```
   - Search batteries:
     ```bash
     curl "http://localhost:8080/api/batteries/search?minPostcode=6000&maxPostcode=6200&minCapacity=10000&maxCapacity=60000"
     ```
     Expected response:
     ```json
     {
         "batteryNames": ["TestBattery"],
         "totalCapacity": 10000,
         "averageCapacity": 10000.0
     }
     ```

## Testing
1. **Run Unit Tests**:
   ```bash
   mvn test
   ```
   - Tests are located in `src/test/java/com/rore_int/vpp`.
   - Uses JUnit 5, Mockito for mocking, and Testcontainers for integration tests.

2. **Check Code Coverage**:
   ```bash
   mvn jacoco:report
   ```
   - View the report at `target/site/jacoco/index.html`.
   - Target coverage: >70% for `BatteryService`, `BatteryRepository`, and `BatteryController`.

3. **Integration Tests**:
   - Ensure Docker is running for Testcontainers.
   - Integration tests (e.g., `BatteryControllerPerformanceTest`) use a PostgreSQL container configured via `TestContainerConfig`.

## Architectural Decisions
1. **Layered Architecture**: Separates concerns into Controller, Service, and Repository layers for maintainability and testability.
2. **DTOs**: Used for request/response models to decouple API contracts from entity models.
3. **Validation**: Bean Validation ensures robust input checking.
4. **Exception Handling**: Global exception handler provides consistent error responses.
5. **Logging**: SLF4J with Logback logs significant events (API calls, errors).
6. **Concurrency**: `CompletableFuture.supplyAsync` for asynchronous battery creation maintains scalability for batch operations.
7. **Testing**:
    - Unit tests with Mockito for service layer.
    - Integration tests with Testcontainers for repository layer.
    - JaCoCo ensures >70% coverage.
8. **Java Streams**: Used in the GET API for sorting and computing statistics efficiently.
9. **Database**: PostgreSQL chosen for reliability and compatibility with Testcontainers.

## Troubleshooting
- **Test Failures**:
  - Check `target/surefire-reports` for detailed stack traces.
  - Run tests with debug logging:
    ```bash
    mvn test -X
    ```

- **Database Connection Issues**:
  - Ensure Docker is running for Testcontainers.
  - Check for port conflicts:
    ```bash
    lsof -i :5432
    ```
  - Restart Docker:
    ```bash
    docker-compose down
    docker-compose up -d
    ```
  - Recreate the database:
    ```bash
    psql -U postgres
    DROP DATABASE rore_db;
    CREATE DATABASE rore_db;
    \q
    ```

- **Coverage Below 70%**:
  - Add unit tests for edge cases in `BatteryController` or `BatteryService`.
  - Run coverage report:
    ```bash
    mvn jacoco:report
    ```

## Future Improvements
- Add API documentation with OpenAPI/Swagger.
- Implement database migrations with Flyway.
- Add caching for GET API to improve performance.
- Introduce authentication/authorization for secure access.

## Contributing
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/xyz`).
3. Commit changes (`git commit -m "Add xyz feature"`).
4. Push to the branch (`git push origin feature/xyz`).
5. Open a pull request.