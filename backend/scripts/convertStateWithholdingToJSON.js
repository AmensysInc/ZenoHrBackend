const { getDatabase } = require('../database/init');
const fs = require('fs');
const path = require('path');

/**
 * Export existing state withholding tables to JSON format
 * Useful for converting approximate tables to JSON, then updating with official data
 */
async function exportStateWithholdingToJSON(stateCode, year = 2026, outputPath = null) {
    const db = getDatabase();

    console.log(`📤 Exporting state withholding tables to JSON...\n`);
    console.log(`State: ${stateCode}`);
    console.log(`Year: ${year}\n`);

    return new Promise((resolve, reject) => {
        db.all(`SELECT pay_frequency, filing_status, wage_min, wage_max, 
                       base_amount, percentage, withholding_amount
                FROM state_withholding_tables
                WHERE year = ? AND state_code = ?
                ORDER BY pay_frequency, filing_status, wage_min`,
            [year, stateCode],
            (err, rows) => {
                if (err) {
                    reject(err);
                    return;
                }

                if (rows.length === 0) {
                    console.log(`⚠️  No data found for ${stateCode} (${year})`);
                    resolve(null);
                    return;
                }

                // Group by pay frequency and filing status
                const tables = {};
                rows.forEach(row => {
                    const key = `${row.pay_frequency}_${row.filing_status}`;
                    if (!tables[key]) {
                        tables[key] = {
                            payFrequency: row.pay_frequency,
                            filingStatus: row.filing_status,
                            entries: []
                        };
                    }

                    tables[key].entries.push({
                        wageMin: row.wage_min,
                        wageMax: row.wage_max,
                        withholdingAmount: row.withholding_amount,
                        percentage: row.percentage,
                        baseAmount: row.base_amount
                    });
                });

                const output = {
                    state: stateCode,
                    year: year,
                    source: "Exported from database - UPDATE WITH OFFICIAL SOURCE",
                    lastUpdated: new Date().toISOString().split('T')[0],
                    notes: `State withholding tables for ${stateCode} - ${year}. Update with official tables from state revenue department.`,
                    tables: Object.values(tables)
                };

                // Write to file
                const filePath = outputPath || path.join(__dirname, `../data/state_withholding/${stateCode}_${year}.json`);
                const dir = path.dirname(filePath);
                
                if (!fs.existsSync(dir)) {
                    fs.mkdirSync(dir, { recursive: true });
                }

                fs.writeFileSync(filePath, JSON.stringify(output, null, 2));
                console.log(`✅ Exported ${rows.length} entries to ${filePath}`);
                console.log(`   Pay frequencies: ${Object.keys(tables).length} combinations`);

                resolve(output);
            }
        );
    });
}

/**
 * Export all states to JSON files
 */
async function exportAllStatesToJSON(year = 2026, outputDir = null) {
    const db = getDatabase();
    const outputDirectory = outputDir || path.join(__dirname, `../data/state_withholding`);

    console.log(`📤 Exporting all state withholding tables...\n`);
    console.log(`Year: ${year}`);
    console.log(`Output directory: ${outputDirectory}\n`);

    // Get all states
    return new Promise((resolve, reject) => {
        db.all(`SELECT DISTINCT state_code FROM state_withholding_tables WHERE year = ? ORDER BY state_code`,
            [year],
            async (err, states) => {
                if (err) {
                    reject(err);
                    return;
                }

                console.log(`Found ${states.length} states\n`);

                const results = [];
                for (const state of states) {
                    try {
                        const filePath = path.join(outputDirectory, `${state.state_code}_${year}.json`);
                        const result = await exportStateWithholdingToJSON(state.state_code, year, filePath);
                        results.push({ state: state.state_code, success: true, filePath });
                    } catch (error) {
                        console.error(`❌ Error exporting ${state.state_code}:`, error.message);
                        results.push({ state: state.state_code, success: false, error: error.message });
                    }
                }

                console.log(`\n${'='.repeat(80)}`);
                console.log('📊 SUMMARY:\n');
                const successful = results.filter(r => r.success).length;
                console.log(`   States exported: ${successful}/${states.length}`);
                console.log(`   Output directory: ${outputDirectory}`);

                resolve(results);
            }
        );
    });
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);

    if (args.length === 0 || args[0] === '--help') {
        console.log('Usage:');
        console.log('  node convertStateWithholdingToJSON.js <STATE_CODE> [year] [output_path]');
        console.log('  node convertStateWithholdingToJSON.js --all [year] [output_dir]');
        console.log('\nExamples:');
        console.log('  node convertStateWithholdingToJSON.js CA 2026');
        console.log('  node convertStateWithholdingToJSON.js MO 2026 data/MO_2026.json');
        console.log('  node convertStateWithholdingToJSON.js --all 2026 data/state_tables/');
        process.exit(1);
    }

    if (args[0] === '--all') {
        const year = args[1] ? parseInt(args[1]) : 2026;
        const outputDir = args[2] || null;
        exportAllStatesToJSON(year, outputDir)
            .then(() => {
                console.log('\n✅ Export completed');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Export failed:', err);
                process.exit(1);
            });
    } else {
        const stateCode = args[0];
        const year = args[1] ? parseInt(args[1]) : 2026;
        const outputPath = args[2] || null;
        exportStateWithholdingToJSON(stateCode, year, outputPath)
            .then(() => {
                console.log('\n✅ Export completed');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Export failed:', err);
                process.exit(1);
            });
    }
}

module.exports = {
    exportStateWithholdingToJSON,
    exportAllStatesToJSON
};

