#!/bin/bash

set -e

echo "========================================="
echo "Game News Docker Build & Push Script"
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

# Get Docker Hub username
read -p "Enter your Docker Hub username: " DOCKER_USERNAME
if [ -z "$DOCKER_USERNAME" ]; then
    print_error "Docker Hub username is required."
    exit 1
fi

# Get tag (default: latest)
read -p "Enter image tag (default: latest): " TAG
TAG=${TAG:-latest}

# Login to Docker Hub
print_message "Logging in to Docker Hub..."
docker login

# Build backend
print_message "Building backend image (Step 1/3)..."
docker build -t ${DOCKER_USERNAME}/game-news-backend:${TAG} ./gamenews

# Build frontend
print_message "Building frontend image (Step 2/3)..."
docker build -t ${DOCKER_USERNAME}/game-news-frontend:${TAG} ./game-news-web

# Build nginx
print_message "Building nginx image (Step 3/3)..."
docker build -t ${DOCKER_USERNAME}/game-news-nginx:${TAG} ./nginx

# Push images
print_message "Pushing images to Docker Hub..."
print_message "Pushing backend image..."
docker push ${DOCKER_USERNAME}/game-news-backend:${TAG}

print_message "Pushing frontend image..."
docker push ${DOCKER_USERNAME}/game-news-frontend:${TAG}

print_message "Pushing nginx image..."
docker push ${DOCKER_USERNAME}/game-news-nginx:${TAG}

# Create .env file for deployment
print_message "Creating .env file for deployment..."
cat > .env.prod << EOF
DOCKER_USERNAME=${DOCKER_USERNAME}
TAG=${TAG}
EOF

print_message "========================================="
print_message "Build and push completed!"
print_message "========================================="
echo ""
echo "Images pushed:"
echo "  - ${DOCKER_USERNAME}/game-news-backend:${TAG}"
echo "  - ${DOCKER_USERNAME}/game-news-frontend:${TAG}"
echo "  - ${DOCKER_USERNAME}/game-news-nginx:${TAG}"
echo ""
echo "Next steps:"
echo "  1. Copy .env.prod to your EC2 server"
echo "  2. Copy docker-compose.prod.yml to your EC2 server"
echo "  3. Run: docker compose -f docker-compose.prod.yml --env-file .env.prod up -d"
echo ""
