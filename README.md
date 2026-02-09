# Notification Service

A central notification service built with Java, Spring Boot, and Gradle using Clean Architecture principles. This service handles multiple notification types including push notifications via CleverTap and WhatsApp messages.

## Architecture

The service follows Clean Architecture principles with the following layers:

- **Domain**: Core business logic, entities, and use cases
- **Application**: Use case implementations and DTOs
- **Infrastructure**: External integrations (CleverTap, WhatsApp, Database)
- **Presentation**: REST API controllers and configuration

## Features

- **Multiple Notification Types**: Push notifications (CleverTap) and WhatsApp messages
- **Database Integration**: Stores notification requests and tracks delivery status
- **Batch Processing**: Process pending notifications from the database
- **REST API**: Full REST API with OpenAPI/Swagger documentation
- **Clean Architecture**: Modular, testable, and maintainable codebase

## Prerequisites

- Java 21+
- Gradle 8.0+
- MySQL 8.0+
- CleverTap account (for push notifications)
- WhatsApp Business API access (for WhatsApp messages)

## Setup

### Quick Start (Development)

#### Option 1: Automated Setup (Recommended)

```bash
git clone <repository-url>
cd notification-service-java

# Run setup script (Linux/Mac)
./setup-dev.sh

# Or for Windows PowerShell:
# Copy setup-dev.sh content and run manually, or use:
# gradle wrapper
# .\gradlew.bat build -x test
```

#### Option 2: Manual Setup

```bash
git clone <repository-url>
cd notification-service-java

# Build the application
./gradlew build

# Setup environment
cp .env.example .env
# Edit .env with your credentials

# Setup application properties
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties 2>/dev/null || echo "Dev properties template not found"
```

### Environment Configuration

The application supports multiple environments with different configuration profiles:

- **dev**: Development environment with verbose logging and H2 database
- **prod**: Production environment with MySQL and optimized settings
- **local**: Local development (similar to dev but with custom settings)

#### Environment Files

1. **`.env`**: Environment variables for Docker and external services
2. **`application.properties`**: Common configuration
3. **`application-{profile}.properties`**: Profile-specific configuration

### Database Setup

#### Development (with Docker)

```bash
# Start MySQL database
docker-compose -f docker-compose.dev.yml up mysql-dev -d

# Or start full development environment
docker-compose -f docker-compose.dev.yml up -d
```

#### Production

```bash
# Run production setup script
./setup-prod.sh

# Start production environment
docker-compose -f docker-compose.prod.yml up -d
```

#### Manual Database Setup

Create a MySQL database:

```sql
-- Development
CREATE DATABASE notification_db_dev;
CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'dev_password';
GRANT ALL PRIVILEGES ON notification_db_dev.* TO 'dev_user'@'localhost';

-- Production
CREATE DATABASE notification_db_prod;
CREATE USER 'prod_user'@'localhost' IDENTIFIED BY 'prod_password';
GRANT ALL PRIVILEGES ON notification_db_prod.* TO 'prod_user'@'localhost';

FLUSH PRIVILEGES;
```

The service uses Flyway for database migrations, so tables will be created automatically on startup.

### Running the Application

#### Development Mode

```bash
# With Gradle
./gradlew bootRun --args='--spring.profiles.active=dev'

# With Docker
docker-compose -f docker-compose.dev.yml up

# With Docker (application only)
docker-compose -f docker-compose.dev.yml up notification-service-dev
```

#### Production Mode

```bash
# Build and run with Docker
docker-compose -f docker-compose.prod.yml up -d

# Scale the application
docker-compose -f docker-compose.prod.yml up -d --scale notification-service-prod=3
```

#### Configuration Priority

Environment variables override properties files:
1. Command line arguments (`--property=value`)
2. Environment variables (`PROPERTY_NAME`)
3. `application-{profile}.properties`
4. `application.properties`
5. Default values in code

### Configuration Reference

#### Environment Variables (.env file)

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=notification_db_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password

# CleverTap Configuration
CLEVERTAP_DEV_ACCOUNT_ID=your_dev_clevertap_account_id
CLEVERTAP_DEV_PASSCODE=your_dev_clevertap_passcode
CLEVERTAP_DEV_REGION=in1

# WhatsApp Configuration
WHATSAPP_DEV_API_URL=https://api.whatsapp.dev.example.com
WHATSAPP_DEV_API_KEY=your_dev_whatsapp_api_key
WHATSAPP_DEV_ACCOUNT_ID=your_dev_account_id
WHATSAPP_DEV_PHONE_NUMBER_ID=your_dev_phone_number_id
```

#### Application Properties

**Common Properties** (`application.properties`):
```properties
server.port=8080
server.servlet.context-path=/api/v1
spring.jpa.hibernate.ddl-auto=validate
management.endpoints.web.exposure.include=health,info,metrics
```

**Development Properties** (`application-dev.properties`):
```properties
# Verbose logging and H2 database for development
logging.level.com.ozi.notification=DEBUG
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.show-sql=true
```

**Production Properties** (`application-prod.properties`):
```properties
# Optimized for production
logging.level.root=WARN
spring.datasource.url=jdbc:mysql://${DB_HOST}:3306/${DB_NAME}
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

## Deployment

### Docker Development

```bash
# Start full development environment
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop environment
docker-compose -f docker-compose.dev.yml down
```

### Docker Production

```bash
# Build production image
docker build -t notification-service:latest .

# Start production environment
docker-compose -f docker-compose.prod.yml up -d

# Scale application instances
docker-compose -f docker-compose.prod.yml up -d --scale notification-service-prod=2

# View logs
docker-compose -f docker-compose.prod.yml logs -f notification-service-prod

# Update deployment
docker-compose -f docker-compose.prod.yml up -d --no-deps notification-service-prod

# Backup database
docker exec notification-service-mysql-prod mysqldump -u prod_user -p notification_db_prod > backup.sql
```

### Traditional Deployment

```bash
# Build JAR
./gradlew clean build

# Run JAR directly
java -jar build/libs/notification-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Run as service (Linux)
sudo ln -s /path/to/notification-service.jar /etc/init.d/notification-service
sudo service notification-service start
```

### Production Checklist

- [ ] Environment variables configured
- [ ] SSL certificates installed
- [ ] Database backups configured
- [ ] Monitoring and alerting set up
- [ ] Load balancer configured (if scaling)
- [ ] Log aggregation configured
- [ ] Security scanning completed
- [ ] Performance testing completed

## Monitoring & Security

### Health Checks

```bash
# Application health
curl http://localhost:8080/api/v1/actuator/health

# Database health
curl http://localhost:8080/api/v1/actuator/health/db

# Metrics
curl http://localhost:8080/api/v1/actuator/metrics
```

### Security Considerations

- **API Keys**: Store in secure credential management system
- **Database Credentials**: Use strong passwords, rotate regularly
- **Network Security**: Configure firewalls and VPCs
- **SSL/TLS**: Enable HTTPS in production
- **Rate Limiting**: Implement API rate limiting
- **Audit Logging**: Enable comprehensive audit logs
- **Dependency Updates**: Regularly update dependencies for security patches

### Log Aggregation

```bash
# View application logs
docker-compose -f docker-compose.prod.yml logs -f notification-service-prod

# View all service logs
docker-compose -f docker-compose.prod.yml logs -f

# Follow logs with timestamps
docker-compose -f docker-compose.prod.yml logs -f -t
```

### Performance Monitoring

- **JVM Metrics**: Available at `/actuator/metrics/jvm.*`
- **HTTP Metrics**: Available at `/actuator/metrics/http.*`
- **Database Metrics**: Available at `/actuator/metrics/r2dbc.*`
- **Custom Metrics**: Implement Micrometer metrics for business logic

### Backup & Recovery

```bash
# Database backup
docker exec notification-service-mysql-prod mysqldump \
  -u prod_user \
  -p"$DB_PASSWORD" \
  notification_db_prod > backup_$(date +%Y%m%d_%H%M%S).sql

# Application logs backup
docker cp notification-service-prod:/app/logs ./backups/logs/

# Configuration backup
tar -czf backup_config_$(date +%Y%m%d_%H%M%S).tar.gz \
  secrets/ \
  nginx/ \
  docker-compose.prod.yml \
  .env.prod
```

### 4. Run the Service

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The service will start on `http://localhost:8080`

## API Documentation

Once running, access the API documentation at:
- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v1/v3/api-docs

## API Endpoints

### Send Notification
```http
POST /api/v1/notifications/send
Content-Type: application/json

{
  "type": "PUSH",
  "recipient": "user@example.com",
  "title": "Welcome!",
  "message": "Welcome to our platform",
  "data": {
    "userId": "12345"
  },
  "deepLink": "https://app.example.com/welcome",
  "priority": "NORMAL"
}
```

### Get Notification by ID
```http
GET /api/v1/notifications/{id}
```

### Get Notifications by Status
```http
GET /api/v1/notifications/status/{status}
```

Status values: `PENDING`, `PROCESSING`, `SENT`, `FAILED`, `CANCELLED`

### Get Notifications by Recipient
```http
GET /api/v1/notifications/recipient/{recipient}
```

### Process Pending Notifications
```http
POST /api/v1/notifications/process-pending
```

## Notification Types

### Push Notifications (CleverTap)
- **Type**: `PUSH`
- **Recipient**: User identifier (email, user ID, etc.)
- **Integration**: Uses CleverTap External Trigger API
- **Features**: Deep links, custom data payload

### WhatsApp Messages
- **Type**: `WHATSAPP`
- **Recipient**: Phone number in international format
- **Integration**: WhatsApp Business API (configurable)
- **Features**: Text messages with deep links

## Database Schema

The service uses a `notifications` table to store notification requests:

```sql
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    title TEXT,
    message TEXT,
    data JSON,
    deep_link TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    metadata JSON
);
```

## Monitoring

The service includes Spring Boot Actuator endpoints:

- **Health Check**: http://localhost:8080/api/v1/actuator/health
- **Metrics**: http://localhost:8080/api/v1/actuator/metrics
- **Info**: http://localhost:8080/api/v1/actuator/info

## Development

### Running Tests
```bash
./gradlew test
```

### Code Coverage
```bash
./gradlew jacocoTestReport
```

Coverage reports are generated in `build/reports/jacoco/`

### Code Formatting
```bash
./gradlew spotlessApply
```

## Configuration Reference

### CleverTap Settings
- `notification.clevertap.account-id`: Your CleverTap Account ID
- `notification.clevertap.passcode`: Your CleverTap Passcode
- `notification.clevertap.region`: CleverTap region (in1, us1, eu1, sg1)
- `notification.clevertap.base-url`: Auto-generated from region

### WhatsApp Settings
- `notification.whatsapp.api-url`: WhatsApp Business API endpoint
- `notification.whatsapp.api-key`: API authentication key
- `notification.whatsapp.account-id`: WhatsApp Business Account ID
- `notification.whatsapp.phone-number-id`: WhatsApp Phone Number ID

### Database Settings
- `spring.datasource.url`: Database JDBC URL
- `spring.datasource.username`: Database username
- `spring.datasource.password`: Database password

## Troubleshooting

### CleverTap Integration Issues
1. Verify Account ID and Passcode in CleverTap dashboard
2. Check region setting matches your CleverTap account
3. Ensure the recipient identifier exists in CleverTap

### WhatsApp Integration Issues
1. Verify API URL and credentials
2. Check phone number format (international format required)
3. Ensure WhatsApp Business API access is properly configured

### Database Issues
1. Verify database connection settings
2. Check Flyway migrations have run successfully
3. Ensure database user has proper permissions

## Contributing

1. Follow Clean Architecture principles
2. Write tests for new features
3. Update API documentation for new endpoints
4. Ensure code formatting with Spotless

## License

This project is licensed under the MIT License.