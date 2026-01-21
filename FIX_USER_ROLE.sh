#!/bin/bash

echo "================================================================="
echo "Fixing User Role in Database"
echo "================================================================="

cd ~/zenohr || exit 1

# Fix the user role
echo "Updating user role to EMPLOYEE..."
docker exec zenohr-mysql mysql -uroot -p'RootPassword@123!' quickhrms << 'MYSQL_EOF'
UPDATE user SET role = 'EMPLOYEE' WHERE email = 'sai@gmail.com';
SELECT id, email, role FROM user WHERE email = 'sai@gmail.com';
MYSQL_EOF

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ User role updated successfully!"
    echo ""
    echo "Now the user should be able to:"
    echo "1. Log in successfully"
    echo "2. Access the /user-company endpoint"
    echo ""
    echo "Try logging in again with sai@gmail.com"
else
    echo ""
    echo "❌ Failed to update user role"
    exit 1
fi

echo "================================================================="

