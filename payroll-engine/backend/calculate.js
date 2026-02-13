#!/usr/bin/env node
/**
 * Standalone payroll calculation script
 * Called from Java backend to calculate payroll
 */

const { calculatePaystub } = require('./services/payrollService');
const { initDatabase } = require('./database/init');
const { importCompletePub15TTables } = require('./scripts/importPub15TComplete');
const { seedFederalData2026 } = require('./database/seedData');

// Initialize database and import tax data
let dbInitialized = false;

async function ensureDatabaseReady() {
    if (dbInitialized) {
        return;
    }
    
    try {
        // Initialize database (create tables)
        await initDatabase();
        
        // Seed FICA rates and federal data (required for calculations)
        await seedFederalData2026();
        
        // Import Pub 15-T data if tables are empty
        // Check if pub15t_percentage_tables has data
        const { getDatabase } = require('./database/init');
        const db = getDatabase();
        
        return new Promise((resolve, reject) => {
            db.get('SELECT COUNT(*) as count FROM pub15t_percentage_tables', (err, row) => {
                if (err) {
                    // Table doesn't exist or error - try to import
                    importCompletePub15TTables()
                        .then(() => {
                            dbInitialized = true;
                            resolve();
                        })
                        .catch(importErr => {
                            // If import fails, still try to proceed (tables are created)
                            console.error('Warning: Could not import Pub 15-T data:', importErr.message);
                            dbInitialized = true;
                            resolve();
                        });
                } else if (row.count === 0) {
                    // Table exists but empty - import data
                    importCompletePub15TTables()
                        .then(() => {
                            dbInitialized = true;
                            resolve();
                        })
                        .catch(importErr => {
                            console.error('Warning: Could not import Pub 15-T data:', importErr.message);
                            dbInitialized = true;
                            resolve();
                        });
                } else {
                    // Data exists
                    dbInitialized = true;
                    resolve();
                }
            });
        });
    } catch (error) {
        console.error('Database initialization error:', error);
        throw error;
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
                console.error(JSON.stringify({ error: error.message, stack: error.stack }));
                process.exit(1);
            });
    } catch (error) {
        console.error(JSON.stringify({ error: error.message, stack: error.stack }));
        process.exit(1);
    }
});

