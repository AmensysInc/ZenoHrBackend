/**
 * Parse PDF withholding tables and convert to JSON
 * 
 * Note: This requires a PDF parsing library like pdf-parse or pdfjs-dist
 * Install: npm install pdf-parse
 */

const fs = require('fs');
const path = require('path');

/**
 * Parse PDF withholding table (requires pdf-parse library)
 */
async function parsePDFWithholdingTable(pdfPath, stateCode, year = 2026) {
    try {
        // Try to use pdf-parse if available
        const pdfParse = require('pdf-parse');
        
        const dataBuffer = fs.readFileSync(pdfPath);
        const data = await pdfParse(dataBuffer);
        
        console.log(`📄 Parsing PDF: ${pdfPath}`);
        console.log(`   Pages: ${data.numpages}`);
        console.log(`   Text length: ${data.text.length} characters`);
        
        // Extract text and try to parse tables
        const text = data.text;
        
        // This is a simplified parser - you may need to customize per state
        const tables = extractTablesFromText(text, stateCode);
        
        return {
            state: stateCode,
            year: year,
            source: `Parsed from PDF: ${path.basename(pdfPath)}`,
            lastUpdated: new Date().toISOString().split('T')[0],
            notes: `Auto-parsed from PDF - verify against official source`,
            tables: tables
        };
        
    } catch (error) {
        if (error.code === 'MODULE_NOT_FOUND') {
            console.error('❌ pdf-parse library not found');
            console.log('   Install it: npm install pdf-parse');
            console.log('   Or manually convert PDF to JSON using the template');
            return null;
        }
        throw error;
    }
}

/**
 * Extract tables from PDF text (simplified - needs customization per state)
 */
function extractTablesFromText(text, stateCode) {
    const tables = [];
    
    // Look for common patterns in withholding tables
    // This is a basic implementation - each state may have different formats
    
    // Try to find pay frequency headers
    const payFreqPatterns = {
        'WEEKLY': /weekly|week|w\s*$/i,
        'BIWEEKLY': /bi.?weekly|bi.?week|biw/i,
        'SEMIMONTHLY': /semi.?monthly|semi.?month|semimonth/i,
        'MONTHLY': /monthly|month|m\s*$/i
    };
    
    // Try to find filing status headers
    const filingStatusPatterns = {
        'SINGLE': /single|s\s*$/i,
        'MARRIED': /married|joint|m\s*$/i,
        'MARRIED_SEPARATE': /married.?separate|separate/i,
        'HEAD_OF_HOUSEHOLD': /head.?of.?household|hoh/i
    };
    
    // Split text into lines
    const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    
    // This is a placeholder - actual parsing needs to be customized per state
    // Each state PDF has different formatting
    
    console.log('⚠️  PDF parsing is state-specific and may require manual adjustment');
    console.log('   Consider using the template to manually create JSON files');
    
    return tables; // Empty for now - needs implementation
}

/**
 * Convert parsed data to JSON file
 */
async function convertPDFToJSON(pdfPath, stateCode, year = 2026, outputPath = null) {
    const parsed = await parsePDFWithholdingTable(pdfPath, stateCode, year);
    
    if (!parsed) {
        return null;
    }
    
    const outputDir = path.join(__dirname, '../data/state_withholding');
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    const jsonPath = outputPath || path.join(outputDir, `${stateCode}_${year}.json`);
    fs.writeFileSync(jsonPath, JSON.stringify(parsed, null, 2));
    
    console.log(`✅ Converted to JSON: ${jsonPath}`);
    
    return jsonPath;
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length < 1) {
        console.log('Usage:');
        console.log('  node parsePDFWithholdingTables.js <pdf_file> [state_code] [year] [output_path]');
        console.log('\nExample:');
        console.log('  node parsePDFWithholdingTables.js data/MO_2026_raw.pdf MO 2026');
        process.exit(1);
    }
    
    const pdfPath = args[0];
    const stateCode = args[1] || path.basename(pdfPath).split('_')[0].toUpperCase();
    const year = args[2] ? parseInt(args[2]) : 2026;
    const outputPath = args[3] || null;
    
    convertPDFToJSON(pdfPath, stateCode, year, outputPath)
        .then((jsonPath) => {
            if (jsonPath) {
                console.log('\n✅ Conversion complete');
                console.log(`   JSON file: ${jsonPath}`);
                console.log(`\n📝 Next: Review and verify the JSON file, then import:`);
                console.log(`   node scripts/importOfficialStateWithholdingTables.js ${jsonPath} --clear`);
            }
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Conversion failed:', err);
            process.exit(1);
        });
}

module.exports = { parsePDFWithholdingTable, convertPDFToJSON };

