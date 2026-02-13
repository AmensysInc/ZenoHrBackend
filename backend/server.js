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

// Rate limiting storage (simple in-memory - use Redis in production)
const rateLimitStore = new Map();
const RATE_LIMIT_WINDOW = 60000; // 1 minute
const RATE_LIMIT_MAX_REQUESTS = 100; // Max requests per window per IP

// Simple rate limiting middleware
function rateLimit(req, res, next) {
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    const now = Date.now();
    
    if (!rateLimitStore.has(ip)) {
        rateLimitStore.set(ip, { count: 1, resetTime: now + RATE_LIMIT_WINDOW });
        return next();
    }
    
    const limit = rateLimitStore.get(ip);
    
    if (now > limit.resetTime) {
        limit.count = 1;
        limit.resetTime = now + RATE_LIMIT_WINDOW;
        return next();
    }
    
    if (limit.count >= RATE_LIMIT_MAX_REQUESTS) {
        return res.status(429).json({
            error: 'Rate limit exceeded',
            message: 'Too many requests. Please try again later.'
        });
    }
    
    limit.count++;
    next();
}

// Simple authentication middleware (replace with proper auth in production)
function authenticate(req, res, next) {
    // In production, use proper authentication:
    // - JWT tokens
    // - Session validation
    // - API keys
    // - OAuth2
    
    const authToken = req.headers.authorization || req.headers['x-api-key'];
    
    // For now, allow if AUTH_REQUIRED env var is not set
    // In production, set AUTH_REQUIRED=true and implement proper auth
    if (process.env.AUTH_REQUIRED === 'true' && !authToken) {
        return res.status(401).json({
            error: 'Authentication required',
            message: 'Valid authentication token required'
        });
    }
    
    // TODO: Validate token here
    // if (authToken && !isValidToken(authToken)) {
    //     return res.status(403).json({ error: 'Invalid token' });
    // }
    
    next();
}

// Audit logging
function auditLog(req, calculationId, result) {
    const { getDatabase } = require('./database/init');
    const db = getDatabase();
    
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    const userAgent = req.headers['user-agent'] || 'unknown';
    
    // Log to audit table
    db.run(`INSERT INTO payroll_calculations_audit 
            (employee_id, calculation_date, tax_year, input_data, output_data, calculated_by, ip_address)
            VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [
            req.body.employeeId || 'unknown',
            new Date().toISOString(),
            req.body.taxYear || 2026,
            JSON.stringify(req.body),
            JSON.stringify(result),
            req.headers['x-user-id'] || 'system',
            ip
        ],
        (err) => {
            if (err) {
                console.error('[Audit] Failed to log calculation:', err);
            } else {
                console.log(`[Audit] Logged calculation ${calculationId} from IP ${ip}`);
            }
        }
    );
}

// Main calculation endpoint
app.post('/api/v1/payroll/calculate', authenticate, rateLimit, async (req, res) => {
    const calculationId = `CALC-${req.body.taxYear || 2026}-${Date.now()}`;
    const startTime = Date.now();
    
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

        // Security: Validate input ranges to prevent abuse
        if (grossPay && (grossPay < 0 || grossPay > 1000000)) {
            return res.status(400).json({
                error: 'Invalid input',
                message: 'Gross pay must be between 0 and 1,000,000'
            });
        }

        // Validate required fields
        if (!grossPay || !state || !filingStatus || !payPeriods) {
            return res.status(400).json({
                error: 'Missing required fields',
                required: ['grossPay', 'state', 'filingStatus', 'payPeriods']
            });
        }

        // Validate state code format
        if (!/^[A-Z]{2}$/.test(state)) {
            return res.status(400).json({
                error: 'Invalid state code',
                message: 'State must be a valid 2-letter US state code'
            });
        }

        // Validate pay periods
        const validPayPeriods = [1, 4, 12, 24, 26, 52];
        if (!validPayPeriods.includes(parseInt(payPeriods))) {
            return res.status(400).json({
                error: 'Invalid pay periods',
                message: 'Pay periods must be one of: 1, 4, 12, 24, 26, 52'
            });
        }

        // Log request (without sensitive data)
        console.log(`[API] Calculation request ${calculationId}:`, {
            state,
            filingStatus,
            payPeriods,
            taxYear: taxYear || 2026,
            ip: req.ip || req.connection.remoteAddress
        });

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

        const response = {
            success: true,
            paystub: result,
            calculationId: calculationId,
            timestamp: new Date().toISOString(),
            processingTimeMs: Date.now() - startTime
        };

        // Audit log the calculation
        auditLog(req, calculationId, result);

        res.json(response);

    } catch (error) {
        console.error(`[API] Calculation error ${calculationId}:`, error);
        
        // Audit log the error
        auditLog(req, calculationId, { error: error.message });
        
        res.status(500).json({
            error: 'Calculation failed',
            message: error.message,
            calculationId: calculationId
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

