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
        await initDatabase();
        
        // Strict validation - fail if critical tables are missing
        const validation = await validateTables(2026);
        
        if (!validation.valid) {
            const errorMsg = `Database validation failed. Critical tables missing:\n${validation.errors.join('\n')}`;
            throw new Error(errorMsg);
        }
        
        if (validation.warnings.length > 0) {
            // Log warnings but don't fail
            console.warn('Database validation warnings:', validation.warnings.join('; '));
        }
        
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
                console.log(JSON.stringify(result));
                process.exit(0);
            })
            .catch(error => {
                // Don't expose stack traces - security best practice
                const errorResponse = {
                    error: error.message || 'Payroll calculation failed',
                    code: 'CALCULATION_ERROR'
                };
                console.error(JSON.stringify(errorResponse));
                process.exit(1);
            });
    } catch (error) {
        // Don't expose stack traces - security best practice
        const errorResponse = {
            error: error.message || 'Invalid request or database error',
            code: 'REQUEST_ERROR'
        };
        console.error(JSON.stringify(errorResponse));
        process.exit(1);
    }
});

