#!/bin/bash

echo "================================================================="
echo "Rebuilding Frontend with Latest Changes"
echo "================================================================="

cd ~/zenohr || exit 1

# Rebuild frontend container
echo "Rebuilding frontend container..."
docker compose up -d --build zenohr-frontend

echo ""
echo "Waiting for frontend to be ready..."
sleep 10

echo ""
echo "Checking frontend container status..."
docker ps | grep zenohr-frontend

echo ""
echo "================================================================="
echo "Frontend rebuild complete!"
echo "================================================================="
echo ""
echo "The frontend now includes the fix that allows EMPLOYEE, PROSPECT,"
echo "and HR_MANAGER to login even if the user-company fetch returns 403."
echo ""
echo "Try logging in again with sai@gmail.com"
echo "================================================================="

