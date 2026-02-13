# Backend Setup Guide

## Quick Start

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Initialize Database and Import All Data

```bash
npm run import-all
```

This will:
- Create database tables
- Seed federal tax data (2026)
- Import Pub 15-T percentage method tables (sample data)
- Import all state tax data (brackets, deductions, payroll taxes)
- Import local tax data (NYC, Yonkers, PA, MD, OH)

### 3. Start Server

```bash
npm start
```

Server runs on `http://localhost:3000`

## Data Import Status

### ✅ Completed
- Federal tax brackets (2026)
- Federal standard deductions (2026)
- FICA rates (2026)
- State tax brackets (all 50 states + DC)
- State standard deductions
- State payroll taxes (NJ, CA, NY, HI, RI, WA, MA, CO, OR, CT, DC)
- Local taxes (NYC, Yonkers, PA, MD, OH)

### ⚠️ Needs Complete Data
- **Pub 15-T Percentage Method Tables**: Currently has sample data. Need to import complete tables from IRS Publication 15-T (2026) for all:
  - Pay frequencies (Weekly, Bi-weekly, Semi-monthly, Monthly)
  - Filing statuses (Single, Married Jointly, Married Separately, Head of Household)
  - Step 2 checkbox variations
  
- **State Withholding Tables**: Currently falls back to bracket calculation. Need to import official state withholding tables from each state's revenue department for exact matching.

## Importing Complete Pub 15-T Tables

1. Download IRS Publication 15-T (2026) from IRS website
2. Extract percentage method tables
3. Update `backend/scripts/importPub15T.js` with complete data
4. Run: `node scripts/importPub15T.js`

## Importing State Withholding Tables

For each state:
1. Download official withholding tables from state revenue department
2. Create import script or update `backend/scripts/importStateData.js`
3. Insert into `state_withholding_tables` table

## Testing

### Test API Endpoint

```bash
curl -X POST http://localhost:3000/api/v1/payroll/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "grossPay": 9152.00,
    "state": "CO",
    "localJurisdiction": "NONE",
    "employeeStatus": "H1B",
    "filingStatus": "SINGLE",
    "payPeriods": 12,
    "w4Data": {
      "step2Checkbox": false,
      "step3Credits": 0,
      "step4aOtherIncome": 0,
      "step4bDeductions": 0,
      "step4cExtraWithholding": 0
    },
    "yearToDateGross": 0,
    "yearToDateNet": 0,
    "taxYear": 2026
  }'
```

## Frontend Integration

The frontend (`paystub_calculator.html`) is configured to call the backend API by default.

To use backend:
- Set `USE_BACKEND = true` in the HTML file
- Ensure backend is running on `http://localhost:3000`

To use frontend calculation (fallback):
- Set `USE_BACKEND = false` in the HTML file

## Database Location

Database file: `backend/database/tax_data.db`

To reset database:
1. Delete `backend/database/tax_data.db`
2. Run `npm run import-all`

## Next Steps

1. Import complete Pub 15-T tables from IRS
2. Import state withholding tables for all states
3. Verify all rates against official 2026 sources
4. Add more local tax jurisdictions
5. Implement audit logging
6. Add authentication/authorization

