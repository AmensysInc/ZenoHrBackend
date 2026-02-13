const { getDatabase } = require('../database/init');

/**
 * Check if state withholding tables are aligned with actual 2026 official state withholding tables
 * This script identifies which states need official table verification
 */
async function checkStateWithholdingAlignment() {
    const db = getDatabase();
    const year = 2026;

    console.log('🔍 Checking State Withholding (W/H) Alignment for 2026\n');
    console.log('='.repeat(80));

    // States with income tax
    const statesWithTax = [
        'AL', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'GA', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY',
        'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NJ', 'NM', 'NY', 'NC', 'ND',
        'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 'UT', 'VT', 'VA', 'WV', 'WI', 'DC'
    ];

    const payFrequencies = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
    const filingStatuses = ['SINGLE', 'MARRIED', 'MARRIED_SEPARATE', 'HEAD_OF_HOUSEHOLD'];

    // Known states that require official withholding tables (not bracket-based)
    // These states have documented issues or use complex withholding formulas
    const statesRequiringOfficialTables = {
        'MO': {
            reason: 'Missouri uses official withholding tables that differ from bracket calculations',
            source: 'Missouri Department of Revenue (MODOR)',
            issue: 'Bracket method produces incorrect results (see MISSOURI_WITHHOLDING_ISSUE.md)'
        },
        'CO': {
            reason: 'Colorado uses flat rate but may have specific withholding table adjustments',
            source: 'Colorado Department of Revenue',
            issue: 'Verify flat rate withholding matches official tables'
        },
        'PA': {
            reason: 'Pennsylvania uses flat rate but verify against official tables',
            source: 'Pennsylvania Department of Revenue',
            issue: 'Flat rate may need verification'
        },
        'NY': {
            reason: 'New York has complex withholding rules with local taxes',
            source: 'New York State Department of Taxation and Finance',
            issue: 'Verify withholding tables match official NY tables'
        },
        'CA': {
            reason: 'California has complex withholding with multiple brackets',
            source: 'California Franchise Tax Board',
            issue: 'Verify bracket-based tables match official CA withholding tables'
        }
    };

    try {
        // 1. Check database status
        console.log('\n📊 Database Status:\n');
        const totalCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`   Total withholding table records (2026): ${totalCount.toLocaleString()}`);

        const stateCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(DISTINCT state_code) as count FROM state_withholding_tables WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });
        console.log(`   States with data: ${stateCount}`);

        // 2. Check each state
        console.log('\n📋 State-by-State Analysis:\n');
        console.log('State | Records | Status | Notes');
        console.log('-'.repeat(80));

        const stateAnalysis = [];

        for (const stateCode of statesWithTax) {
            const stateRecords = await new Promise((resolve, reject) => {
                db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ? AND state_code = ?`, 
                    [year, stateCode], (err, row) => {
                    if (err) reject(err);
                    else resolve(row.count);
                });
            });

            // Check if state has complete data (all pay frequencies and filing statuses)
            const expectedRecords = payFrequencies.length * filingStatuses.length * 1000; // Approx 1000 wage ranges per combination
            const hasData = stateRecords > 0;
            const isComplete = stateRecords >= expectedRecords * 0.8; // 80% threshold

            // Check if state uses bracket-based (approximate) or official tables
            const isApproximate = true; // Current system generates approximate tables
            const needsOfficial = statesRequiringOfficialTables[stateCode] !== undefined;

            let status = '✅';
            let notes = '';
            
            if (!hasData) {
                status = '❌';
                notes = 'No data found';
            } else if (!isComplete) {
                status = '⚠️';
                notes = `Incomplete (${stateRecords.toLocaleString()} records, expected ~${expectedRecords.toLocaleString()})`;
            } else if (needsOfficial) {
                status = '⚠️';
                notes = `Needs official table verification - ${statesRequiringOfficialTables[stateCode].reason}`;
            } else if (isApproximate) {
                status = '⚠️';
                notes = 'Using approximate tables (generated from brackets)';
            } else {
                status = '✅';
                notes = 'Data present';
            }

            stateAnalysis.push({
                state: stateCode,
                records: stateRecords,
                status: status,
                notes: notes,
                needsOfficial: needsOfficial
            });

            console.log(`${stateCode.padEnd(6)} | ${stateRecords.toString().padStart(8)} | ${status.padEnd(6)} | ${notes}`);
        }

        // 3. Summary of states needing official tables
        console.log('\n\n⚠️  States Requiring Official Withholding Table Verification:\n');
        const needsVerification = stateAnalysis.filter(s => s.needsOfficial || s.status === '⚠️');
        
        if (needsVerification.length === 0) {
            console.log('   ✅ All states appear to have adequate data');
        } else {
            needsVerification.forEach(state => {
                console.log(`   ${state.state}: ${state.notes}`);
                if (statesRequiringOfficialTables[state.state]) {
                    const info = statesRequiringOfficialTables[state.state];
                    console.log(`      - Source: ${info.source}`);
                    console.log(`      - Issue: ${info.issue}`);
                }
            });
        }

        // 4. Check data quality indicators
        console.log('\n\n🔎 Data Quality Indicators:\n');

        // Check for states with very low record counts
        const lowRecordStates = stateAnalysis.filter(s => s.records > 0 && s.records < 1000);
        if (lowRecordStates.length > 0) {
            console.log('   ⚠️  States with low record counts (< 1000):');
            lowRecordStates.forEach(s => {
                console.log(`      - ${s.state}: ${s.records} records`);
            });
        } else {
            console.log('   ✅ All states have adequate record counts');
        }

        // Check sample calculations for key states
        console.log('\n\n🧪 Sample Calculation Verification:\n');
        const testStates = ['CA', 'NY', 'MO', 'CO', 'TX'];
        const testGrossPay = 5000; // Monthly
        const testFilingStatus = 'SINGLE';
        const testPayFreq = 'MONTHLY';

        for (const stateCode of testStates) {
            if (stateCode === 'TX') {
                console.log(`   ${stateCode}: No state income tax`);
                continue;
            }

            const sample = await new Promise((resolve, reject) => {
                db.get(`SELECT * FROM state_withholding_tables 
                        WHERE year = ? AND state_code = ? AND pay_frequency = ? AND filing_status = ?
                        AND ? >= wage_min AND ? < wage_max
                        LIMIT 1`,
                    [year, stateCode, testPayFreq, testFilingStatus, testGrossPay, testGrossPay],
                    (err, row) => {
                        if (err) reject(err);
                        else resolve(row);
                    }
                );
            });

            if (sample) {
                let withholding = 0;
                if (sample.withholding_amount !== null) {
                    withholding = sample.withholding_amount;
                } else if (sample.percentage !== null) {
                    if (sample.base_amount !== null) {
                        withholding = sample.base_amount + (testGrossPay * sample.percentage);
                    } else {
                        withholding = testGrossPay * sample.percentage;
                    }
                } else if (sample.base_amount !== null) {
                    withholding = sample.base_amount;
                }

                console.log(`   ${stateCode}: $${testGrossPay.toLocaleString()} gross → $${withholding.toFixed(2)} withholding`);
                console.log(`      (Method: ${sample.withholding_amount ? 'Fixed' : sample.percentage ? 'Percentage' : 'Base'})`);
            } else {
                console.log(`   ${stateCode}: ⚠️  No table entry found for $${testGrossPay} (MONTHLY, SINGLE)`);
            }
        }

        // 5. Recommendations
        console.log('\n\n📝 Recommendations:\n');
        console.log('   1. ⚠️  Current system uses APPROXIMATE withholding tables generated from state brackets');
        console.log('   2. ⚠️  For production accuracy, import OFFICIAL state withholding tables from each state\'s revenue department');
        console.log('   3. ⚠️  States with known issues (MO, CO, etc.) should be prioritized for official table import');
        console.log('   4. ✅ Verify calculations against payroll systems (Paycom, ADP, etc.) for accuracy');
        console.log('   5. 📋 Official state withholding tables are typically published annually by state revenue departments');
        console.log('   6. 🔗 Check state revenue department websites for 2026 withholding table publications');

        // 6. Official sources reference
        console.log('\n\n📚 Official State Revenue Department Resources:\n');
        console.log('   - Most states publish withholding tables on their revenue/taxation department websites');
        console.log('   - Look for "Withholding Tables", "Employer Withholding", or "W-4 Equivalent" forms');
        console.log('   - Tables are typically updated annually for the new tax year');
        console.log('   - Some states use percentage method, others use wage bracket method');
        console.log('   - Verify 2026 tables are available and match database entries');

        // 7. Final summary
        console.log('\n\n' + '='.repeat(80));
        console.log('📊 SUMMARY:\n');
        const totalStates = stateAnalysis.length;
        const statesWithData = stateAnalysis.filter(s => s.records > 0).length;
        const statesComplete = stateAnalysis.filter(s => s.status === '✅').length;
        const statesNeedingVerification = needsVerification.length;

        console.log(`   Total states with income tax: ${totalStates}`);
        console.log(`   States with data: ${statesWithData} (${((statesWithData/totalStates)*100).toFixed(1)}%)`);
        console.log(`   States with complete data: ${statesComplete} (${((statesComplete/totalStates)*100).toFixed(1)}%)`);
        console.log(`   States needing verification: ${statesNeedingVerification} (${((statesNeedingVerification/totalStates)*100).toFixed(1)}%)`);
        
        console.log('\n   ⚠️  IMPORTANT: Current tables are APPROXIMATE and generated from brackets.');
        console.log('   For production use, verify against official 2026 state withholding tables.\n');

    } catch (error) {
        console.error('❌ Verification failed:', error);
        throw error;
    }
}

// Run if called directly
if (require.main === module) {
    checkStateWithholdingAlignment()
        .then(() => {
            console.log('\n✅ Alignment check completed');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Alignment check failed:', err);
            process.exit(1);
        });
}

module.exports = { checkStateWithholdingAlignment };

