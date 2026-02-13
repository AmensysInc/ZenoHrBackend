const { getDatabase } = require('../database/init');

/**
 * Verify state withholding tables import
 * Checks data completeness, counts, and sample records
 */
async function verifyStateWithholdingTables() {
    const db = getDatabase();
    const year = 2026;

    console.log('🔍 Verifying State Withholding Tables Import...\n');

    // States with income tax
    const statesWithTax = [
        'AL', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'GA', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY',
        'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NJ', 'NM', 'NY', 'NC', 'ND',
        'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 'UT', 'VT', 'VA', 'WV', 'WI', 'DC'
    ];

    const payFrequencies = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
    const filingStatuses = ['SINGLE', 'MARRIED', 'MARRIED_SEPARATE', 'HEAD_OF_HOUSEHOLD'];

    try {
        // 1. Total count
        const totalCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`📊 Total Records: ${totalCount.toLocaleString()}`);
        console.log(`   Expected: ${statesWithTax.length * payFrequencies.length * filingStatuses.length * 1000} (approx)\n`);

        // 2. Count by state
        console.log('📈 Records by State:');
        const stateCounts = await new Promise((resolve, reject) => {
            db.all(`SELECT state_code, COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? 
                    GROUP BY state_code 
                    ORDER BY state_code`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        let missingStates = [];
        const foundStates = new Set();
        stateCounts.forEach(row => {
            foundStates.add(row.state_code);
            console.log(`   ${row.state_code}: ${row.count.toLocaleString()} records`);
        });

        statesWithTax.forEach(state => {
            if (!foundStates.has(state)) {
                missingStates.push(state);
            }
        });

        if (missingStates.length > 0) {
            console.log(`\n   ⚠️  Missing states: ${missingStates.join(', ')}`);
        } else {
            console.log(`\n   ✅ All ${statesWithTax.length} states present`);
        }

        // 3. Count by pay frequency
        console.log('\n📅 Records by Pay Frequency:');
        const freqCounts = await new Promise((resolve, reject) => {
            db.all(`SELECT pay_frequency, COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? 
                    GROUP BY pay_frequency 
                    ORDER BY pay_frequency`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        freqCounts.forEach(row => {
            const expected = statesWithTax.length * filingStatuses.length * 1000; // Approx
            console.log(`   ${row.pay_frequency}: ${row.count.toLocaleString()} records (expected ~${expected.toLocaleString()})`);
        });

        // 4. Count by filing status
        console.log('\n👥 Records by Filing Status:');
        const statusCounts = await new Promise((resolve, reject) => {
            db.all(`SELECT filing_status, COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? 
                    GROUP BY filing_status 
                    ORDER BY filing_status`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        statusCounts.forEach(row => {
            const expected = statesWithTax.length * payFrequencies.length * 1000; // Approx
            console.log(`   ${row.filing_status}: ${row.count.toLocaleString()} records (expected ~${expected.toLocaleString()})`);
        });

        // 5. Sample records for a few states
        console.log('\n📋 Sample Records (CA - Weekly - Single):');
        const samples = await new Promise((resolve, reject) => {
            db.all(`SELECT wage_min, wage_max, percentage, base_amount, withholding_amount
                    FROM state_withholding_tables 
                    WHERE year = ? AND state_code = 'CA' AND pay_frequency = 'WEEKLY' AND filing_status = 'SINGLE'
                    ORDER BY wage_min
                    LIMIT 10`, [year], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });

        console.log('   Wage Range          | Percentage | Base Amount | Withholding');
        console.log('   ' + '-'.repeat(70));
        samples.forEach(row => {
            const wageRange = `$${row.wage_min.toFixed(0).padStart(6)} - $${row.wage_max.toFixed(0).padStart(6)}`;
            const pct = row.percentage ? (row.percentage * 100).toFixed(4) + '%' : 'N/A';
            const base = row.base_amount ? '$' + row.base_amount.toFixed(2) : 'N/A';
            const withhold = row.withholding_amount ? '$' + row.withholding_amount.toFixed(2) : 'N/A';
            console.log(`   ${wageRange.padEnd(19)} | ${pct.padEnd(10)} | ${base.padEnd(11)} | ${withhold}`);
        });

        // 6. Check for data quality issues
        console.log('\n🔎 Data Quality Checks:');
        
        // Check for null percentages (shouldn't happen)
        const nullPercentages = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND percentage IS NULL`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`   Records with null percentage: ${nullPercentages} ${nullPercentages === 0 ? '✅' : '⚠️'}`);

        // Check for negative values
        const negativeWages = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND (wage_min < 0 OR wage_max < 0)`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`   Records with negative wages: ${negativeWages} ${negativeWages === 0 ? '✅' : '⚠️'}`);

        // Check for invalid wage ranges
        const invalidRanges = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND wage_min >= wage_max`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`   Records with invalid wage ranges: ${invalidRanges} ${invalidRanges === 0 ? '✅' : '⚠️'}`);

        // 7. Summary statistics
        console.log('\n📊 Summary Statistics:');
        const stats = await new Promise((resolve, reject) => {
            db.get(`SELECT 
                    MIN(wage_min) as min_wage,
                    MAX(wage_max) as max_wage,
                    AVG(percentage) as avg_percentage,
                    MIN(percentage) as min_percentage,
                    MAX(percentage) as max_percentage
                    FROM state_withholding_tables 
                    WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row);
            });
        });

        console.log(`   Minimum wage: $${stats.min_wage.toFixed(2)}`);
        console.log(`   Maximum wage: $${stats.max_wage.toFixed(2)}`);
        console.log(`   Average percentage: ${(stats.avg_percentage * 100).toFixed(4)}%`);
        console.log(`   Min percentage: ${(stats.min_percentage * 100).toFixed(4)}%`);
        console.log(`   Max percentage: ${(stats.max_percentage * 100).toFixed(4)}%`);

        // 8. Check specific state combinations
        console.log('\n✅ Verification Complete!\n');
        console.log('Sample state verification:');
        for (const state of ['CA', 'NY', 'TX']) {
            if (state === 'TX') continue; // TX doesn't have income tax
            const stateCount = await new Promise((resolve, reject) => {
                db.get(`SELECT COUNT(*) as count 
                        FROM state_withholding_tables 
                        WHERE year = ? AND state_code = ?`, [year, state], (err, row) => {
                    if (err) reject(err);
                    else resolve(row.count);
                });
            });
            console.log(`   ${state}: ${stateCount.toLocaleString()} records`);
        }

    } catch (error) {
        console.error('❌ Verification failed:', error);
        throw error;
    }
}

// Run if called directly
if (require.main === module) {
    verifyStateWithholdingTables()
        .then(() => {
            console.log('\n✅ Verification script completed successfully');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Verification script failed:', err);
            process.exit(1);
        });
}

module.exports = { verifyStateWithholdingTables };

