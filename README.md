# PowerAlert Backend

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> Smart Utility Outage Management System for Sri Lanka

## 📋 Overview

PowerAlert is a modern utility notification service designed to address Sri Lanka's frequent power outages and water supply disruptions. This intelligent platform predicts and alerts citizens about scheduled and unexpected utility interruptions through timely notifications.

This repository contains the backend services that power the PowerAlert system.

## 🎬 Demo Video

<div align="center">
  <a href="https://youtu.be/bxJTsn6AoK0">
    <img src="https://img.youtube.com/vi/bxJTsn6AoK0/maxresdefault.jpg" alt="PowerAlert Demo Video" width="600" />
  </a>
  <p>Click the image above to watch the demo video</p>
</div>

## 🚀 Features

- **Real-time Outage Notifications**: Geo-targeted alerts for relevant utility disruptions
- **Multi-channel Communication**: Delivery via SMS, WhatsApp, and email
- **Restoration Estimates**: Accurate predictions with regular updates
- **Resource Mapping**: Alternative utility access points during outages
- **Community Feedback**: Verification and crowdsourced reporting
- **Historical Analytics**: Pattern recognition for better predictions
- **Multi-tenant Architecture**: Support for different utility providers
- **Advanced Geospatial Capabilities**: Location-based search and targeting

## 🔧 Tech Stack

- **Framework**: Spring Boot
- **Database**: PostgreSQL with PostGIS extension
- **Caching**: Redis
- **Authentication**: JWT with Spring Security
- **Background Processing**: Spring Scheduler
- **API Documentation**: Swagger/OpenAPI
- **Messaging**: Kafka for event processing
- **Containerization**: Docker & Kubernetes
- **CI/CD**: GitHub Actions

## 📦 Installation

### Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+ with PostGIS
- Redis 6+

### Clone Repository

```bash
git clone https://github.com/yourusername/poweralert.git
cd poweralert
```

### Configure Environment

Create an `application.properties` or use environment variables:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/poweralert
spring.datasource.username=postgres
spring.datasource.password=yourpassword

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379

# JWT Configuration
app.security.jwt.secret=your-jwt-secret-key
app.security.jwt.expiration=86400000

# SMS Gateway Configuration
sms.gateway.api.key=your-api-key
sms.gateway.sender.id=PWRALRT
```

### Build and Run

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/poweralert-backend-1.0.0.jar
```

## 🏗️ Project Structure

```
src
├── main
│   ├── java
│   │   └── com
│   │       └── poweralert
│   │           ├── config        # Configuration classes
│   │           ├── controller    # REST controllers
│   │           ├── dto           # Data Transfer Objects
│   │           ├── entity        # JPA entities
│   │           ├── exception     # Custom exceptions
│   │           ├── repository    # Data access layer
│   │           ├── security      # Authentication and authorization
│   │           ├── service       # Business logic
│   │           ├── util          # Utility classes
│   │           └── PowerAlertApplication.java
│   └── resources
│       ├── application.properties # App configuration
│       ├── db/migration           # Flyway migrations
│       └── logback.xml            # Logging configuration
└── test                           # Unit and integration tests
```

## 🌐 API Documentation

Once the application is running, you can access the API documentation at:

```
http://localhost:8080/swagger-ui.html
```

## 🛠️ Development

### Running in Development Mode

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Migrations

```bash
mvn flyway:migrate -Dflyway.configFiles=flyway.properties
```

### Running Tests

```bash
mvn test                 # Run unit tests
mvn integration-test     # Run integration tests
```

## 🔒 Security Features

- JWT-based authentication
- Role-based access control
- Two-factor authentication
- API rate limiting
- Input validation
- Audit logging
- HTTPS enforcement

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please follow our [Code of Conduct](CODE_OF_CONDUCT.md) and [Contributing Guidelines](CONTRIBUTING.md).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📚 Documentation

For comprehensive documentation, please visit our [Wiki](https://github.com/yourusername/poweralert/wiki).

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/)
- [Twilio](https://www.twilio.com/) for SMS services
- All contributors who have helped shape this project
