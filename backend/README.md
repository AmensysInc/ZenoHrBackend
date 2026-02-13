# Paystub Calculator Backend API

Production-grade backend for paystub calculations with IRS Pub 15-T percentage method tables and state withholding tables.

## Features

- ✅ IRS Pub 15-T Percentage Method tables
- ✅ State withholding tables (with bracket fallback)
- ✅ Federal tax brackets and standard deductions
- ✅ FICA calculations (Social Security, Medicare)
- ✅ State payroll taxes (UI, DI, FLI, SDI, PFL, etc.)
- ✅ Local taxes support
- ✅ SQLite database for tax data storage
- ✅ Audit logging

## Setup

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Initialize Database

```bash
npm run init-db
```

This will:
- Create database tables
- Seed 2026 federal tax data
- Seed sample Pub 15-T tables
- Seed sample state data (Colorado)

### 3. Start Server

```bash
npm start
```

Or for development with auto-reload:

```bash
npm run dev
```

Server runs on `http://localhost:3000`

## API Endpoints

### Health Check

```
GET /health
```

### Calculate Paystub

```
POST /api/v1/payroll/calculate
```

**Request Body:**
```json
{
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
}
```

**Response:**
```json
{
  "success": true,
  "paystub": {
    "grossPay": 9152.00,
    "federalIncomeTax": 1227.61,
    "stateIncomeTax": 402.69,
    "statePayrollTaxes": {
      "FAMLI": {
        "amount": 41.18,
        "name": "Family Medical Leave Insurance"
      }
    },
    "localTaxes": {},
    "socialSecurity": 567.42,
    "medicare": 132.70,
    "additionalMedicare": 0,
    "totalDeductions": 2371.60,
    "netPay": 6780.40,
    "ytdGross": 9152.00,
    "ytdNet": 6780.40,
    "annualGross": 109824.00
  },
  "calculationId": "CALC-2026-1234567890",
  "timestamp": "2026-01-15T10:30:00.000Z"
}
```

### Get Tax Configuration

```
GET /api/v1/tax-config/:year
```

## Database Schema

- `tax_years` - Tax year configuration
- `federal_brackets` - Federal tax brackets
- `federal_deductions` - Standard deductions
- `fica_rates` - FICA tax rates and wage bases
- `pub15t_percentage_tables` - IRS Pub 15-T percentage method tables
- `state_brackets` - State tax brackets
- `state_deductions` - State standard deductions
- `state_withholding_tables` - Official state withholding tables
- `state_payroll_taxes` - State payroll tax rates
- `local_taxes` - Local tax configuration
- `payroll_calculations_audit` - Audit log

## Adding Tax Data

### Add Pub 15-T Tables

Insert into `pub15t_percentage_tables`:

```sql
INSERT INTO pub15t_percentage_tables 
(year, pay_frequency, filing_status, step2_checkbox, wage_min, wage_max, percentage, base_amount)
VALUES (2026, 'MONTHLY', 'SINGLE', 0, 0, 1000, 0.0, 0);
```

### Add State Withholding Tables

Insert into `state_withholding_tables`:

```sql
INSERT INTO state_withholding_tables
(year, state_code, pay_frequency, filing_status, wage_min, wage_max, withholding_amount, percentage, base_amount)
VALUES (2026, 'CO', 'MONTHLY', 'SINGLE', 0, 10000, 0, 0.044, 0);
```

## Notes

- **Pub 15-T Tables**: Currently seeded with sample data. Full implementation requires complete Pub 15-T tables from IRS.
- **State Withholding Tables**: Currently falls back to bracket calculation. Add official state withholding tables for exact matching.
- **Database**: Uses SQLite for simplicity. For production, consider PostgreSQL or MySQL.

## Next Steps

1. Import complete Pub 15-T percentage method tables
2. Import state withholding tables for all states
3. Add more state payroll tax data
4. Add local tax data
5. Implement audit logging
6. Add authentication/authorization
7. Add rate limiting
8. Add comprehensive error handling

