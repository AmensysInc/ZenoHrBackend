const { getDatabase } = require('../database/init');

/**
 * Clean up state withholding tables:
 * 1. Remove duplicate records
 * 2. Fix null percentages (set to 0 for zero wage ranges)
 */
async function cleanupStateWithholdingTables() {
    const db = getDatabase();
    const year = 2026;

    console.log('🧹 Cleaning up State Withholding Tables...\n');

    try {
        // 1. Count duplicates before cleanup
        const duplicateCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) - COUNT(DISTINCT state_code || '|' || pay_frequency || '|' || filing_status || '|' || wage_min || '|' || wage_max) as duplicates
                    FROM state_withholding_tables 
                    WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.duplicates);
            });
        });

        console.log(`📊 Duplicate records found: ${duplicateCount}`);

        if (duplicateCount > 0) {
            console.log('   Removing duplicates...');
            
            // Use a loop to remove duplicates until none remain
            let iterations = 0;
            let maxIterations = 10;
            let removed = 0;
            
            while (iterations < maxIterations) {
                const result = await new Promise((resolve, reject) => {
                    db.run(`DELETE FROM state_withholding_tables 
                            WHERE year = ? AND id NOT IN (
                                SELECT MIN(id) 
                                FROM state_withholding_tables 
                                WHERE year = ?
                                GROUP BY state_code, pay_frequency, filing_status, wage_min, wage_max
                            )`, [year, year], function(err) {
                        if (err) reject(err);
                        else resolve(this.changes);
                    });
                });
                
                removed += result;
                iterations++;
                
                if (result === 0) {
                    // No more duplicates removed, break
                    break;
                }
            }
            
            console.log(`   ✅ Removed ${removed} duplicate records in ${iterations} iteration(s)`);
        } else {
            console.log('   ✅ No duplicates found');
        }

        // 2. Fix null percentages
        const nullCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND percentage IS NULL`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });

        console.log(`\n📊 Records with null percentage: ${nullCount}`);

        if (nullCount > 0) {
            console.log('   Fixing null percentages...');
            
            // Set null percentages to 0 (for zero wage ranges, no withholding)
            await new Promise((resolve, reject) => {
                db.run(`UPDATE state_withholding_tables 
                        SET percentage = 0 
                        WHERE year = ? AND percentage IS NULL`, [year], (err) => {
                    if (err) reject(err);
                    else {
                        console.log('   ✅ Null percentages fixed (set to 0)');
                        resolve();
                    }
                });
            });
        } else {
            console.log('   ✅ No null percentages found');
        }

        // 3. Final count
        const finalCount = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });

        console.log(`\n📊 Final record count: ${finalCount.toLocaleString()}`);

        // 4. Verify no duplicates remain
        const remainingDuplicates = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) - COUNT(DISTINCT state_code || '|' || pay_frequency || '|' || filing_status || '|' || wage_min || '|' || wage_max) as duplicates
                    FROM state_withholding_tables 
                    WHERE year = ?`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.duplicates);
            });
        });

        console.log(`📊 Remaining duplicates: ${remainingDuplicates} ${remainingDuplicates === 0 ? '✅' : '⚠️'}`);

        // 5. Verify no null percentages remain
        const remainingNulls = await new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count 
                    FROM state_withholding_tables 
                    WHERE year = ? AND percentage IS NULL`, [year], (err, row) => {
                if (err) reject(err);
                else resolve(row.count);
            });
        });

        console.log(`📊 Remaining null percentages: ${remainingNulls} ${remainingNulls === 0 ? '✅' : '⚠️'}`);

        console.log('\n✅ Cleanup complete!');

    } catch (error) {
        console.error('❌ Cleanup failed:', error);
        throw error;
    }
}

// Run if called directly
if (require.main === module) {
    cleanupStateWithholdingTables()
        .then(() => {
            console.log('\n✅ Cleanup script completed successfully');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Cleanup script failed:', err);
            process.exit(1);
        });
}

module.exports = { cleanupStateWithholdingTables };

