const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const { importOfficialStateWithholdingTables } = require('./importOfficialStateWithholdingTables');

/**
 * Fetch official state withholding tables from various sources
 * Attempts to download and convert to JSON format
 */
class StateWithholdingFetcher {
    constructor() {
        this.dataDir = path.join(__dirname, '../data/state_withholding');
        this.year = 2026;
        
        // Ensure data directory exists
        if (!fs.existsSync(this.dataDir)) {
            fs.mkdirSync(this.dataDir, { recursive: true });
        }
    }

    /**
     * Known sources for 2026 state withholding tables
     */
    getStateSources() {
        return {
            'MO': {
                url: 'https://dor.mo.gov/forms/Withholding%20Table%20-%20Monthly_2026.pdf',
                format: 'pdf',
                notes: 'Missouri Department of Revenue - Monthly withholding table'
            },
            'OR': {
                url: 'https://www.oregon.gov/dor/forms/FormsPubs/withholding-tax-tables_206-430_2026.pdf',
                format: 'pdf',
                notes: 'Oregon Department of Revenue - 2026 withholding tables'
            },
            'HI': {
                url: 'https://files.hawaii.gov/tax/legal/har_temp/Appendix2_Income_Tax_WH_Tables_TY_after_12-31-25.pdf',
                format: 'pdf',
                notes: 'Hawaii Department of Taxation - 2026 withholding tables'
            },
            // Add more states as sources are found
            'CA': {
                url: 'https://www.ftb.ca.gov/forms/2026/2026-california-withholding-tables.pdf',
                format: 'pdf',
                notes: 'California Franchise Tax Board - 2026 withholding tables',
                alternative: 'https://www.ftb.ca.gov/forms/search'
            },
            'NY': {
                url: 'https://www.tax.ny.gov/pdf/2026/withholding/2026-ny-withholding-tables.pdf',
                format: 'pdf',
                notes: 'New York State Department of Taxation - 2026 withholding tables',
                alternative: 'https://www.tax.ny.gov/forms/withholding_forms.htm'
            }
        };
    }

    /**
     * Download file from URL
     */
    async downloadFile(url, outputPath) {
        return new Promise((resolve, reject) => {
            const protocol = url.startsWith('https') ? https : http;
            
            const file = fs.createWriteStream(outputPath);
            
            protocol.get(url, (response) => {
                if (response.statusCode === 301 || response.statusCode === 302) {
                    // Handle redirect
                    return this.downloadFile(response.headers.location, outputPath)
                        .then(resolve)
                        .catch(reject);
                }
                
                if (response.statusCode !== 200) {
                    reject(new Error(`Failed to download: ${response.statusCode} ${response.statusMessage}`));
                    return;
                }
                
                response.pipe(file);
                
                file.on('finish', () => {
                    file.close();
                    resolve(outputPath);
                });
            }).on('error', (err) => {
                fs.unlink(outputPath, () => {}); // Delete file on error
                reject(err);
            });
        });
    }

    /**
     * Fetch state withholding table
     */
    async fetchState(stateCode) {
        const sources = this.getStateSources();
        const source = sources[stateCode];
        
        if (!source) {
            console.log(`⚠️  No known source for ${stateCode}`);
            return null;
        }

        console.log(`\n📥 Fetching ${stateCode}...`);
        console.log(`   Source: ${source.url}`);
        console.log(`   Format: ${source.format.toUpperCase()}`);

        const outputPath = path.join(this.dataDir, `${stateCode}_${this.year}_raw.${source.format}`);
        
        try {
            await this.downloadFile(source.url, outputPath);
            console.log(`   ✅ Downloaded to: ${outputPath}`);
            return { stateCode, filePath: outputPath, format: source.format, source };
        } catch (error) {
            console.log(`   ❌ Download failed: ${error.message}`);
            if (source.alternative) {
                console.log(`   💡 Try alternative: ${source.alternative}`);
            }
            return null;
        }
    }

    /**
     * Fetch all available states
     */
    async fetchAllStates() {
        const sources = this.getStateSources();
        const results = [];

        console.log('🚀 Fetching Official State Withholding Tables\n');
        console.log(`Year: ${this.year}`);
        console.log(`Output Directory: ${this.dataDir}`);
        console.log(`States with known sources: ${Object.keys(sources).length}\n`);

        for (const stateCode of Object.keys(sources)) {
            const result = await this.fetchState(stateCode);
            if (result) {
                results.push(result);
            }
            // Small delay to avoid overwhelming servers
            await new Promise(resolve => setTimeout(resolve, 1000));
        }

        console.log(`\n${'='.repeat(80)}`);
        console.log('📊 SUMMARY:\n');
        console.log(`   States attempted: ${Object.keys(sources).length}`);
        console.log(`   Successfully downloaded: ${results.length}`);
        console.log(`   Failed: ${Object.keys(sources).length - results.length}`);

        if (results.length > 0) {
            console.log(`\n📁 Downloaded files:`);
            results.forEach(r => {
                console.log(`   - ${r.stateCode}: ${r.filePath}`);
            });
            console.log(`\n📝 Next Steps:`);
            console.log(`   1. Convert PDF files to JSON format (manual or using PDF parser)`);
            console.log(`   2. Use template: backend/scripts/state_withholding_template.json`);
            console.log(`   3. Import using: node scripts/importOfficialStateWithholdingTables.js`);
        }

        return results;
    }
}

/**
 * Alternative: Use third-party API or service to get withholding tables
 * Note: This is a placeholder - you may need to use a paid service or manual conversion
 */
async function fetchFromThirdPartyService() {
    // Some services provide APIs, but most require manual download
    // Examples:
    // - Payroll software APIs (ADP, Paycom - may require subscription)
    // - Tax software APIs (TurboTax, H&R Block - may require subscription)
    // - Government data portals (some states provide APIs)
    
    console.log('⚠️  Third-party API integration not implemented');
    console.log('   Most state withholding tables are in PDF format');
    console.log('   Manual conversion or PDF parsing library required');
}

/**
 * Main function
 */
async function fetchOfficialStateWithholdingTables(options = {}) {
    const {
        states = null, // Array of state codes, or null for all
        year = 2026
    } = options;

    const fetcher = new StateWithholdingFetcher();
    fetcher.year = year;

    if (states && Array.isArray(states)) {
        // Fetch specific states
        const results = [];
        for (const stateCode of states) {
            const result = await fetcher.fetchState(stateCode);
            if (result) results.push(result);
        }
        return results;
    } else {
        // Fetch all available states
        return await fetcher.fetchAllStates();
    }
}

// Run if called directly
if (require.main === module) {
    const args = process.argv.slice(2);
    
    const options = {
        year: 2026
    };

    const yearIndex = args.indexOf('--year');
    if (yearIndex !== -1 && args[yearIndex + 1]) {
        options.year = parseInt(args[yearIndex + 1]);
    }

    const stateIndex = args.indexOf('--state');
    if (stateIndex !== -1 && args[stateIndex + 1]) {
        options.states = args[stateIndex + 1].split(',').map(s => s.trim().toUpperCase());
    }

    fetchOfficialStateWithholdingTables(options)
        .then((results) => {
            console.log('\n✅ Fetch completed');
            if (results.length === 0) {
                console.log('\n💡 Note: Most state withholding tables are in PDF format');
                console.log('   You may need to:');
                console.log('   1. Manually download from state revenue department websites');
                console.log('   2. Convert PDF to JSON using a PDF parser');
                console.log('   3. Or use the template to manually create JSON files');
            }
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Fetch failed:', err);
            process.exit(1);
        });
}

module.exports = { fetchOfficialStateWithholdingTables, StateWithholdingFetcher };

