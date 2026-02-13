const { getDatabase } = require('./init');

// Seed 2026 federal tax data
function seedFederalData2026() {
    return new Promise((resolve, reject) => {
        const db = getDatabase();

        db.serialize(() => {
            // Federal Brackets for 2026
            const federalBrackets = [
                // Single
                { filing_status: 'SINGLE', min: 0, max: 12400, rate: 0.10 },
                { filing_status: 'SINGLE', min: 12400, max: 50400, rate: 0.12 },
                { filing_status: 'SINGLE', min: 50400, max: 105700, rate: 0.22 },
                { filing_status: 'SINGLE', min: 105700, max: 201775, rate: 0.24 },
                { filing_status: 'SINGLE', min: 201775, max: 256225, rate: 0.32 },
                { filing_status: 'SINGLE', min: 256225, max: 640600, rate: 0.35 },
                { filing_status: 'SINGLE', min: 640600, max: 999999999, rate: 0.37 },
                // Married Jointly
                { filing_status: 'MARRIED_JOINTLY', min: 0, max: 24800, rate: 0.10 },
                { filing_status: 'MARRIED_JOINTLY', min: 24800, max: 100800, rate: 0.12 },
                { filing_status: 'MARRIED_JOINTLY', min: 100800, max: 211400, rate: 0.22 },
                { filing_status: 'MARRIED_JOINTLY', min: 211400, max: 403550, rate: 0.24 },
                { filing_status: 'MARRIED_JOINTLY', min: 403550, max: 512450, rate: 0.32 },
                { filing_status: 'MARRIED_JOINTLY', min: 512450, max: 1281200, rate: 0.35 },
                { filing_status: 'MARRIED_JOINTLY', min: 1281200, max: 999999999, rate: 0.37 },
                // Married Separately
                { filing_status: 'MARRIED_SEPARATELY', min: 0, max: 12400, rate: 0.10 },
                { filing_status: 'MARRIED_SEPARATELY', min: 12400, max: 50400, rate: 0.12 },
                { filing_status: 'MARRIED_SEPARATELY', min: 50400, max: 105700, rate: 0.22 },
                { filing_status: 'MARRIED_SEPARATELY', min: 105700, max: 201775, rate: 0.24 },
                { filing_status: 'MARRIED_SEPARATELY', min: 201775, max: 256225, rate: 0.32 },
                { filing_status: 'MARRIED_SEPARATELY', min: 256225, max: 640600, rate: 0.35 },
                { filing_status: 'MARRIED_SEPARATELY', min: 640600, max: 999999999, rate: 0.37 },
                // Head of Household
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 0, max: 18650, rate: 0.10 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 18650, max: 75650, rate: 0.12 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 75650, max: 105700, rate: 0.22 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 105700, max: 201775, rate: 0.24 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 201775, max: 256225, rate: 0.32 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 256225, max: 640600, rate: 0.35 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', min: 640600, max: 999999999, rate: 0.37 }
            ];

            const stmt = db.prepare(`INSERT OR REPLACE INTO federal_brackets 
                (year, filing_status, bracket_min, bracket_max, rate) 
                VALUES (?, ?, ?, ?, ?)`);

            federalBrackets.forEach(bracket => {
                stmt.run(2026, bracket.filing_status, bracket.min, bracket.max, bracket.rate);
            });
            stmt.finalize();

            // Standard Deductions
            const deductions = [
                { filing_status: 'SINGLE', deduction: 16100 },
                { filing_status: 'MARRIED_JOINTLY', deduction: 32200 },
                { filing_status: 'MARRIED_SEPARATELY', deduction: 16100 },
                { filing_status: 'HEAD_OF_HOUSEHOLD', deduction: 24150 }
            ];

            const dedStmt = db.prepare(`INSERT OR REPLACE INTO federal_deductions 
                (year, filing_status, standard_deduction) 
                VALUES (?, ?, ?)`);

            deductions.forEach(ded => {
                dedStmt.run(2026, ded.filing_status, ded.deduction);
            });
            dedStmt.finalize();

            // FICA Rates
            db.run(`INSERT OR REPLACE INTO fica_rates 
                (year, social_security_rate, social_security_wage_base, medicare_rate, 
                 additional_medicare_rate, additional_medicare_threshold) 
                VALUES (2026, 0.062, 184500, 0.0145, 0.009, 200000)`, (err) => {
                if (err) {
                    console.error('Error seeding FICA:', err);
                    reject(err);
                } else {
                    console.log('Federal data seeded successfully');
                    // Don't close the database - it's a shared connection
                    resolve();
                }
            });
        });
    });
}

// Seed Pub 15-T Percentage Method Tables
// Note: This is a simplified version. Full implementation requires complete Pub 15-T tables
function seedPub15TTables2026() {
    return new Promise((resolve, reject) => {
        const db = getDatabase();

        db.serialize(() => {
            // Example: Monthly pay frequency, Single, no Step 2 checkbox
            // These are sample values - full tables would have many more entries
            const sampleEntries = [
                // Monthly, Single, No Step 2
                { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 0, wage_max: 1000, percentage: 0.0, base: 0 },
                { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 1000, wage_max: 2000, percentage: 0.10, base: 0 },
                { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 2000, wage_max: 5000, percentage: 0.12, base: 100 },
                // Add more entries as needed
            ];

            const stmt = db.prepare(`INSERT OR REPLACE INTO pub15t_percentage_tables 
                (year, pay_frequency, filing_status, step2_checkbox, wage_min, wage_max, percentage, base_amount) 
                VALUES (2026, ?, ?, ?, ?, ?, ?, ?)`);

            sampleEntries.forEach(entry => {
                stmt.run(2026, entry.pay_freq, entry.filing_status, entry.step2, 
                        entry.wage_min, entry.wage_max, entry.percentage, entry.base);
            });
            stmt.finalize((err) => {
                if (err) {
                    console.error('Error seeding Pub 15-T tables:', err);
                    reject(err);
                } else {
                    console.log('Pub 15-T tables seeded (sample data)');
                    resolve();
                }
            });
        });

        db.close();
    });
}

// Seed Colorado state data (example)
function seedColoradoData2026() {
    return new Promise((resolve, reject) => {
        const db = getDatabase();

        db.serialize(() => {
            // Colorado flat rate
            db.run(`INSERT OR REPLACE INTO state_brackets 
                (year, state_code, filing_status, bracket_min, bracket_max, rate) 
                VALUES (2026, 'CO', 'SINGLE', 0, 999999999, 0.044)`, (err) => {
                if (err) {
                    reject(err);
                } else {
                    // Colorado FAMLI
                    db.run(`INSERT OR REPLACE INTO state_payroll_taxes 
                        (year, state_code, tax_type, rate, wage_base, employee_paid) 
                        VALUES (2026, 'CO', 'FAMLI', 0.0045, 153164, 1)`, (err2) => {
                        if (err2) {
                            reject(err2);
                        } else {
                            console.log('Colorado data seeded');
                            resolve();
                        }
                    });
                }
            });
        });

        db.close();
    });
}

module.exports = {
    seedFederalData2026,
    seedPub15TTables2026,
    seedColoradoData2026
};

