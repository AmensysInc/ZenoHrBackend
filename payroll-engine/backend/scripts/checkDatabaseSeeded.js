#!/usr/bin/env node
/**
 * Check if database has been seeded
 * Returns exit code 0 if seeded, 1 if needs seeding
 */

const { getDatabase } = require('../database/init');
const path = require('path');
const fs = require('fs');

const DB_PATH = path.join(__dirname, '../database/tax_data.db');

// Check if database file exists and has size
if (!fs.existsSync(DB_PATH) || fs.statSync(DB_PATH).size === 0) {
    console.log('Database file missing or empty');
    process.exit(1);
}

// Check if critical table has data
const db = getDatabase();

db.get('SELECT COUNT(*) as count FROM pub15t_percentage_tables WHERE year = 2026', (err, row) => {
    if (err) {
        console.log('Database error:', err.message);
        process.exit(1);
    }
    
    if (!row || row.count === 0) {
        console.log('Database tables are empty');
        process.exit(1);
    }
    
    console.log('Database is seeded');
    process.exit(0);
});

