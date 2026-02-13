const { getDatabase } = require('../database/init');

/**
 * Import IRS Pub 15-T Percentage Method Tables
 * 
 * Note: This is a comprehensive structure. Actual values should be imported from
 * IRS Publication 15-T (2026) official tables.
 * 
 * Format: For each pay frequency, filing status, and Step 2 checkbox status,
 * there are wage ranges with percentage rates and base amounts.
 */
async function importPub15TTables() {
    const db = getDatabase();
    const year = 2026;

    // Example structure for Monthly, Single, No Step 2
    // These are sample values - replace with actual IRS Pub 15-T tables
    const pub15tData = [
        // Monthly, Single, No Step 2
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 0, wage_max: 1000, percentage: 0.0, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 1000, wage_max: 2000, percentage: 0.10, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 2000, wage_max: 5000, percentage: 0.12, base: 100 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 5000, wage_max: 10000, percentage: 0.22, base: 460 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 10000, wage_max: 20000, percentage: 0.24, base: 1560 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 20000, wage_max: 50000, percentage: 0.32, base: 3960 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 50000, wage_max: 100000, percentage: 0.35, base: 13560 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 0, wage_min: 100000, wage_max: 999999999, percentage: 0.37, base: 31060 },
        
        // Monthly, Single, With Step 2 (higher withholding)
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 0, wage_max: 1000, percentage: 0.0, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 1000, wage_max: 2000, percentage: 0.12, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 2000, wage_max: 5000, percentage: 0.14, base: 120 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 5000, wage_max: 10000, percentage: 0.24, base: 540 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 10000, wage_max: 20000, percentage: 0.26, base: 1740 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 20000, wage_max: 50000, percentage: 0.34, base: 4340 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 50000, wage_max: 100000, percentage: 0.37, base: 14540 },
        { pay_freq: 'MONTHLY', filing_status: 'SINGLE', step2: 1, wage_min: 100000, wage_max: 999999999, percentage: 0.39, base: 33040 },
        
        // Monthly, Married Jointly, No Step 2
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 0, wage_max: 2000, percentage: 0.0, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 2000, wage_max: 4000, percentage: 0.10, base: 0 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 4000, wage_max: 10000, percentage: 0.12, base: 200 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 10000, wage_max: 20000, percentage: 0.22, base: 920 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 20000, wage_max: 40000, percentage: 0.24, base: 3120 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 40000, wage_max: 100000, percentage: 0.32, base: 7920 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 100000, wage_max: 200000, percentage: 0.35, base: 27120 },
        { pay_freq: 'MONTHLY', filing_status: 'MARRIED_JOINTLY', step2: 0, wage_min: 200000, wage_max: 999999999, percentage: 0.37, base: 62120 },
        
        // Add more pay frequencies (WEEKLY, BIWEEKLY, SEMIMONTHLY) as needed
        // Add more filing statuses (MARRIED_SEPARATELY, HEAD_OF_HOUSEHOLD) as needed
    ];

    return new Promise((resolve, reject) => {
        db.serialize(() => {
            // Clear existing data for this year
            db.run(`DELETE FROM pub15t_percentage_tables WHERE year = ?`, [year], (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                const stmt = db.prepare(`INSERT INTO pub15t_percentage_tables 
                    (year, pay_frequency, filing_status, step2_checkbox, wage_min, wage_max, percentage, base_amount) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)`);

                pub15tData.forEach(row => {
                    stmt.run(year, row.pay_freq, row.filing_status, row.step2, 
                            row.wage_min, row.wage_max, row.percentage, row.base);
                });

                stmt.finalize((err) => {
                    if (err) {
                        reject(err);
                    } else {
                        console.log(`Imported ${pub15tData.length} Pub 15-T table entries`);
                        resolve();
                    }
                });
            });
        });
    });
}

module.exports = { importPub15TTables };

// Run if called directly
if (require.main === module) {
    importPub15TTables()
        .then(() => {
            console.log('Pub 15-T import complete');
            process.exit(0);
        })
        .catch(err => {
            console.error('Import error:', err);
            process.exit(1);
        });
}

