const { getDatabase } = require('../database/init');
const { importFromDirectory } = require('./importOfficialStateWithholdingTables');
const path = require('path');

/**
 * Clear all old approximate tables and import official tables for all states
 */
async function clearAndImportAllOfficialTables() {
    const db = getDatabase();
    const year = 2026;
    const dataDir = path.join(__dirname, '../data/state_withholding');
    
    console.log('🧹 Clearing Old Approximate Tables and Importing Official Tables\n');
    console.log('='.repeat(80));
    
    // Step 1: Clear ALL old approximate tables
    console.log('📋 Step 1: Clearing all old approximate withholding tables...\n');
    
    await new Promise((resolve, reject) => {
        db.run(`DELETE FROM state_withholding_tables WHERE year = ?`, [year], (err) => {
            if (err) {
                reject(err);
            } else {
                db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ?`, [year], (err2, row) => {
                    if (err2) {
                        reject(err2);
                    } else {
                        console.log(`✅ Cleared all tables. Remaining records: ${row.count}\n`);
                        resolve();
                    }
                });
            }
        });
    });
    
    // Step 2: Import only official tables (files ending with _official.json)
    console.log('📋 Step 2: Importing official withholding tables...\n');
    
    const fs = require('fs');
    const officialFiles = fs.readdirSync(dataDir)
        .filter(file => file.endsWith('_official.json'))
        .map(file => path.join(dataDir, file));
    
    console.log(`Found ${officialFiles.length} official table files\n`);
    
    let totalInserted = 0;
    const results = [];
    
    for (const file of officialFiles) {
        try {
            const result = await importOfficialStateWithholdingTables(file, {
                clearExisting: false, // Already cleared above
                year: year,
                verifyOnly: false
            });
            
            results.push({ file: path.basename(file), ...result });
            totalInserted += result.totalInserted || 0;
        } catch (error) {
            console.error(`❌ Error processing ${file}:`, error.message);
            results.push({ file: path.basename(file), success: false, error: error.message });
        }
    }
    
    // Step 3: Summary
    console.log('\n' + '='.repeat(80));
    console.log('📊 FINAL SUMMARY\n');
    
    const successful = results.filter(r => r.success).length;
    const failed = results.filter(r => !r.success).length;
    
    console.log(`   Files processed: ${results.length}`);
    console.log(`   Successful: ${successful}`);
    console.log(`   Failed: ${failed}`);
    console.log(`   Total entries imported: ${totalInserted.toLocaleString()}`);
    
    // Verify import
    db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = ?`, [year], (err, row) => {
        if (!err) {
            console.log(`   Total records in database: ${row.count.toLocaleString()}`);
        }
        
        console.log('\n✅ Import complete!');
        console.log('\n📝 Next Steps:');
        console.log('   1. Test calculations for multiple states');
        console.log('   2. Verify system uses tables (not bracket fallback)');
        console.log('   3. Compare with Paycom for accuracy');
        
        process.exit(0);
    });
}

// Import the function we need
const { importOfficialStateWithholdingTables } = require('./importOfficialStateWithholdingTables');

if (require.main === module) {
    clearAndImportAllOfficialTables().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = { clearAndImportAllOfficialTables };

