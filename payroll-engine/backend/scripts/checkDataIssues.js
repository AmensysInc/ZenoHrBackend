const { getDatabase } = require('../database/init');

/**
 * Check for data quality issues in state withholding tables
 */
async function checkDataIssues() {
    const db = getDatabase();
    const year = 2026;

    console.log('🔍 Checking Data Quality Issues...\n');

    try {
        // Check which states have null percentages
        console.log('📊 States with null percentages:');
        const nullStates = await new Promise((resolve, reject) => {
            db.all(`SELECT state_code, COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND percentage IS NULL
                    GROUP BY state_code 
                    ORDER BY count DESC`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        if (nullStates.length > 0) {
            nullStates.forEach(row => {
                console.log(`   ${row.state_code}: ${row.count} records with null percentage`);
            });
        } else {
            console.log('   ✅ No states with null percentages');
        }

        // Check for duplicate records
        console.log('\n📊 Checking for duplicate records:');
        const duplicates = await new Promise((resolve, reject) => {
            db.all(`SELECT state_code, pay_frequency, filing_status, wage_min, wage_max, COUNT(*) as count
                    FROM state_withholding_tables 
                    WHERE year = ?
                    GROUP BY state_code, pay_frequency, filing_status, wage_min, wage_max
                    HAVING COUNT(*) > 1
                    ORDER BY count DESC
                    LIMIT 10`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        if (duplicates.length > 0) {
            console.log(`   ⚠️  Found ${duplicates.length} duplicate combinations (showing first 10):`);
            duplicates.forEach(row => {
                console.log(`   ${row.state_code} ${row.pay_frequency} ${row.filing_status} $${row.wage_min}-$${row.wage_max}: ${row.count} duplicates`);
            });
        } else {
            console.log('   ✅ No duplicate records found');
        }

        // Check total duplicate count
        const totalDuplicates = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) - COUNT(DISTINCT state_code || pay_frequency || filing_status || wage_min || wage_max) as duplicates
                    FROM state_withholding_tables 
                    WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.duplicates);
            });
        });
        console.log(`   Total duplicate records: ${totalDuplicates}`);

        // Sample records with null percentages
        console.log('\n📋 Sample records with null percentages:');
        const nullSamples = await new Promise((resolve, reject) => {
            db.all(`SELECT state_code, pay_frequency, filing_status, wage_min, wage_max, percentage, base_amount, withholding_amount
                    FROM state_withholding_tables 
                    WHERE year = ? AND percentage IS NULL
                    LIMIT 5`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        if (nullSamples.length > 0) {
            nullSamples.forEach(row => {
                console.log(`   ${row.state_code} ${row.pay_frequency} ${row.filing_status}: $${row.wage_min}-$${row.wage_max}`);
            });
        }

    } catch (error) {
        console.error('❌ Check failed:', error);
        throw error;
    }
}

// Run if called directly
if (require.main === module) {
    checkDataIssues()
        .then(() => {
            console.log('\n✅ Check completed successfully');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Check failed:', err);
            process.exit(1);
        });
}

module.exports = { checkDataIssues };

