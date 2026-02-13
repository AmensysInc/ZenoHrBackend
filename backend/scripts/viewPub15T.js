const { getDatabase } = require('../database/init');

/**
 * View Pub 15-T table data
 * Usage: node scripts/viewPub15T.js [pay_frequency] [filing_status] [step2]
 * Example: node scripts/viewPub15T.js MONTHLY SINGLE 0
 */

const args = process.argv.slice(2);
const payFrequency = args[0] || null;
const filingStatus = args[1] || null;
const step2 = args[2] !== undefined ? parseInt(args[2]) : null;

function viewPub15TTables() {
    const db = getDatabase();
    const year = 2026;
    
    let query = `SELECT * FROM pub15t_percentage_tables WHERE year = ?`;
    const params = [year];
    
    if (payFrequency) {
        query += ` AND pay_frequency = ?`;
        params.push(payFrequency.toUpperCase());
    }
    if (filingStatus) {
        query += ` AND filing_status = ?`;
        params.push(filingStatus.toUpperCase());
    }
    if (step2 !== null) {
        query += ` AND step2_checkbox = ?`;
        params.push(step2);
    }
    
    query += ` ORDER BY pay_frequency, filing_status, step2_checkbox, wage_min LIMIT 100`;
    
    return new Promise((resolve, reject) => {
        db.all(query, params, (err, rows) => {
            if (err) {
                reject(err);
                return;
            }
            
            if (rows.length === 0) {
                console.log('No data found matching criteria.');
                resolve();
                return;
            }
            
            console.log('\n📊 Pub 15-T Percentage Method Tables (2026)\n');
            console.log('Pay Frequency | Filing Status        | Step 2 | Wage Min    | Wage Max    | Percentage | Base Amount');
            console.log('--------------|---------------------|--------|-------------|-------------|------------|------------');
            
            rows.forEach(row => {
                const step2Text = row.step2_checkbox ? 'Yes' : 'No';
                const freq = row.pay_frequency.padEnd(13);
                const status = row.filing_status.padEnd(19);
                const wageMin = row.wage_min.toFixed(2).padStart(11);
                const wageMax = row.wage_max === 999999999 ? '∞'.padStart(11) : row.wage_max.toFixed(2).padStart(11);
                const percentage = (row.percentage * 100).toFixed(4).padStart(10) + '%';
                const base = row.base_amount.toFixed(2).padStart(11);
                
                console.log(`${freq} | ${status} | ${step2Text.padEnd(6)} | ${wageMin} | ${wageMax} | ${percentage} | ${base}`);
            });
            
            // Get total count
            let countQuery = `SELECT COUNT(*) as total FROM pub15t_percentage_tables WHERE year = ?`;
            const countParams = [year];
            
            if (payFrequency) {
                countQuery += ` AND pay_frequency = ?`;
                countParams.push(payFrequency.toUpperCase());
            }
            if (filingStatus) {
                countQuery += ` AND filing_status = ?`;
                countParams.push(filingStatus.toUpperCase());
            }
            if (step2 !== null) {
                countQuery += ` AND step2_checkbox = ?`;
                countParams.push(step2);
            }
            
            db.get(countQuery, countParams, (err2, row) => {
                if (err2) {
                    reject(err2);
                    return;
                }
                
                console.log(`\n📈 Total entries shown: ${rows.length} (of ${row.total} total matching criteria)`);
                
                if (rows.length >= 100) {
                    console.log('⚠️  Showing first 100 entries. Use filters to narrow down results.');
                }
                
                resolve();
            });
        });
    });
}

// Show summary if no filters
if (!payFrequency && !filingStatus && step2 === null) {
    const db = getDatabase();
    db.all(`SELECT pay_frequency, filing_status, step2_checkbox, COUNT(*) as count
            FROM pub15t_percentage_tables
            WHERE year = 2026
            GROUP BY pay_frequency, filing_status, step2_checkbox
            ORDER BY pay_frequency, filing_status, step2_checkbox`,
        [],
        (err, rows) => {
            if (err) {
                console.error('Error:', err);
                process.exit(1);
            }
            
            console.log('\n📊 Pub 15-T Table Summary (2026)\n');
            console.log('Pay Frequency | Filing Status        | Step 2 | Entries');
            console.log('--------------|---------------------|--------|--------');
            
            rows.forEach(row => {
                const step2Text = row.step2_checkbox ? 'Yes' : 'No';
                const freq = row.pay_frequency.padEnd(13);
                const status = row.filing_status.padEnd(19);
                console.log(`${freq} | ${status} | ${step2Text.padEnd(6)} | ${row.count}`);
            });
            
            db.get(`SELECT COUNT(*) as total FROM pub15t_percentage_tables WHERE year = 2026`, [], (err2, row) => {
                if (err2) {
                    console.error('Error:', err2);
                    process.exit(1);
                }
                
                console.log(`\n📈 Total entries: ${row.total}`);
                console.log('\n💡 Usage examples:');
                console.log('   node scripts/viewPub15T.js MONTHLY SINGLE 0');
                console.log('   node scripts/viewPub15T.js WEEKLY MARRIED_JOINTLY 1');
                console.log('   node scripts/viewPub15T.js BIWEEKLY HEAD_OF_HOUSEHOLD 0');
                
                db.close();
                process.exit(0);
            });
        }
    );
} else {
    viewPub15TTables()
        .then(() => {
            const db = getDatabase();
            db.close();
            process.exit(0);
        })
        .catch(err => {
            console.error('Error:', err);
            process.exit(1);
        });
}

