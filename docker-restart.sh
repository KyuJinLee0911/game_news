#!/bin/bash

echo "Restarting all containers..."
docker compose restart

echo "Containers restarted successfully!"
docker compose ps
