#!/bin/bash

# Development Environment Setup Script
# This script helps set up the development environment for the Notification Service

set -e

echo "🚀 Setting up Notification Service - Development Environment"
echo "=========================================================="

# Check if Java 21 is installed
echo "📋 Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 or later is required. Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi
echo "✅ Java $JAVA_VERSION found"

# Check if Docker is available (optional for development)
if command -v docker &> /dev/null; then
    echo "✅ Docker found - you can use docker-compose for full environment setup"
else
    echo "⚠️  Docker not found - you can still run the service directly with Gradle"
fi

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p logs
mkdir -p secrets
chmod 700 secrets

# Setup environment file
echo "🔧 Setting up environment configuration..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "✅ Created .env file from template"
    echo "⚠️  Please edit .env file with your actual credentials"
else
    echo "ℹ️  .env file already exists"
fi

# Setup application properties
echo "🔧 Setting up application properties..."
if [ ! -f "src/main/resources/application-dev.properties" ]; then
    cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties 2>/dev/null || true
    echo "✅ Created application-dev.properties from template"
else
    echo "ℹ️  application-dev.properties already exists"
fi

# Build the application
echo "🔨 Building the application..."
if command -v ./gradlew &> /dev/null; then
    ./gradlew clean build -x test
    echo "✅ Application built successfully"
else
    echo "⚠️  Gradle wrapper not found. Run 'gradle wrapper' if needed."
fi

# Create sample secrets (for development only)
echo "🔐 Creating sample secrets for development..."
if [ ! -f "secrets/mysql_root_password.txt" ]; then
    echo "rootpassword" > secrets/mysql_root_password.txt
    echo "dev_password" > secrets/mysql_password.txt
    echo "dev_whatsapp_api_key" > secrets/whatsapp_api_key.txt
    echo "✅ Created sample secrets (CHANGE THESE IN PRODUCTION!)"
else
    echo "ℹ️  Secrets already exist"
fi

echo ""
echo "🎉 Development environment setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env file with your actual credentials"
echo "2. Start the database: docker-compose -f docker-compose.dev.yml up mysql-dev -d"
echo "3. Run the application: ./gradlew bootRun --args='--spring.profiles.active=dev'"
echo "4. Access the API: http://localhost:8080/api/v1"
echo "5. View API docs: http://localhost:8080/api/v1/swagger-ui.html"
echo ""
echo "For full environment with Docker:"
echo "docker-compose -f docker-compose.dev.yml up -d"
echo ""