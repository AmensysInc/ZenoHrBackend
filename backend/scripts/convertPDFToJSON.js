const fs = require('fs');
const path = require('path');

/**
 * Convert PDF withholding tables to JSON format
 * 
 * This script helps convert downloaded PDF files to JSON format.
 * Since PDF parsing is complex, it provides multiple approaches:
 * 1. Try automatic parsing (if pdf-parse is installed)
 * 2. Provide manual conversion guide
 * 3. Create template JSON files for manual entry
 */

/**
 * Try to parse PDF using pdf-parse library
 */
async function parsePDF(pdfPath) {
    try {
        const pdfParse = require('pdf-parse');
        const dataBuffer = fs.readFileSync(pdfPath);
        const data = await pdfParse(dataBuffer);
        return data.text;
    } catch (error) {
        if (error.code === 'MODULE_NOT_FOUND' || error.message.includes('Cannot find module')) {
            console.log('⚠️  pdf-parse library not installed');
            console.log('   Install: npm install pdf-parse');
            return null;
        }
        if (error.message && error.message.includes('pdfParse is not a function')) {
            // Try alternative require
            try {
                const pdfParseModule = require('pdf-parse');
                const pdfParse = pdfParseModule.default || pdfParseModule;
                const dataBuffer = fs.readFileSync(pdfPath);
                const data = await pdfParse(dataBuffer);
                return data.text;
            } catch (err2) {
                console.log('⚠️  Error parsing PDF:', err2.message);
                return null;
            }
        }
        throw error;
    }
}

/**
 * Extract state code from filename
 */
function getStateFromFilename(filename) {
    const match = filename.match(/([A-Z]{2})_\d{4}/);
    return match ? match[1] : null;
}

/**
 * Create template JSON from PDF info
 */
function createTemplateJSON(stateCode, year, pdfPath) {
    const template = {
        state: stateCode,
        year: year,
        source: `Parsed from PDF: ${path.basename(pdfPath)}`,
        lastUpdated: new Date().toISOString().split('T')[0],
        notes: `State withholding tables for ${stateCode} - ${year}. Update with official data from PDF.`,
        tables: [
            {
                payFrequency: "MONTHLY",
                filingStatus: "SINGLE",
                entries: [
                    {
                        wageMin: 0,
                        wageMax: 1000,
                        withholdingAmount: null,
                        percentage: null,
                        baseAmount: null
                    }
                ]
            },
            {
                payFrequency: "MONTHLY",
                filingStatus: "MARRIED",
                entries: []
            },
            {
                payFrequency: "WEEKLY",
                filingStatus: "SINGLE",
                entries: []
            },
            {
                payFrequency: "WEEKLY",
                filingStatus: "MARRIED",
                entries: []
            },
            {
                payFrequency: "BIWEEKLY",
                filingStatus: "SINGLE",
                entries: []
            },
            {
                payFrequency: "BIWEEKLY",
                filingStatus: "MARRIED",
                entries: []
            },
            {
                payFrequency: "SEMIMONTHLY",
                filingStatus: "SINGLE",
                entries: []
            },
            {
                payFrequency: "SEMIMONTHLY",
                filingStatus: "MARRIED",
                entries: []
            }
        ]
    };
    
    return template;
}

/**
 * Convert PDF to JSON (with manual assistance)
 */
async function convertPDFToJSON(pdfPath, stateCode = null, year = 2026) {
    const filename = path.basename(pdfPath);
    const state = stateCode || getStateFromFilename(filename) || 'XX';
    
    console.log(`📄 Converting PDF to JSON...\n`);
    console.log(`   PDF: ${pdfPath}`);
    console.log(`   State: ${state}`);
    console.log(`   Year: ${year}\n`);

    // Try to parse PDF
    const text = await parsePDF(pdfPath);
    
    if (text) {
        console.log(`✅ PDF parsed successfully`);
        console.log(`   Text length: ${text.length} characters\n`);
        console.log(`📝 First 500 characters:`);
        console.log(text.substring(0, 500));
        console.log(`\n⚠️  Automatic extraction from PDF text is complex and state-specific.`);
        console.log(`   You may need to manually extract data from the PDF.`);
    } else {
        console.log(`⚠️  Could not parse PDF automatically`);
        console.log(`   Creating template JSON file for manual entry\n`);
    }

    // Create output JSON file
    const outputDir = path.join(__dirname, '../data/state_withholding');
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    const jsonPath = path.join(outputDir, `${state}_${year}.json`);
    const template = createTemplateJSON(state, year, pdfPath);
    
    fs.writeFileSync(jsonPath, JSON.stringify(template, null, 2));
    
    console.log(`✅ Created template JSON: ${jsonPath}`);
    console.log(`\n📝 Next Steps:`);
    console.log(`   1. Open the PDF file: ${pdfPath}`);
    console.log(`   2. Open the JSON file: ${jsonPath}`);
    console.log(`   3. Extract data from PDF tables and fill in the JSON entries`);
    console.log(`   4. For each pay frequency and filing status:`);
    console.log(`      - Find the wage ranges in the PDF`);
    console.log(`      - Extract withholding amounts or percentages`);
    console.log(`      - Add entries to the JSON file`);
    console.log(`   5. Save the JSON file`);
    console.log(`   6. Import: node scripts/importOfficialStateWithholdingTables.js ${jsonPath} --clear`);
    
    if (text) {
        // Also save extracted text for reference
        const textPath = path.join(outputDir, `${state}_${year}_extracted_text.txt`);
        fs.writeFileSync(textPath, text);
        console.log(`\n💡 Extracted text saved to: ${textPath}`);
        console.log(`   You can search this file to find specific values`);
    }
    
    return jsonPath;
}

/**
 * Convert all PDFs in directory
 */
async function convertAllPDFs(directory, year = 2026) {
    const files = fs.readdirSync(directory)
        .filter(file => file.endsWith('.pdf') && file.includes('_raw'))
        .map(file => path.join(directory, file));
    
    console.log(`📁 Found ${files.length} PDF files to convert\n`);
    
    const results = [];
    for (const pdfPath of files) {
        try {
            const jsonPath = await convertPDFToJSON(pdfPath, null, year);
            results.push({ pdfPath, jsonPath, success: true });
            console.log('\n' + '='.repeat(80) + '\n');
        } catch (error) {
            console.error(`❌ Error converting ${pdfPath}:`, error.message);
            results.push({ pdfPath, success: false, error: error.message });
        }
    }
    
    return results;
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args[0] === '--help') {
        console.log('Usage:');
        console.log('  node convertPDFToJSON.js <pdf_file> [state_code] [year]');
        console.log('  node convertPDFToJSON.js --all [directory] [year]');
        console.log('\nExamples:');
        console.log('  node convertPDFToJSON.js data/state_withholding/MO_2026_raw.pdf MO 2026');
        console.log('  node convertPDFToJSON.js --all data/state_withholding 2026');
        process.exit(1);
    }
    
    if (args[0] === '--all') {
        const directory = args[1] || path.join(__dirname, '../data/state_withholding');
        const year = args[2] ? parseInt(args[2]) : 2026;
        convertAllPDFs(directory, year)
            .then(() => {
                console.log('\n✅ Conversion complete');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Conversion failed:', err);
                process.exit(1);
            });
    } else {
        const pdfPath = args[0];
        const stateCode = args[1] || null;
        const year = args[2] ? parseInt(args[2]) : 2026;
        convertPDFToJSON(pdfPath, stateCode, year)
            .then(() => {
                console.log('\n✅ Conversion complete');
                process.exit(0);
            })
            .catch((err) => {
                console.error('\n❌ Conversion failed:', err);
                process.exit(1);
            });
    }
}

module.exports = { convertPDFToJSON, convertAllPDFs };

