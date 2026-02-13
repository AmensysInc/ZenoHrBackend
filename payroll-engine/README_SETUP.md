# Payroll Engine Backend Setup

## Prerequisites
- Node.js (v14 or higher)
- npm or yarn

## Installation

1. Navigate to the backend directory:
```bash
cd payroll-engine/backend
```

2. Install dependencies:
```bash
npm install
```

3. Initialize the database:
```bash
npm run init-db
```

## Running the Service

### Development Mode
```bash
npm start
```

The service will run on `http://localhost:3000` by default.

### Production Mode
Set the `PORT` environment variable:
```bash
PORT=3000 npm start
```

## Environment Variables

- `PORT`: Port number for the service (default: 3000)
- `NODE_ENV`: Environment (development/production)

## API Endpoints

- `GET /health` - Health check
- `POST /api/v1/payroll/calculate` - Calculate payroll
- `GET /api/v1/tax-config/:year` - Get tax configuration for a year

## Integration with Main Application

The payroll engine should be running as a separate service. The main application will call it via HTTP.

Set the following environment variable in your main application:
- `REACT_APP_PAYROLL_ENGINE_URL=http://localhost:3000` (or your production URL)

