#!/bin/bash
#
# Deployment Database Seeding Script
# Run this manually on VPS when you need to seed or re-seed the database
#
# Usage:
#   docker exec -it zenohr-backend node /app/payroll-engine/scripts/seedDatabase.js
#
# Or from VPS:
#   cd ~/zenohr
#   docker exec -it zenohr-backend node /app/payroll-engine/scripts/seedDatabase.js
#

echo "=========================================="
echo "Payroll Database Seeding Script"
echo "=========================================="
echo ""
echo "This script will seed the payroll database with:"
echo "  ✓ Federal tax data (FICA rates, brackets, deductions)"
echo "  ✓ Pub 15-T percentage tables (official IRS 2026 data)"
echo "  ✓ State tax data (brackets, deductions for all states)"
echo "  ✓ State withholding tables (for all states with income tax)"
echo "  ✓ Database validation"
echo ""
echo "⚠️  WARNING: This will overwrite existing data!"
echo ""
read -p "Do you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Seeding cancelled."
    exit 0
fi

echo ""
echo "Starting database seeding..."
echo ""

# Run the seeding script
docker exec -it zenohr-backend node /app/payroll-engine/scripts/seedDatabase.js

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ Database seeding completed successfully"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "✗ Database seeding failed"
    echo "Check the logs above for errors"
    echo "=========================================="
    exit 1
fi

