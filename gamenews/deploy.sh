#!/bin/bash

echo "Building Spring Boot application..."
./gradlew clean build -x test

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "JAR file location: build/libs/gamenews-0.0.1-SNAPSHOT.jar"

    # Kill existing process
    PID=$(lsof -t -i:8080)
    if [ ! -z "$PID" ]; then
        echo "Killing existing process on port 8080 (PID: $PID)"
        kill -9 $PID
    fi

    # Start the application
    echo "Starting application..."
    nohup java -jar build/libs/gamenews-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

    echo "Application started! Check app.log for logs."
    echo "PID: $!"
else
    echo "Build failed!"
    exit 1
fi
