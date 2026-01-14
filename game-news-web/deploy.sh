#!/bin/bash

echo "Installing dependencies..."
npm install

echo "Building Next.js application..."
npm run build

if [ $? -eq 0 ]; then
    echo "Build successful!"

    # Stop existing PM2 process
    pm2 stop game-news-web 2>/dev/null || true
    pm2 delete game-news-web 2>/dev/null || true

    # Start the application with PM2
    echo "Starting application with PM2..."
    pm2 start ecosystem.config.js

    echo "Application started!"
    echo "Run 'pm2 logs game-news-web' to view logs"
    echo "Run 'pm2 status' to check status"
else
    echo "Build failed!"
    exit 1
fi
