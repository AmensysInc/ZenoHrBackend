#!/bin/bash
# Quick fix script for entrypoint.sh not found error

# Navigate to project root
cd ~/zenohr

# Copy entrypoint.sh from backend to root (if it exists in backend)
if [ -f "backend/entrypoint.sh" ]; then
    cp backend/entrypoint.sh entrypoint.sh
    chmod +x entrypoint.sh
    echo "✓ Copied entrypoint.sh to root directory"
else
    echo "✗ entrypoint.sh not found in backend/ directory"
    echo "Please check your directory structure"
fi

# Verify
if [ -f "entrypoint.sh" ]; then
    echo "✓ entrypoint.sh is now in root directory"
    echo "You can now run: docker compose -f docker-compose.prod.yml up -d --build"
else
    echo "✗ Failed to copy entrypoint.sh"
fi

