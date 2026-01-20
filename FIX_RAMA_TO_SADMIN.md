# Fix Rama User to SADMIN

This guide helps you convert `rama.k@amensys.com` from ADMIN to SADMIN and remove company assignments.

## Option 1: Using the Script (Recommended)

1. **Copy the script to your VPS:**
   ```bash
   # On your local machine, the script is already created
   # Copy it to VPS or create it there
   ```

2. **On your VPS, navigate to the zenohr directory:**
   ```bash
   cd ~/zenohr
   ```

3. **Make sure your .env file has DB credentials:**
   ```bash
   cat .env | grep DB_
   ```

4. **Run the script:**
   ```bash
   chmod +x fix_rama_to_sadmin.sh
   ./fix_rama_to_sadmin.sh
   ```

   Or if you need to set DB_PASSWORD manually:
   ```bash
   export DB_PASSWORD=your_mysql_password
   ./fix_rama_to_sadmin.sh
   ```

5. **Restart the backend:**
   ```bash
   docker compose restart zenohr-backend
   ```

## Option 2: Using SQL Directly

1. **Connect to MySQL container:**
   ```bash
   docker exec -it zenohr-mysql mysql -uroot -p
   # Enter your MySQL root password
   ```

2. **Run the SQL commands:**
   ```sql
   USE quickhrms;
   
   -- Update to SADMIN
   UPDATE `user` 
   SET ROLE = 'SADMIN' 
   WHERE EMAIL = 'rama.k@amensys.com';
   
   -- Remove company assignments
   DELETE FROM user_company 
   WHERE userId IN (
       SELECT id FROM `user` WHERE EMAIL = 'rama.k@amensys.com'
   );
   
   -- Verify
   SELECT EMAIL, ROLE FROM `user` WHERE EMAIL = 'rama.k@amensys.com';
   ```

3. **Restart the backend:**
   ```bash
   docker compose restart zenohr-backend
   ```

## Option 3: Quick One-Liner

```bash
# Update role
docker exec -i zenohr-mysql mysql -uroot -p"YOUR_PASSWORD" quickhrms -e "UPDATE \`user\` SET ROLE = 'SADMIN' WHERE EMAIL = 'rama.k@amensys.com';"

# Remove company assignments
docker exec -i zenohr-mysql mysql -uroot -p"YOUR_PASSWORD" quickhrms -e "DELETE FROM user_company WHERE userId IN (SELECT id FROM \`user\` WHERE EMAIL = 'rama.k@amensys.com');"

# Restart backend
docker compose restart zenohr-backend
```

## Verification

After running the script:

1. **Check the user in database:**
   ```bash
   docker exec -i zenohr-mysql mysql -uroot -p"YOUR_PASSWORD" quickhrms -e "SELECT EMAIL, ROLE FROM \`user\` WHERE EMAIL = 'rama.k@amensys.com';"
   ```
   Should show: `ROLE = SADMIN`

2. **Check company assignments:**
   ```bash
   docker exec -i zenohr-mysql mysql -uroot -p"YOUR_PASSWORD" quickhrms -e "SELECT COUNT(*) FROM user_company WHERE userId IN (SELECT id FROM \`user\` WHERE EMAIL = 'rama.k@amensys.com');"
   ```
   Should show: `0`

3. **Log out and log back in:**
   - You should now be SADMIN
   - No company selector should appear
   - You should see all companies and all employees

## Troubleshooting

### Script fails with "DB_PASSWORD not found"
- Make sure your `.env` file exists in `~/zenohr/`
- Or export it: `export DB_PASSWORD=your_password`

### MySQL container not found
- Start it: `docker compose up -d zenohr-mysql`
- Wait a few seconds for it to be ready

### Permission denied
- Make script executable: `chmod +x fix_rama_to_sadmin.sh`

