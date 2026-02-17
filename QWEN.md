# Java Template Project Context

## Project Overview

This is a modern Java template project built with Spring Boot 4.0.2, using Java 25 as the target version. It follows contemporary development practices with a focus on security, testing, and deployment automation. The project serves as a foundation for building enterprise-grade applications with features like JWT authentication, database integration, and monitoring capabilities.

## Key Technologies & Architecture

- **Framework**: Spring Boot 4.0.2
- **Language**: Java 25
- **Build Tool**: Gradle 8.10+ (with wrapper)
- **Database**: PostgreSQL (with MyBatis Plus ORM)
- **Authentication**: JWT + Spring Security
- **Testing**: JUnit 5, Testcontainers, Mockito
- **Documentation**: Swagger UI, OpenAPI 3
- **Monitoring**: Spring Boot Actuator, Prometheus
- **Caching**: Caffeine
- **Deployment**: Docker, Docker Compose

## Project Structure

```
java-template/
├── build.gradle          # Main Gradle build configuration
├── settings.gradle       # Gradle project settings
├── gradlew              # Gradle wrapper script
├── gradlew.bat          # Windows Gradle wrapper script
├── Dockerfile           # Multi-stage Docker build configuration
├── compose.yaml         # Docker Compose configuration
├── Makefile             # Convenience commands for development
├── README.md            # Project documentation
├── gradle/              # Gradle wrapper distribution
├── src/
│   ├── main/
│   │   ├── java/        # Source code
│   │   │   └── com/
│   │   │       └── github/
│   │   │           └── starhq/
│   │   │               └── template/
│   │   └── resources/   # Configuration files
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── db/      # Database migration scripts
│   │       └── mapper/  # MyBatis mapper XML files
│   └── test/            # Test source code
└── build/               # Build output directory
```

## Building and Running

### Prerequisites
- Java 25+
- Gradle 8.10+ (or use the provided gradlew wrapper)

### Development Commands

```bash
# Build the project
./gradlew clean build -x test

# Run the application locally
./gradlew bootRun --args='--spring.profiles.active=local'

# Run tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport

# Check code coverage meets minimum requirements
./gradlew check
```

### Using Makefile (Alternative)

```bash
# Run the application with hot reload (requires entr and fd)
make run

# Run tests
make test

# Build JAR
make build

# Build Docker image
make docker-build

# Run in Docker
make docker-run
```

### Docker Deployment

```bash
# Build Docker image
docker build -t java-template .

# Or using Makefile
make docker-build

# Run with Docker Compose
docker-compose up -d
```

## Configuration

The project uses a layered configuration approach:

- `application.yml`: Base configuration shared across environments
- `application-dev.yml`: Development-specific settings
- `application-prod.yml`: Production-specific settings
- Environment variables for sensitive data

Key configuration areas include:
- Database connection (PostgreSQL)
- JWT authentication settings
- MyBatis Plus ORM configuration
- Server settings (port, compression, etc.)
- Actuator endpoints for monitoring

## Development Conventions

### Code Style
- Follows Google Java Style Guide
- Uses Lombok for reducing boilerplate code
- Uses MapStruct for object mapping
- MyBatis Plus for database operations

### Testing Practices
- Unit tests with JUnit 5 and Mockito
- Integration tests with Testcontainers
- Minimum 80% code coverage requirement
- Automated testing via Gradle tasks

### Dependency Management
- Uses Gradle Version Catalogs (`libs.versions.toml`)
- Centralized dependency version management
- Regular dependency updates encouraged

### Security Features
- JWT-based authentication
- Spring Security integration
- Password hashing
- Input validation

## Special Features

1. **Multi-stage Docker Build**: Optimized Docker image with layered JAR extraction for efficient caching
2. **Health Checks**: Built-in health check endpoints for container orchestration
3. **Virtual Threads**: Enabled for improved concurrency (Java 19+ feature)
4. **SonarQube Integration**: Pre-configured for code quality analysis
5. **Database Migration**: Flyway integration for schema management
6. **Caching**: Caffeine-based caching solution
7. **API Documentation**: Auto-generated with SpringDoc OpenAPI

## Environment Setup

1. Clone the repository
2. Install Java 25+
3. Configure database connection in application properties
4. Run `./gradlew build` to download dependencies and build the project
5. Start the application with `./gradlew bootRun`

## Common Tasks

- **Running tests**: `./gradlew test`
- **Generating coverage report**: `./gradlew jacocoTestReport`
- **Building production JAR**: `./gradlew bootJar`
- **Checking code quality**: `./gradlew check`
- **Running with specific profile**: `./gradlew bootRun --args='--spring.profiles.active=dev'`