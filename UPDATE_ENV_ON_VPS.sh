#!/bin/bash

echo "================================================================="
echo "Updating .env file on VPS"
echo "================================================================="

cd ~/zenohr || exit 1

# Backup current .env
cp .env .env.backup

# Update REACT_APP_API_URL to use HTTPS
sed -i 's|REACT_APP_API_URL=http://zenopayhr.com/api|REACT_APP_API_URL=https://zenopayhr.com/api|' .env

echo "✅ Updated REACT_APP_API_URL to use HTTPS"
echo ""
echo "Current .env values:"
cat .env
echo ""
echo "================================================================="
echo "Next steps:"
echo "1. Update docker-compose.yml to use environment variables"
echo "2. Restart containers: docker compose down && docker compose up -d --build"
echo "================================================================="

