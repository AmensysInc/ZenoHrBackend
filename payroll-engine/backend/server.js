const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const { calculatePaystub } = require('./services/payrollService');
const { initDatabase } = require('./database/init');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Initialize database on startup
initDatabase().then(() => {
    console.log('Database initialized successfully');
}).catch(err => {
    console.error('Database initialization error:', err);
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', message: 'Paystub Calculator API is running' });
});

// Main calculation endpoint
app.post('/api/v1/payroll/calculate', async (req, res) => {
    try {
        const {
            grossPay,
            taxableGrossPay,
            preTaxDeductions,
            state,
            localJurisdiction,
            employeeStatus,
            filingStatus,
            payPeriods,
            w4Data,
            yearToDateGross,
            yearToDateNet,
            taxYear
        } = req.body;

        // Debug: Log what we received
        console.log('[API] Received request:', {
            grossPay,
            taxableGrossPay,
            preTaxDeductions,
            state,
            filingStatus,
            payPeriods
        });

        // Validate required fields
        if (!grossPay || !state || !filingStatus || !payPeriods) {
            return res.status(400).json({
                error: 'Missing required fields',
                required: ['grossPay', 'state', 'filingStatus', 'payPeriods']
            });
        }

        // Calculate paystub
        const result = await calculatePaystub({
            grossPay: parseFloat(grossPay),
            taxableGrossPay: taxableGrossPay ? parseFloat(taxableGrossPay) : undefined,
            preTaxDeductions: preTaxDeductions || undefined,
            state,
            localJurisdiction: localJurisdiction || 'NONE',
            employeeStatus: employeeStatus || 'US_CITIZEN',
            filingStatus,
            payPeriods: parseInt(payPeriods),
            w4Data: w4Data || {},
            yearToDateGross: parseFloat(yearToDateGross) || 0,
            yearToDateNet: parseFloat(yearToDateNet) || 0,
            taxYear: taxYear || 2026
        });

        res.json({
            success: true,
            paystub: result,
            calculationId: `CALC-${taxYear}-${Date.now()}`,
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        console.error('Calculation error:', error);
        res.status(500).json({
            error: 'Calculation failed',
            message: error.message
        });
    }
});

// Get tax configuration by year
app.get('/api/v1/tax-config/:year', async (req, res) => {
    try {
        const { year } = req.params;
        const { getTaxConfig } = require('./database/taxConfig');
        const config = await getTaxConfig(parseInt(year));
        res.json(config);
    } catch (error) {
        console.error('Tax config error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`Paystub Calculator API running on http://localhost:${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
});

