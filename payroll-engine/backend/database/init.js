const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const DB_PATH = path.join(__dirname, 'tax_data.db');

// Create database directory if it doesn't exist
const dbDir = path.dirname(DB_PATH);
if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
}

// Use a single shared database connection to avoid connection issues
let sharedDb = null;

function getDatabase() {
    if (!sharedDb) {
        // Ensure database directory exists and is writable
        const dbDir = path.dirname(DB_PATH);
        if (!fs.existsSync(dbDir)) {
            fs.mkdirSync(dbDir, { recursive: true, mode: 0o775 });
        }
        // Ensure database file is writable if it exists
        if (fs.existsSync(DB_PATH)) {
            try {
                fs.chmodSync(DB_PATH, 0o664);
            } catch (err) {
                console.warn('Could not set database file permissions:', err.message);
            }
        }
        
        // Open database with read-write mode (default, but explicit for clarity)
        sharedDb = new sqlite3.Database(DB_PATH, sqlite3.OPEN_READWRITE | sqlite3.OPEN_CREATE, (err) => {
            if (err) {
                console.error('Error opening database:', err);
            } else {
                // Set database to use WAL mode for better concurrency
                sharedDb.run('PRAGMA journal_mode = WAL;', (err) => {
                    if (err) {
                        console.warn('Could not set WAL mode:', err.message);
                    }
                });
            }
        });
    }
    return sharedDb;
}

function initDatabase() {
    return new Promise((resolve, reject) => {
        // Ensure database directory exists and is writable
        const dbDir = path.dirname(DB_PATH);
        if (!fs.existsSync(dbDir)) {
            fs.mkdirSync(dbDir, { recursive: true, mode: 0o775 });
        }
        // Ensure database file is writable if it exists
        if (fs.existsSync(DB_PATH)) {
            try {
                fs.chmodSync(DB_PATH, 0o664);
            } catch (err) {
                console.warn('Could not set database file permissions:', err.message);
            }
        }
        
        const db = getDatabase();

        db.serialize(() => {
            // Tax Years table
            db.run(`CREATE TABLE IF NOT EXISTS tax_years (
                year INTEGER PRIMARY KEY,
                status TEXT DEFAULT 'active',
                effective_date TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);

            // Federal Tax Brackets
            db.run(`CREATE TABLE IF NOT EXISTS federal_brackets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                filing_status TEXT,
                bracket_min REAL,
                bracket_max REAL,
                rate REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // Federal Standard Deductions
            db.run(`CREATE TABLE IF NOT EXISTS federal_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                filing_status TEXT,
                standard_deduction REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // FICA Rates
            db.run(`CREATE TABLE IF NOT EXISTS fica_rates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                social_security_rate REAL,
                social_security_wage_base REAL,
                medicare_rate REAL,
                additional_medicare_rate REAL,
                additional_medicare_threshold REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // IRS Pub 15-T Percentage Method Tables
            // Proper IRS format: base_tax + rate × (taxable_wages − excess_over)
            db.run(`CREATE TABLE IF NOT EXISTS pub15t_percentage_tables (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                pay_frequency TEXT,
                filing_status TEXT,
                step2_checkbox INTEGER DEFAULT 0,
                wage_min REAL,
                wage_max REAL,
                base_tax REAL,
                rate REAL,
                excess_over REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // State Tax Brackets
            db.run(`CREATE TABLE IF NOT EXISTS state_brackets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                state_code TEXT,
                filing_status TEXT,
                bracket_min REAL,
                bracket_max REAL,
                rate REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // State Standard Deductions
            db.run(`CREATE TABLE IF NOT EXISTS state_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                state_code TEXT,
                filing_status TEXT,
                standard_deduction REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // State Withholding Tables
            db.run(`CREATE TABLE IF NOT EXISTS state_withholding_tables (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                state_code TEXT,
                pay_frequency TEXT,
                filing_status TEXT,
                wage_min REAL,
                wage_max REAL,
                withholding_amount REAL,
                percentage REAL,
                base_amount REAL,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // State Payroll Taxes
            db.run(`CREATE TABLE IF NOT EXISTS state_payroll_taxes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                state_code TEXT,
                tax_type TEXT,
                rate REAL,
                wage_base REAL,
                employee_paid INTEGER DEFAULT 1,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // Local Taxes
            db.run(`CREATE TABLE IF NOT EXISTS local_taxes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                state_code TEXT,
                jurisdiction TEXT,
                tax_type TEXT,
                rate REAL,
                fixed_amount REAL,
                brackets_json TEXT,
                FOREIGN KEY (year) REFERENCES tax_years(year)
            )`);

            // Audit Log
            db.run(`CREATE TABLE IF NOT EXISTS payroll_calculations_audit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT,
                calculation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                tax_year INTEGER,
                input_data TEXT,
                output_data TEXT,
                calculated_by TEXT,
                ip_address TEXT
            )`);

            // Insert 2026 tax year (only if it doesn't exist)
            // Check first to avoid write errors if database is read-only or already has data
            db.get(`SELECT COUNT(*) as count FROM tax_years WHERE year = 2026`, (err, row) => {
                if (err) {
                    console.error('Error checking tax year:', err);
                    reject(err);
                    return;
                }
                
                if (row && row.count > 0) {
                    // Tax year already exists, skip insert
                    // Don't log to stdout - this goes to stderr to avoid JSON parsing issues
                    console.error('Database tables already initialized');
                    resolve();
                } else {
                    // Insert tax year
                    db.run(`INSERT OR IGNORE INTO tax_years (year, status, effective_date) 
                            VALUES (2026, 'active', '2026-01-01')`, (err) => {
                        if (err) {
                            // If write fails, check if it's because data already exists
                            if (err.code === 'SQLITE_READONLY') {
                                console.warn('Database is read-only, but tables appear to exist. Continuing...');
                                resolve(); // Continue if tables exist
                            } else {
                                console.error('Error inserting tax year:', err);
                                reject(err);
                            }
                        } else {
                            // Don't log to stdout - this goes to stderr to avoid JSON parsing issues
                            console.error('Database tables created successfully');
                            resolve();
                        }
                    });
                }
            });
        });

        // Don't close database - it's shared
    });
}

module.exports = { initDatabase, getDatabase };

