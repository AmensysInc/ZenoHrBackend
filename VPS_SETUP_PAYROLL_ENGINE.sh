#!/bin/bash
# Setup script to ensure payroll-engine is in the backend repo on VPS

cd ~/zenohr

# Check if payroll-engine exists in backend
if [ ! -d "backend/payroll-engine" ]; then
    echo "payroll-engine not found in backend/. Checking root..."
    
    # If it's at root level, we need to move it or copy it
    if [ -d "payroll-engine" ]; then
        echo "Found payroll-engine at root. Copying to backend/..."
        cp -r payroll-engine backend/
    else
        echo "ERROR: payroll-engine not found. Please ensure it's in the backend repo."
        exit 1
    fi
fi

echo "✓ payroll-engine is in backend/payroll-engine"
echo "You can now run: docker compose -f docker-compose.prod.yml up -d --build"

