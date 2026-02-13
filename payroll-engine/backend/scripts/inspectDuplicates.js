const { getDatabase } = require('../database/init');

async function inspectDuplicates() {
    const db = getDatabase();
    const year = 2026;

    console.log('🔍 Inspecting remaining duplicates...\n');

    const duplicates = await new Promise((resolve, reject) => {
        db.all(`SELECT state_code, pay_frequency, filing_status, wage_min, wage_max, COUNT(*) as count, 
                GROUP_CONCAT(id) as ids
                FROM state_withholding_tables 
                WHERE year = ?
                GROUP BY state_code, pay_frequency, filing_status, wage_min, wage_max
                HAVING COUNT(*) > 1
                ORDER BY count DESC
                LIMIT 30`, [year], (err, rows) => {
            if (err) reject(err);
            else resolve(rows);
        });
    });

    if (duplicates.length > 0) {
        console.log(`Found ${duplicates.length} duplicate groups:\n`);
        duplicates.forEach(row => {
            console.log(`${row.state_code} ${row.pay_frequency} ${row.filing_status}`);
            console.log(`  Wage: $${row.wage_min} - $${row.wage_max}`);
            console.log(`  Count: ${row.count}, IDs: ${row.ids}\n`);
        });
    } else {
        console.log('✅ No duplicates found');
    }
}

if (require.main === module) {
    inspectDuplicates()
        .then(() => process.exit(0))
        .catch(err => {
            console.error('Error:', err);
            process.exit(1);
        });
}

module.exports = { inspectDuplicates };

