#!/bin/bash

set -e

echo "========================================="
echo "Game News Docker Deployment Script"
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

# Stop existing containers
print_message "Stopping existing containers..."
docker compose down

# Remove old images (optional)
read -p "Do you want to remove old images? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_message "Removing old images..."
    docker compose down --rmi all
fi

# Build and start containers sequentially to reduce memory usage
print_message "Building backend service (Step 1/3)..."
docker compose build backend

print_message "Building frontend service (Step 2/3)..."
docker compose build frontend

print_message "Building nginx service (Step 3/3)..."
docker compose build nginx

print_message "Starting all containers..."
docker compose up -d

# Wait for services to be healthy
print_message "Waiting for services to be healthy..."
sleep 10

# Check container status
print_message "Checking container status..."
docker compose ps

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
echo "  - View logs: docker compose logs -f"
echo "  - Stop services: docker compose down"
echo "  - Restart services: docker compose restart"
echo "  - View status: docker compose ps"
echo ""

# Ask if user wants to see logs
read -p "Do you want to see the logs? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker compose logs -f
fi
