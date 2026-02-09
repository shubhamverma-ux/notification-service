#!/bin/bash

# Production Environment Setup Script
# This script helps set up the production environment for the Notification Service

set -e

echo "🚀 Setting up Notification Service - Production Environment"
echo "=========================================================="

# Check if required tools are installed
echo "📋 Checking system requirements..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker is required for production deployment"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is required for production deployment"
    exit 1
fi

echo "✅ Docker and Docker Compose found"

# Create necessary directories
echo "📁 Creating production directories..."
mkdir -p secrets
mkdir -p logs
mkdir -p config
mkdir -p nginx/ssl
mkdir -p nginx/logs
chmod 700 secrets

# Setup secrets (these should be created securely)
echo "🔐 Setting up production secrets..."
echo "⚠️  IMPORTANT: Replace these with actual secure values!"

if [ ! -f "secrets/mysql_root_password.txt" ]; then
    read -p "Enter MySQL root password: " -s MYSQL_ROOT_PASS
    echo "$MYSQL_ROOT_PASS" > secrets/mysql_root_password.txt
    echo "✅ MySQL root password set"
else
    echo "ℹ️  MySQL root password already exists"
fi

if [ ! -f "secrets/mysql_password.txt" ]; then
    read -p "Enter MySQL user password: " -s MYSQL_USER_PASS
    echo "$MYSQL_USER_PASS" > secrets/mysql_password.txt
    echo "✅ MySQL user password set"
else
    echo "ℹ️  MySQL user password already exists"
fi

if [ ! -f "secrets/whatsapp_api_key.txt" ]; then
    read -p "Enter WhatsApp API key: " -s WHATSAPP_API_KEY
    echo "$WHATSAPP_API_KEY" > secrets/whatsapp_api_key.txt
    echo "✅ WhatsApp API key set"
else
    echo "ℹ️  WhatsApp API key already exists"
fi

# Setup environment variables
echo "🔧 Setting up environment variables..."
cat > .env.prod << EOF
# Production Environment Variables
# Copy values from your secure credential store

# Database
DB_HOST=mysql-prod
DB_PORT=3306
DB_NAME=notification_db_prod
DB_USERNAME=prod_user
# DB_PASSWORD is loaded from secrets

# CleverTap Production
CLEVERTAP_PROD_ACCOUNT_ID=your_prod_clevertap_account_id
CLEVERTAP_PROD_PASSCODE=your_prod_clevertap_passcode
CLEVERTAP_PROD_REGION=in1

# WhatsApp Production
WHATSAPP_PROD_API_URL=https://graph.facebook.com/v18.0/your_prod_phone_number_id/messages
# WHATSAPP_PROD_API_KEY is loaded from secrets
WHATSAPP_PROD_ACCOUNT_ID=your_prod_whatsapp_business_account_id
WHATSAPP_PROD_PHONE_NUMBER_ID=your_prod_phone_number_id

# Redis
REDIS_PASSWORD=your_secure_redis_password

# SSL (if using HTTPS)
# SSL_KEYSTORE_PASSWORD=your_keystore_password
# SSL_KEY_ALIAS=your_key_alias

# Monitoring
ALLOWED_ORIGINS=https://yourdomain.com
EOF

echo "✅ Created .env.prod file"
echo "⚠️  Please edit .env.prod with your actual production values"

# Setup Nginx configuration (basic)
echo "🌐 Setting up Nginx configuration..."
cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # Logging
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log;

    # Basic settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;

    # Upstream for notification service
    upstream notification_service {
        server notification-service-prod:8080;
    }

    server {
        listen 80;
        server_name localhost;

        # Security headers
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }

        # Main application
        location / {
            proxy_pass http://notification_service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # Timeout settings
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }

        # Static files (if any)
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # HTTPS server (uncomment and configure SSL)
    # server {
    #     listen 443 ssl http2;
    #     server_name yourdomain.com;
    #
    #     ssl_certificate /etc/nginx/ssl/cert.pem;
    #     ssl_certificate_key /etc/nginx/ssl/key.pem;
    #
    #     # SSL configuration
    #     ssl_protocols TLSv1.2 TLSv1.3;
    #     ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    #     ssl_prefer_server_ciphers off;
    #
    #     location / {
    #         proxy_pass http://notification_service;
    #         # ... same proxy settings as above
    #     }
    # }
}
EOF

echo "✅ Created Nginx configuration"

# Build the application
echo "🔨 Building production Docker image..."
if [ -f "Dockerfile" ]; then
    docker build -t notification-service:latest .
    echo "✅ Docker image built successfully"
else
    echo "⚠️  Dockerfile not found"
fi

echo ""
echo "🎉 Production environment setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env.prod with your actual production values"
echo "2. Set up SSL certificates in nginx/ssl/ directory"
echo "3. Review and customize nginx/nginx.conf"
echo "4. Test the deployment:"
echo "   docker-compose -f docker-compose.prod.yml config"
echo "   docker-compose -f docker-compose.prod.yml up -d --scale notification-service-prod=2"
echo "5. Monitor logs:"
echo "   docker-compose -f docker-compose.prod.yml logs -f"
echo ""
echo "Security reminders:"
echo "- Change all default passwords"
echo "- Use strong, unique passwords for all services"
echo "- Set up proper SSL/TLS certificates"
echo "- Configure firewall rules"
echo "- Regularly update Docker images"
echo "- Monitor logs and metrics"
echo ""