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
            // First verify FICA rates (critical - must be present before calculations)
            db.get('SELECT COUNT(*) as count FROM fica_rates WHERE year = 2026', (err, ficaRow) => {
                if (err) {
                    console.error('Error checking fica_rates:', err);
                    // Try to seed anyway
                    seedFederalData2026()
                        .then(() => checkPub15T())
                        .catch(seedErr => {
                            console.error('Warning: Could not seed FICA rates:', seedErr.message);
                            checkPub15T();
                        });
                } else if (!ficaRow || ficaRow.count === 0) {
                    console.log('FICA rates missing, seeding...');
                    seedFederalData2026()
                        .then(() => {
                            console.log('FICA rates seeded successfully');
                            checkPub15T();
                        })
                        .catch(seedErr => {
                            console.error('Warning: Could not seed FICA rates:', seedErr.message);
                            checkPub15T();
                        });
                } else {
                    console.log('FICA rates verified');
                    checkPub15T();
                }
            });
            
            function checkPub15T() {
                // Check Pub 15-T tables
                db.get('SELECT COUNT(*) as count FROM pub15t_percentage_tables', (err, row) => {
                    if (err) {
                        console.error('Error checking pub15t_percentage_tables:', err);
                        // Try to import anyway
                        importCompletePub15TTables()
                            .then(() => {
                                console.log('Pub 15-T data imported successfully');
                                dbInitialized = true;
                                resolve();
                            })
                            .catch(importErr => {
                                console.error('Warning: Could not import Pub 15-T data:', importErr.message);
                                dbInitialized = true;
                                resolve();
                            });
                    } else if (row.count === 0) {
                        // Table exists but empty - import data
                        console.log('Pub 15-T tables empty, importing data...');
                        importCompletePub15TTables()
                            .then(() => {
                                console.log('Pub 15-T data imported successfully');
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
                        console.log(`Pub 15-T tables verified (${row.count} entries)`);
                        dbInitialized = true;
                        resolve();
                    }
                });
            }
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

