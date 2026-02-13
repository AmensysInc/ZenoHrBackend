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
        sharedDb = new sqlite3.Database(DB_PATH, (err) => {
            if (err) {
                console.error('Error opening database:', err);
            }
        });
    }
    return sharedDb;
}

function initDatabase() {
    return new Promise((resolve, reject) => {
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

            // Insert 2026 tax year
            db.run(`INSERT OR IGNORE INTO tax_years (year, status, effective_date) 
                    VALUES (2026, 'active', '2026-01-01')`, (err) => {
                if (err) {
                    console.error('Error inserting tax year:', err);
                    reject(err);
                } else {
                    console.log('Database tables created successfully');
                    // Don't close the database here - it's used by the server
                    resolve();
                }
            });
        });

        // Don't close database - it's shared
    });
}

module.exports = { initDatabase, getDatabase };

