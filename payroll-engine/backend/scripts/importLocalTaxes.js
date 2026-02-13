const { getDatabase } = require('../database/init');

/**
 * Import local tax data (NYC, Yonkers, PA, MD, OH)
 */
async function importLocalTaxes() {
    const db = getDatabase();
    const year = 2026;

    const localTaxes = [
        // New York - NYC Resident Tax
        {
            state_code: 'NY',
            jurisdiction: 'NYC',
            tax_type: 'brackets',
            rate: null,
            fixed_amount: null,
            brackets_json: JSON.stringify([
                { min: 0, max: 12000, rate: 0.03078 },
                { min: 12000, max: 25000, rate: 0.03762 },
                { min: 25000, max: 50000, rate: 0.03819 },
                { min: 50000, max: 999999999, rate: 0.03876 }
            ])
        },
        // New York - Yonkers Resident
        {
            state_code: 'NY',
            jurisdiction: 'YONKERS_RESIDENT',
            tax_type: 'flat',
            rate: 0.165,
            fixed_amount: null,
            brackets_json: null
        },
        // New York - Yonkers Non-Resident
        {
            state_code: 'NY',
            jurisdiction: 'YONKERS_NONRESIDENT',
            tax_type: 'flat',
            rate: 0.005,
            fixed_amount: null,
            brackets_json: null
        },
        // Pennsylvania - Local EIT (example - varies by municipality)
        {
            state_code: 'PA',
            jurisdiction: 'LOCAL_EIT',
            tax_type: 'flat',
            rate: 0.01, // Default 1% - varies by municipality
            fixed_amount: null,
            brackets_json: null
        },
        // Pennsylvania - Local Services Tax (LST)
        {
            state_code: 'PA',
            jurisdiction: 'LOCAL_LST',
            tax_type: 'fixed',
            rate: null,
            fixed_amount: 52, // Annual amount, prorated
            brackets_json: null
        },
        // Maryland - County Tax (example - varies by county)
        {
            state_code: 'MD',
            jurisdiction: 'COUNTY',
            tax_type: 'flat',
            rate: 0.032, // Default 3.2% - varies by county (2.25% to 3.2%)
            fixed_amount: null,
            brackets_json: null
        },
        // Ohio - School District Tax (example - varies by district)
        {
            state_code: 'OH',
            jurisdiction: 'SCHOOL_DISTRICT',
            tax_type: 'flat',
            rate: 0.01, // Default 1% - varies by school district (0.5% to 2.5%)
            fixed_amount: null,
            brackets_json: null
        }
    ];

    return new Promise((resolve, reject) => {
        db.serialize(() => {
            db.run(`DELETE FROM local_taxes WHERE year = ?`, [year], (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                const stmt = db.prepare(`INSERT INTO local_taxes 
                    (year, state_code, jurisdiction, tax_type, rate, fixed_amount, brackets_json) 
                    VALUES (?, ?, ?, ?, ?, ?, ?)`);

                localTaxes.forEach(tax => {
                    stmt.run(year, tax.state_code, tax.jurisdiction, tax.tax_type, 
                            tax.rate, tax.fixed_amount, tax.brackets_json);
                });

                stmt.finalize((err) => {
                    if (err) {
                        reject(err);
                    } else {
                        console.log(`Imported ${localTaxes.length} local tax entries`);
                        resolve();
                    }
                });
            });
        });
    });
}

module.exports = { importLocalTaxes };

if (require.main === module) {
    importLocalTaxes()
        .then(() => {
            console.log('Local taxes import complete');
            process.exit(0);
        })
        .catch(err => {
            console.error('Import error:', err);
            process.exit(1);
        });
}

