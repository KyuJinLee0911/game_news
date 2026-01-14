#!/bin/bash

PID=$(lsof -t -i:8080)

if [ ! -z "$PID" ]; then
    echo "Stopping application (PID: $PID)..."
    kill -9 $PID
    echo "Application stopped!"
else
    echo "No application running on port 8080"
fi
