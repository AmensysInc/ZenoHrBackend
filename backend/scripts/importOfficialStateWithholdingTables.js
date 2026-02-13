const { getDatabase } = require('../database/init');
const fs = require('fs');
const path = require('path');

/**
 * Import official state withholding tables from JSON files
 * Supports multiple formats and all states
 * 
 * JSON Format:
 * {
 *   "state": "CA",
 *   "year": 2026,
 *   "tables": [
 *     {
 *       "payFrequency": "MONTHLY",
 *       "filingStatus": "SINGLE",
 *       "entries": [
 *         {
 *           "wageMin": 0,
 *           "wageMax": 1000,
 *           "withholdingAmount": null,
 *           "percentage": 0.01,
 *           "baseAmount": null
 *         }
 *       ]
 *     }
 *   ]
 * }
 */
async function importOfficialStateWithholdingTables(filePath, options = {}) {
    const db = getDatabase();
    const {
        clearExisting = false,
        stateCode = null, // If specified, only import for this state
        year = 2026,
        verifyOnly = false // If true, only verify, don't import
    } = options;

    console.log('📥 Importing Official State Withholding Tables\n');
    console.log(`File: ${filePath}`);
    console.log(`Year: ${year}`);
    console.log(`State Filter: ${stateCode || 'ALL'}`);
    console.log(`Mode: ${verifyOnly ? 'VERIFY ONLY' : 'IMPORT'}\n`);

    // Read JSON file
    let data;
    try {
        const fileContent = fs.readFileSync(filePath, 'utf8');
        data = JSON.parse(fileContent);
    } catch (error) {
        throw new Error(`Failed to read/parse JSON file: ${error.message}`);
    }

    // Handle single state or array of states
    const states = Array.isArray(data) ? data : [data];
    
    let totalInserted = 0;
    let totalErrors = 0;
    const errors = [];

    for (const stateData of states) {
        const state = stateData.state || stateData.stateCode;
        const stateYear = stateData.year || year;

        // Filter by state if specified
        if (stateCode && state !== stateCode) {
            continue;
        }

        // Filter by year
        if (stateYear !== year) {
            console.log(`⚠️  Skipping ${state} - year ${stateYear} (expected ${year})`);
            continue;
        }

        console.log(`\n📋 Processing ${state} (${stateYear})...`);

        if (!stateData.tables || !Array.isArray(stateData.tables)) {
            console.log(`  ⚠️  No tables found for ${state}, skipping...`);
            continue;
        }

        // Clear existing data for this state if requested
        if (clearExisting && !verifyOnly) {
            await new Promise((resolve, reject) => {
                db.run(`DELETE FROM state_withholding_tables WHERE year = ? AND state_code = ?`, 
                    [stateYear, state], (err) => {
                    if (err) reject(err);
                    else {
                        console.log(`  ✅ Cleared existing data for ${state}`);
                        resolve();
                    }
                });
            });
        }

        // Process each table
        for (const table of stateData.tables) {
            const payFreq = (table.payFrequency || table.pay_frequency || '').toUpperCase();
            const filingStatus = (table.filingStatus || table.filing_status || 'SINGLE').toUpperCase();

            if (!payFreq) {
                console.log(`  ⚠️  Skipping table - missing payFrequency`);
                continue;
            }

            if (!table.entries || !Array.isArray(table.entries)) {
                console.log(`  ⚠️  Skipping ${payFreq}/${filingStatus} - no entries`);
                continue;
            }

            console.log(`  📊 ${payFreq}/${filingStatus}: ${table.entries.length} entries`);

            let inserted = 0;
            let tableErrors = 0;

            for (const entry of table.entries) {
                const wageMin = parseFloat(entry.wageMin || entry.wage_min || 0);
                const wageMax = parseFloat(entry.wageMax || entry.wage_max || null);
                const withholdingAmount = entry.withholdingAmount !== undefined && entry.withholdingAmount !== null 
                    ? parseFloat(entry.withholdingAmount) : null;
                const percentage = entry.percentage !== undefined && entry.percentage !== null 
                    ? parseFloat(entry.percentage) : null;
                const baseAmount = entry.baseAmount !== undefined && entry.baseAmount !== null 
                    ? parseFloat(entry.baseAmount) : null;

                // Validate entry
                if (isNaN(wageMin) || wageMin < 0) {
                    tableErrors++;
                    errors.push(`${state} ${payFreq} ${filingStatus}: Invalid wageMin ${entry.wageMin}`);
                    continue;
                }

                if (wageMax !== null && (isNaN(wageMax) || wageMax <= wageMin)) {
                    tableErrors++;
                    errors.push(`${state} ${payFreq} ${filingStatus}: Invalid wageMax ${entry.wageMax}`);
                    continue;
                }

                // At least one calculation method must be provided
                if (withholdingAmount === null && percentage === null && baseAmount === null) {
                    tableErrors++;
                    errors.push(`${state} ${payFreq} ${filingStatus}: No calculation method provided`);
                    continue;
                }

                if (!verifyOnly) {
                    // Insert into database
                    db.run(`INSERT OR REPLACE INTO state_withholding_tables 
                            (year, state_code, pay_frequency, filing_status, wage_min, wage_max, base_amount, percentage, withholding_amount)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                        [
                            stateYear,
                            state,
                            payFreq,
                            filingStatus,
                            wageMin,
                            wageMax,
                            baseAmount,
                            percentage,
                            withholdingAmount
                        ],
                        (err) => {
                            if (err) {
                                tableErrors++;
                                errors.push(`${state} ${payFreq} ${filingStatus}: ${err.message}`);
                            } else {
                                inserted++;
                                totalInserted++;
                            }
                        }
                    );
                } else {
                    // Just count for verification
                    inserted++;
                }
            }

            if (tableErrors > 0) {
                console.log(`    ⚠️  ${tableErrors} errors`);
            } else {
                console.log(`    ✅ ${inserted} entries ${verifyOnly ? 'verified' : 'imported'}`);
            }
        }
    }

    // Wait for all inserts to complete
    if (!verifyOnly) {
        await new Promise(resolve => setTimeout(resolve, 2000));
    }

    console.log(`\n${'='.repeat(80)}`);
    if (verifyOnly) {
        console.log(`✅ Verification complete!`);
        console.log(`   Total entries verified: ${totalInserted}`);
    } else {
        console.log(`✅ Import complete!`);
        console.log(`   Total entries imported: ${totalInserted}`);
    }
    
    if (errors.length > 0) {
        console.log(`\n⚠️  Errors encountered: ${errors.length}`);
        errors.slice(0, 10).forEach(err => console.log(`   - ${err}`));
        if (errors.length > 10) {
            console.log(`   ... and ${errors.length - 10} more errors`);
        }
        totalErrors = errors.length;
    }

    return {
        success: totalErrors === 0,
        totalInserted,
        totalErrors,
        errors: errors.slice(0, 20) // Return first 20 errors
    };
}

/**
 * Import from directory containing multiple JSON files (one per state)
 */
async function importFromDirectory(directoryPath, options = {}) {
    const {
        clearExisting = false,
        year = 2026,
        verifyOnly = false
    } = options;

    console.log('📁 Importing from directory...\n');
    console.log(`Directory: ${directoryPath}`);
    console.log(`Year: ${year}\n`);

    const files = fs.readdirSync(directoryPath)
        .filter(file => file.endsWith('.json'))
        .map(file => path.join(directoryPath, file));

    console.log(`Found ${files.length} JSON files\n`);

    let totalStates = 0;
    let totalSuccess = 0;
    const results = [];

    for (const file of files) {
        try {
            console.log(`\n${'='.repeat(80)}`);
            const result = await importOfficialStateWithholdingTables(file, {
                clearExisting: totalStates === 0 ? clearExisting : false, // Only clear on first state
                year,
                verifyOnly
            });
            
            results.push({ file, ...result });
            totalStates++;
            if (result.success) totalSuccess++;
        } catch (error) {
            console.error(`❌ Error processing ${file}:`, error.message);
            results.push({ file, success: false, error: error.message });
        }
    }

    console.log(`\n${'='.repeat(80)}`);
    console.log('📊 SUMMARY:\n');
    console.log(`   Files processed: ${totalStates}`);
    console.log(`   Successful: ${totalSuccess}`);
    console.log(`   Failed: ${totalStates - totalSuccess}`);

    return results;
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length === 0) {
        console.log('Usage:');
        console.log('  node importOfficialStateWithholdingTables.js <file.json> [options]');
        console.log('  node importOfficialStateWithholdingTables.js --dir <directory> [options]');
        console.log('\nOptions:');
        console.log('  --clear          Clear existing data before import');
        console.log('  --verify         Verify only, don\'t import');
        console.log('  --state <CODE>   Import only for specified state');
        console.log('  --year <YEAR>    Year (default: 2026)');
        console.log('\nExample:');
        console.log('  node importOfficialStateWithholdingTables.js data/CA_2026.json --clear');
        console.log('  node importOfficialStateWithholdingTables.js --dir data/state_tables/ --clear');
        process.exit(1);
    }

    const options = {
        clearExisting: args.includes('--clear'),
        verifyOnly: args.includes('--verify'),
        year: 2026
    };

    const stateIndex = args.indexOf('--state');
    if (stateIndex !== -1 && args[stateIndex + 1]) {
        options.stateCode = args[stateIndex + 1];
    }

    const yearIndex = args.indexOf('--year');
    if (yearIndex !== -1 && args[yearIndex + 1]) {
        options.year = parseInt(args[yearIndex + 1]);
    }

    const dirIndex = args.indexOf('--dir');
    if (dirIndex !== -1 && args[dirIndex + 1]) {
        const directory = args[dirIndex + 1];
        importFromDirectory(directory, options)
            .then(() => {
                console.log('\n✅ Script completed successfully');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Script failed:', err);
                process.exit(1);
            });
    } else {
        const filePath = args[0];
        importOfficialStateWithholdingTables(filePath, options)
            .then(() => {
                console.log('\n✅ Script completed successfully');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Script failed:', err);
                process.exit(1);
            });
    }
}

module.exports = {
    importOfficialStateWithholdingTables,
    importFromDirectory
};

