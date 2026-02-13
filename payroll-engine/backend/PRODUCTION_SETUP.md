# Production Setup Guide

## Database Seeding

The payroll engine requires tax tables to be pre-seeded before use. **Auto-import has been disabled in production mode** for security and performance.

### Required Tables

#### Critical (Must Have Data):
- `tax_years` - Tax year definitions
- `fica_rates` - FICA rates (Social Security, Medicare)
- `federal_deductions` - Federal standard deductions
- `pub15t_percentage_tables` - IRS Pub 15-T percentage tables (minimum 100 rows)

#### Important (Should Have Data):
- `federal_brackets` - Federal tax brackets
- `state_brackets` - State tax brackets
- `state_deductions` - State standard deductions
- `state_withholding_tables` - State withholding tables
- `state_payroll_taxes` - State payroll taxes
- `local_taxes` - Local tax rates

### Seeding Process

#### Option 1: Run Seeding Script (Recommended)

```bash
# Inside the Docker container or on the server
cd /app/payroll-engine
node scripts/seedDatabase.js
```

#### Option 2: Manual Seeding

```bash
# 1. Initialize tables
node -e "require('./database/init').initDatabase().then(() => console.log('Tables created'))"

# 2. Seed federal data
node -e "require('./database/seedData').seedFederalData2026().then(() => console.log('Federal data seeded'))"

# 3. Import Pub 15-T tables
node -e "require('./scripts/importPub15TComplete').importCompletePub15TTables().then(() => console.log('Pub 15-T imported'))"
```

### Validation

After seeding, validate the database:

```bash
node -e "require('./database/validateTables').validateTables(2026).then(v => console.log(JSON.stringify(v, null, 2)))"
```

### Docker Integration

Add to your `entrypoint.sh` or Dockerfile:

```bash
# Seed database on first run (only if database is empty)
if [ ! -f /app/payroll-engine/database/tax_data.db ] || [ ! -s /app/payroll-engine/database/tax_data.db ]; then
    echo "Seeding payroll database..."
    cd /app/payroll-engine && node scripts/seedDatabase.js
fi
```

## Security Features

### 1. No Auto-Import
- Tables must be pre-seeded during deployment
- Prevents accidental data corruption
- Ensures consistent production state

### 2. Strict Validation
- Fails fast if critical tables are missing
- Prevents calculations with incomplete data
- Clear error messages for missing tables

### 3. No Stack Trace Exposure
- Internal errors are logged server-side only
- User-facing errors are sanitized
- Prevents information leakage

## Performance Considerations

### Current Architecture: Process Spawning

Currently, each calculation spawns a new Node.js process:
- **Pros**: Isolation, no memory leaks, simple deployment
- **Cons**: Process startup overhead (~100-200ms per call)

### Future: Persistent Node Service (Recommended)

For high-volume production, consider switching to a persistent Node.js service:

1. **Create a Node.js HTTP service** (`payroll-service.js`):
   ```javascript
   const express = require('express');
   const { calculatePaystub } = require('./services/payrollService');
   
   const app = express();
   app.use(express.json());
   
   app.post('/calculate', async (req, res) => {
       try {
           const result = await calculatePaystub(req.body);
           res.json(result);
       } catch (error) {
           res.status(500).json({ error: error.message });
       }
   });
   
   app.listen(9005);
   ```

2. **Update Java service** to use HTTP instead of process spawning:
   ```java
   // Use RestTemplate or WebClient to call http://localhost:9005/calculate
   ```

3. **Benefits**:
   - Faster response times (no process startup)
   - Better resource utilization
   - Connection pooling
   - Health checks and monitoring

4. **Trade-offs**:
   - More complex deployment (separate service)
   - Memory management (long-running process)
   - Requires process monitoring

## Monitoring

Monitor these metrics:
- Database validation status
- Calculation success/failure rates
- Response times
- Memory usage (if using persistent service)

## Troubleshooting

### Error: "Database validation failed"
- Run `node scripts/seedDatabase.js` to seed missing tables
- Check database file permissions
- Verify SQLite database is not corrupted

### Error: "Payroll calculation failed"
- Check server logs for detailed error (not exposed to client)
- Verify all required tables have data
- Check input data format

### Slow Calculations
- Consider switching to persistent Node service
- Check database file location (should be on fast storage)
- Monitor system resources

