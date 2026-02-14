#!/usr/bin/env node
/**
 * Standalone payroll calculation script
 * Called from Java backend to calculate payroll
 * 
 * PRODUCTION MODE:
 * - No auto-import of data (tables must be pre-seeded)
 * - Strict table validation (fails if critical tables missing)
 * - No stack trace exposure (security)
 */

const { calculatePaystub } = require('./services/payrollService');
const { initDatabase } = require('./database/init');
const { validateTables } = require('./database/validateTables');

// Initialize database and validate tables (NO auto-import in production)
let dbInitialized = false;

async function ensureDatabaseReady() {
    if (dbInitialized) {
        return;
    }
    
    try {
        // Initialize database (create tables only - no data import)
        // Suppress all output from initDatabase - it logs to stderr which Java reads
        await initDatabase();
        
        // Strict validation - fail if critical tables are missing
        const validation = await validateTables(2026);
        
        if (!validation.valid) {
            const errorMsg = `Database validation failed. Critical tables missing: ${validation.errors.join('; ')}`;
            throw new Error(errorMsg);
        }
        
        // Don't log warnings - they pollute stdout/stderr
        // if (validation.warnings.length > 0) {
        //     console.warn('Database validation warnings:', validation.warnings.join('; '));
        // }
        
        dbInitialized = true;
    } catch (error) {
        // Don't expose stack traces in production
        const errorMsg = error.message || 'Database initialization failed';
        throw new Error(errorMsg);
    }
}

// Read input from stdin
let inputData = '';
process.stdin.setEncoding('utf8');

process.stdin.on('data', (chunk) => {
    inputData += chunk;
});

process.stdin.on('end', async () => {
    try {
        // Ensure database is ready before calculation
        await ensureDatabaseReady();
        
        const request = JSON.parse(inputData);
        
        calculatePaystub(request)
            .then(result => {
                // Success: Output JSON to stdout (Java reads this)
                console.log(JSON.stringify(result));
                process.exit(0);
            })
            .catch(error => {
                // Error: Output JSON to stdout so Java can parse it
                // All debug logs already go to stderr, so stdout is clean
                const errorResponse = {
                    error: error.message || 'Payroll calculation failed',
                    code: 'CALCULATION_ERROR'
                };
                console.log(JSON.stringify(errorResponse));
                process.exit(1);
            });
    } catch (error) {
        // Error: Output JSON to stdout so Java can parse it
        const errorResponse = {
            error: error.message || 'Invalid request or database error',
            code: 'REQUEST_ERROR'
        };
        console.log(JSON.stringify(errorResponse));
        process.exit(1);
    }
});

