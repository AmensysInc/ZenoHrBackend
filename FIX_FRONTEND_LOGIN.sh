#!/bin/bash

echo "================================================================="
echo "Fixing Frontend Login Issue"
echo "================================================================="

cd ~/zenohr || exit 1

# Rebuild frontend with latest code
echo "Step 1: Rebuilding frontend container..."
docker compose up -d --build zenohr-frontend

echo ""
echo "Waiting for frontend to build and start..."
sleep 30

echo ""
echo "Step 2: Checking frontend container status..."
docker ps | grep zenohr-frontend

echo ""
echo "Step 3: Checking frontend logs for errors..."
docker logs zenohr-frontend --tail 20

echo ""
echo "================================================================="
echo "Frontend rebuild complete!"
echo "================================================================="
echo ""
echo "The frontend now includes the fix that allows EMPLOYEE to login"
echo "even if the user-company endpoint returns 403."
echo ""
echo "Try logging in again with sai@gmail.com"
echo "================================================================="

