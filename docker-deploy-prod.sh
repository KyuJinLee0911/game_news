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

# Stop existing containers
print_message "Stopping existing containers..."
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# Remove old images (optional)
read -p "Do you want to remove old images? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_message "Removing old images..."
    docker compose -f docker-compose.prod.yml --env-file .env.prod down --rmi all
fi

# Pull latest images
print_message "Pulling latest images from Docker Hub..."
docker compose -f docker-compose.prod.yml --env-file .env.prod pull

# Start containers
print_message "Starting containers..."
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Wait for services to be healthy
print_message "Waiting for services to be healthy..."
sleep 10

# Check container status
print_message "Checking container status..."
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

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
echo "  - View logs: docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f"
echo "  - Stop services: docker compose -f docker-compose.prod.yml --env-file .env.prod down"
echo "  - Restart services: docker compose -f docker-compose.prod.yml --env-file .env.prod restart"
echo "  - View status: docker compose -f docker-compose.prod.yml --env-file .env.prod ps"
echo ""

# Ask if user wants to see logs
read -p "Do you want to see the logs? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f
fi
