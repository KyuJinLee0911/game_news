#!/bin/bash

set -e

echo "========================================="
echo "Game News Production Deployment Script"
echo "========================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

COMPOSE_CMD="docker compose -f docker-compose.prod.yml --env-file .env.prod --env-file gamenews/.env"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Check if .env.prod exists
if [ ! -f .env.prod ]; then
    print_error ".env.prod file not found. Please create it with DOCKER_USERNAME and TAG variables."
    print_error "Example:"
    print_error "  DOCKER_USERNAME=yourusername"
    print_error "  TAG=latest"
    exit 1
fi

# Check if gamenews/.env exists
if [ ! -f gamenews/.env ]; then
    print_error "gamenews/.env file not found. Please create it with the required variables."
    print_error "Example:"
    print_error "  DB_PASSWORD=your_secure_password"
    print_error "  ALLOWED_ORIGIN=https://your-domain.com"
    exit 1
fi

# Stop existing containers
print_message "Stopping existing containers..."
$COMPOSE_CMD down

# Remove old images (optional)
read -p "Do you want to remove old images? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_message "Removing old images..."
    $COMPOSE_CMD down --rmi all
fi

# Pull latest images
print_message "Pulling latest images from Docker Hub..."
$COMPOSE_CMD pull

# Start containers
print_message "Starting containers..."
$COMPOSE_CMD up -d

# Wait for services to be healthy
print_message "Waiting for services to be healthy..."
sleep 10

# Check container status
print_message "Checking container status..."
$COMPOSE_CMD ps

# Show logs
print_message "========================================="
print_message "Deployment completed!"
print_message "========================================="
echo ""
echo "Services:"
echo "  - Nginx (Reverse Proxy): http://localhost"
echo "  - Frontend: http://localhost:3000"
echo "  - Backend API: http://localhost:8080/api/news"
echo ""
echo "Useful commands:"
echo "  - View logs: $COMPOSE_CMD logs -f"
echo "  - Stop services: $COMPOSE_CMD down"
echo "  - Restart services: $COMPOSE_CMD restart"
echo "  - View status: $COMPOSE_CMD ps"
echo ""

# Ask if user wants to see logs
read -p "Do you want to see the logs? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    $COMPOSE_CMD logs -f
fi
