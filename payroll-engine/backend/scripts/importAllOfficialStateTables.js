const { importFromDirectory } = require('./importOfficialStateWithholdingTables');
const { exportAllStatesToJSON } = require('./convertStateWithholdingToJSON');
const fs = require('fs');
const path = require('path');

/**
 * Master script to import official state withholding tables for all states
 * 
 * This script:
 * 1. Exports current approximate tables (for reference)
 * 2. Imports official tables from JSON files
 * 3. Verifies the import
 */
async function importAllOfficialStateTables(options = {}) {
    const {
        year = 2026,
        dataDirectory = path.join(__dirname, '../data/state_withholding'),
        exportCurrent = true,
        clearExisting = true,
        verifyOnly = false
    } = options;

    console.log('🚀 Importing Official State Withholding Tables for ALL States\n');
    console.log('='.repeat(80));
    console.log(`Year: ${year}`);
    console.log(`Data Directory: ${dataDirectory}`);
    console.log(`Export Current: ${exportCurrent}`);
    console.log(`Clear Existing: ${clearExisting}`);
    console.log(`Verify Only: ${verifyOnly}\n`);

    // Step 1: Export current tables (for reference)
    if (exportCurrent && !verifyOnly) {
        console.log('📤 Step 1: Exporting current approximate tables...\n');
        try {
            await exportAllStatesToJSON(year, dataDirectory);
            console.log('\n✅ Current tables exported for reference\n');
        } catch (error) {
            console.error('⚠️  Error exporting current tables:', error.message);
            console.log('   Continuing with import...\n');
        }
    }

    // Step 2: Check if data directory exists
    if (!fs.existsSync(dataDirectory)) {
        console.log(`⚠️  Data directory not found: ${dataDirectory}`);
        console.log(`\n📝 Next Steps:`);
        console.log(`   1. Create directory: ${dataDirectory}`);
        console.log(`   2. Download official 2026 state withholding tables from state revenue departments`);
        console.log(`   3. Convert tables to JSON format (see IMPORT_OFFICIAL_TABLES_GUIDE.md)`);
        console.log(`   4. Place JSON files in: ${dataDirectory}`);
        console.log(`   5. Run this script again\n`);
        console.log(`   Template file: backend/scripts/state_withholding_template.json`);
        console.log(`   Guide: backend/scripts/IMPORT_OFFICIAL_TABLES_GUIDE.md\n`);
        return;
    }

    // Step 3: Find JSON files
    const files = fs.readdirSync(dataDirectory)
        .filter(file => file.endsWith('.json') && file.includes(year.toString()))
        .map(file => path.join(dataDirectory, file));

    if (files.length === 0) {
        console.log(`⚠️  No JSON files found in ${dataDirectory}`);
        console.log(`\n📝 Expected files:`);
        console.log(`   - CA_${year}.json`);
        console.log(`   - MO_${year}.json`);
        console.log(`   - NY_${year}.json`);
        console.log(`   - ... (one file per state)\n`);
        console.log(`   See IMPORT_OFFICIAL_TABLES_GUIDE.md for instructions\n`);
        return;
    }

    console.log(`📁 Found ${files.length} JSON files\n`);

    // Step 4: Import official tables
    console.log('📥 Step 2: Importing official state withholding tables...\n');
    try {
        const results = await importFromDirectory(dataDirectory, {
            clearExisting,
            year,
            verifyOnly
        });

        // Step 5: Summary
        console.log('\n' + '='.repeat(80));
        console.log('📊 FINAL SUMMARY\n');
        
        const successful = results.filter(r => r.success).length;
        const failed = results.filter(r => !r.success).length;
        const totalEntries = results.reduce((sum, r) => sum + (r.totalInserted || 0), 0);
        const totalErrors = results.reduce((sum, r) => sum + (r.totalErrors || 0), 0);

        console.log(`   Files processed: ${results.length}`);
        console.log(`   Successful: ${successful}`);
        console.log(`   Failed: ${failed}`);
        console.log(`   Total entries ${verifyOnly ? 'verified' : 'imported'}: ${totalEntries.toLocaleString()}`);
        console.log(`   Total errors: ${totalErrors}`);

        if (failed > 0) {
            console.log('\n   ⚠️  Failed imports:');
            results.filter(r => !r.success).forEach(r => {
                console.log(`      - ${path.basename(r.file)}: ${r.error || 'Unknown error'}`);
            });
        }

        if (!verifyOnly && successful > 0) {
            console.log('\n✅ Official state withholding tables imported successfully!');
            console.log('\n📝 Next Steps:');
            console.log('   1. Verify calculations match Paycom for test cases');
            console.log('   2. Run: node backend/scripts/checkStateWithholdingAlignment.js');
            console.log('   3. Test API endpoints with various scenarios');
            console.log('   4. Monitor for any calculation discrepancies');
        } else if (verifyOnly) {
            console.log('\n✅ Verification complete!');
            console.log('   Review results above and import when ready.');
        }

    } catch (error) {
        console.error('\n❌ Import failed:', error);
        throw error;
    }
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    
    const options = {
        year: 2026,
        exportCurrent: !args.includes('--no-export'),
        clearExisting: !args.includes('--no-clear'),
        verifyOnly: args.includes('--verify')
    };

    const yearIndex = args.indexOf('--year');
    if (yearIndex !== -1 && args[yearIndex + 1]) {
        options.year = parseInt(args[yearIndex + 1]);
    }

    const dirIndex = args.indexOf('--dir');
    if (dirIndex !== -1 && args[dirIndex + 1]) {
        options.dataDirectory = args[dirIndex + 1];
    }

    importAllOfficialStateTables(options)
        .then(() => {
            console.log('\n✅ Script completed');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Script failed:', err);
            process.exit(1);
        });
}

module.exports = { importAllOfficialStateTables };

