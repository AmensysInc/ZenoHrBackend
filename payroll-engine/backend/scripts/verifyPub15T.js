const { getDatabase } = require('../database/init');

/**
 * Verify Pub 15-T table coverage
 * Check that all combinations of pay frequency, filing status, and Step 2 are present
 */
async function verifyPub15TTables() {
    const db = getDatabase();
    const year = 2026;
    
    return new Promise((resolve, reject) => {
        db.all(`SELECT pay_frequency, filing_status, step2_checkbox, COUNT(*) as count
                FROM pub15t_percentage_tables
                WHERE year = ?
                GROUP BY pay_frequency, filing_status, step2_checkbox
                ORDER BY pay_frequency, filing_status, step2_checkbox`,
            [year],
            (err, rows) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                console.log('\n📊 Pub 15-T Table Coverage Report\n');
                console.log('Pay Frequency | Filing Status        | Step 2 | Entries');
                console.log('--------------|---------------------|--------|--------');
                
                const expected = [
                    { freq: 'WEEKLY', status: 'SINGLE', step2: 0 },
                    { freq: 'WEEKLY', status: 'SINGLE', step2: 1 },
                    { freq: 'WEEKLY', status: 'MARRIED_JOINTLY', step2: 0 },
                    { freq: 'WEEKLY', status: 'MARRIED_JOINTLY', step2: 1 },
                    { freq: 'WEEKLY', status: 'MARRIED_SEPARATELY', step2: 0 },
                    { freq: 'WEEKLY', status: 'MARRIED_SEPARATELY', step2: 1 },
                    { freq: 'WEEKLY', status: 'HEAD_OF_HOUSEHOLD', step2: 0 },
                    { freq: 'WEEKLY', status: 'HEAD_OF_HOUSEHOLD', step2: 1 },
                    { freq: 'BIWEEKLY', status: 'SINGLE', step2: 0 },
                    { freq: 'BIWEEKLY', status: 'SINGLE', step2: 1 },
                    { freq: 'BIWEEKLY', status: 'MARRIED_JOINTLY', step2: 0 },
                    { freq: 'BIWEEKLY', status: 'MARRIED_JOINTLY', step2: 1 },
                    { freq: 'BIWEEKLY', status: 'MARRIED_SEPARATELY', step2: 0 },
                    { freq: 'BIWEEKLY', status: 'MARRIED_SEPARATELY', step2: 1 },
                    { freq: 'BIWEEKLY', status: 'HEAD_OF_HOUSEHOLD', step2: 0 },
                    { freq: 'BIWEEKLY', status: 'HEAD_OF_HOUSEHOLD', step2: 1 },
                    { freq: 'SEMIMONTHLY', status: 'SINGLE', step2: 0 },
                    { freq: 'SEMIMONTHLY', status: 'SINGLE', step2: 1 },
                    { freq: 'SEMIMONTHLY', status: 'MARRIED_JOINTLY', step2: 0 },
                    { freq: 'SEMIMONTHLY', status: 'MARRIED_JOINTLY', step2: 1 },
                    { freq: 'SEMIMONTHLY', status: 'MARRIED_SEPARATELY', step2: 0 },
                    { freq: 'SEMIMONTHLY', status: 'MARRIED_SEPARATELY', step2: 1 },
                    { freq: 'SEMIMONTHLY', status: 'HEAD_OF_HOUSEHOLD', step2: 0 },
                    { freq: 'SEMIMONTHLY', status: 'HEAD_OF_HOUSEHOLD', step2: 1 },
                    { freq: 'MONTHLY', status: 'SINGLE', step2: 0 },
                    { freq: 'MONTHLY', status: 'SINGLE', step2: 1 },
                    { freq: 'MONTHLY', status: 'MARRIED_JOINTLY', step2: 0 },
                    { freq: 'MONTHLY', status: 'MARRIED_JOINTLY', step2: 1 },
                    { freq: 'MONTHLY', status: 'MARRIED_SEPARATELY', step2: 0 },
                    { freq: 'MONTHLY', status: 'MARRIED_SEPARATELY', step2: 1 },
                    { freq: 'MONTHLY', status: 'HEAD_OF_HOUSEHOLD', step2: 0 },
                    { freq: 'MONTHLY', status: 'HEAD_OF_HOUSEHOLD', step2: 1 }
                ];
                
                const found = {};
                rows.forEach(row => {
                    const key = `${row.pay_frequency}|${row.filing_status}|${row.step2_checkbox}`;
                    found[key] = row.count;
                    
                    const step2Text = row.step2_checkbox ? 'Yes' : 'No';
                    const statusText = row.filing_status.padEnd(19);
                    console.log(`${row.pay_frequency.padEnd(13)} | ${statusText} | ${step2Text.padEnd(6)} | ${row.count}`);
                });
                
                console.log('\n');
                
                // Check for missing combinations
                const missing = [];
                expected.forEach(exp => {
                    const key = `${exp.freq}|${exp.status}|${exp.step2}`;
                    if (!found[key]) {
                        missing.push(exp);
                    }
                });
                
                if (missing.length > 0) {
                    console.log('⚠️  Missing combinations:');
                    missing.forEach(m => {
                        console.log(`   - ${m.freq}, ${m.status}, Step 2: ${m.step2 ? 'Yes' : 'No'}`);
                    });
                } else {
                    console.log('✅ All expected combinations are present!');
                }
                
                // Total statistics
                db.get(`SELECT COUNT(*) as total FROM pub15t_percentage_tables WHERE year = ?`, [year], (err2, row) => {
                    if (err2) {
                        reject(err2);
                        return;
                    }
                    
                    console.log(`\n📈 Total entries: ${row.total}`);
                    console.log(`📊 Expected combinations: ${expected.length}`);
                    console.log(`✅ Found combinations: ${rows.length}`);
                    
                    resolve();
                });
            }
        );
    });
}

module.exports = { verifyPub15TTables };

if (require.main === module) {
    verifyPub15TTables()
        .then(() => {
            process.exit(0);
        })
        .catch(err => {
            console.error('Verification error:', err);
            process.exit(1);
        });
}

