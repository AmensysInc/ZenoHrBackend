const { getDatabase } = require('../database/init');
const { importOfficialStateWithholdingTables } = require('./importOfficialStateWithholdingTables');
const fs = require('fs');
const path = require('path');

/**
 * Generate official-style withholding tables from state brackets and import into database
 * This creates comprehensive tables for all states based on their tax brackets
 */
class StateTableGenerator {
    constructor() {
        this.db = getDatabase();
        this.year = 2026;
        this.dataDir = path.join(__dirname, '../data/state_withholding');
        
        if (!fs.existsSync(this.dataDir)) {
            fs.mkdirSync(this.dataDir, { recursive: true });
        }
    }

    /**
     * Get state brackets and deduction from database
     */
    async getStateData(stateCode) {
        return new Promise((resolve, reject) => {
            // Get brackets
            this.db.all(`SELECT bracket_min, bracket_max, rate FROM state_brackets 
                    WHERE year = ? AND state_code = ? AND filing_status = 'SINGLE'
                    ORDER BY bracket_min`,
                [this.year, stateCode],
                (err, brackets) => {
                    if (err) {
                        reject(err);
                        return;
                    }

                    // Get deduction
                    this.db.get(`SELECT standard_deduction FROM state_deductions 
                            WHERE year = ? AND state_code = ? AND filing_status = 'SINGLE'`,
                        [this.year, stateCode],
                        (err2, dedRow) => {
                            if (err2) {
                                reject(err2);
                                return;
                            }

                            const deduction = dedRow ? dedRow.standard_deduction : 0;

                            // Check if flat rate
                            if (brackets.length === 1 && brackets[0].bracket_min === 0 && brackets[0].bracket_max === null) {
                                resolve({
                                    type: 'flat',
                                    rate: brackets[0].rate,
                                    deduction: deduction,
                                    brackets: brackets
                                });
                            } else if (brackets.length > 0) {
                                resolve({
                                    type: 'brackets',
                                    brackets: brackets,
                                    deduction: deduction
                                });
                            } else {
                                resolve(null);
                            }
                        }
                    );
                }
            );
        });
    }

    /**
     * Generate withholding table entries for a state
     */
    generateWithholdingEntries(stateData, payFrequency, filingStatus) {
        const entries = [];
        const payPeriodsPerYear = this.getPayPeriodsPerYear(payFrequency);
        
        // Determine step size based on pay frequency
        const stepSize = this.getStepSize(payFrequency);
        const maxWage = 20000; // Reasonable max per pay period
        
        if (stateData.type === 'flat') {
            // Flat rate state
            for (let wageMin = 0; wageMin < maxWage; wageMin += stepSize) {
                const wageMax = Math.min(wageMin + stepSize, maxWage);
                const annualWage = ((wageMin + wageMax) / 2) * payPeriodsPerYear;
                const taxableIncome = Math.max(0, annualWage - stateData.deduction);
                const annualTax = taxableIncome * stateData.rate;
                const perPeriodTax = annualTax / payPeriodsPerYear;
                
                entries.push({
                    wageMin: wageMin,
                    wageMax: wageMax,
                    withholdingAmount: Math.round(perPeriodTax * 100) / 100,
                    percentage: null,
                    baseAmount: null
                });
            }
        } else if (stateData.type === 'brackets') {
            // Bracket-based state
            for (let wageMin = 0; wageMin < maxWage; wageMin += stepSize) {
                const wageMax = Math.min(wageMin + stepSize, maxWage);
                const midWage = (wageMin + wageMax) / 2;
                const annualWage = midWage * payPeriodsPerYear;
                const taxableIncome = Math.max(0, annualWage - stateData.deduction);
                
                // Calculate tax using brackets
                let annualTax = 0;
                for (const bracket of stateData.brackets) {
                    if (taxableIncome > bracket.bracket_min) {
                        const maxInBracket = bracket.bracket_max === null 
                            ? taxableIncome 
                            : Math.min(taxableIncome, bracket.bracket_max);
                        const taxableAtThisRate = maxInBracket - bracket.bracket_min;
                        if (taxableAtThisRate > 0) {
                            annualTax += taxableAtThisRate * bracket.rate;
                        }
                    }
                }
                
                const perPeriodTax = annualTax / payPeriodsPerYear;
                
                entries.push({
                    wageMin: wageMin,
                    wageMax: wageMax,
                    withholdingAmount: Math.round(perPeriodTax * 100) / 100,
                    percentage: null,
                    baseAmount: null
                });
            }
        }
        
        return entries;
    }

    /**
     * Generate complete withholding tables for a state
     */
    async generateStateTables(stateCode) {
        const stateData = await this.getStateData(stateCode);
        
        if (!stateData) {
            return null;
        }

        const tables = {
            state: stateCode,
            year: this.year,
            source: `Generated from official ${stateCode} tax brackets and deductions for ${this.year}`,
            lastUpdated: new Date().toISOString().split('T')[0],
            notes: `State withholding tables for ${stateCode} - ${this.year}. Generated from official tax brackets.`,
            tables: []
        };

        const payFrequencies = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
        const filingStatuses = ['SINGLE', 'MARRIED', 'MARRIED_SEPARATE', 'HEAD_OF_HOUSEHOLD'];

        for (const payFreq of payFrequencies) {
            for (const filingStatus of filingStatuses) {
                const entries = this.generateWithholdingEntries(stateData, payFreq, filingStatus);
                
                if (entries.length > 0) {
                    tables.tables.push({
                        payFrequency: payFreq,
                        filingStatus: filingStatus,
                        entries: entries
                    });
                }
            }
        }

        return tables;
    }

    /**
     * Process all states
     */
    async processAllStates() {
        console.log('🚀 Generating and Importing All State Withholding Tables\n');
        console.log('='.repeat(80));
        console.log(`Year: ${this.year}\n`);

        // States with income tax
        const statesWithTax = [
            'AL', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'GA', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY',
            'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NJ', 'NM', 'NY', 'NC', 'ND',
            'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 'UT', 'VT', 'VA', 'WV', 'WI', 'DC'
        ];

        const results = [];
        let totalEntries = 0;

        for (const stateCode of statesWithTax) {
            try {
                console.log(`\n📊 Processing ${stateCode}...`);
                
                const tables = await this.generateStateTables(stateCode);
                
                if (!tables || tables.tables.length === 0) {
                    console.log(`   ⚠️  No data generated for ${stateCode}`);
                    results.push({ state: stateCode, success: false, reason: 'No data' });
                    continue;
                }

                // Save JSON
                const jsonPath = path.join(this.dataDir, `${stateCode}_${this.year}.json`);
                fs.writeFileSync(jsonPath, JSON.stringify(tables, null, 2));
                
                const entryCount = tables.tables.reduce((sum, t) => sum + t.entries.length, 0);
                console.log(`   ✅ Generated ${tables.tables.length} tables with ${entryCount.toLocaleString()} entries`);
                console.log(`   💾 Saved: ${jsonPath}`);

                // Import into database
                console.log(`   📥 Importing into database...`);
                const importResult = await importOfficialStateWithholdingTables(jsonPath, {
                    clearExisting: true,
                    year: this.year
                });

                if (importResult.success) {
                    console.log(`   ✅ Imported ${importResult.totalInserted.toLocaleString()} entries`);
                    totalEntries += importResult.totalInserted;
                    results.push({ 
                        state: stateCode, 
                        success: true, 
                        entries: importResult.totalInserted,
                        tables: tables.tables.length
                    });
                } else {
                    console.log(`   ⚠️  Import had ${importResult.totalErrors} errors`);
                    results.push({ 
                        state: stateCode, 
                        success: false, 
                        errors: importResult.totalErrors 
                    });
                }

            } catch (error) {
                console.error(`   ❌ Error processing ${stateCode}:`, error.message);
                results.push({ state: stateCode, success: false, error: error.message });
            }
        }

        // Summary
        console.log(`\n${'='.repeat(80)}`);
        console.log('📊 FINAL SUMMARY\n');
        
        const successful = results.filter(r => r.success).length;
        const failed = results.filter(r => !r.success).length;
        
        console.log(`   States processed: ${results.length}`);
        console.log(`   ✅ Successful: ${successful}`);
        console.log(`   ❌ Failed: ${failed}`);
        console.log(`   📊 Total entries imported: ${totalEntries.toLocaleString()}`);
        
        if (successful > 0) {
            console.log(`\n✅ All state withholding tables generated and imported!`);
            console.log(`   Ready for frontend testing.`);
            console.log(`\n📝 Next Steps:`);
            console.log(`   1. Test calculations on frontend`);
            console.log(`   2. Compare with Paycom results`);
            console.log(`   3. Verify: node scripts/checkStateWithholdingAlignment.js`);
        }

        return results;
    }

    getPayPeriodsPerYear(payFrequency) {
        const periods = {
            'WEEKLY': 52,
            'BIWEEKLY': 26,
            'SEMIMONTHLY': 24,
            'MONTHLY': 12
        };
        return periods[payFrequency.toUpperCase()] || 12;
    }

    getStepSize(payFrequency) {
        const steps = {
            'WEEKLY': 25,        // $25 increments
            'BIWEEKLY': 50,     // $50 increments
            'SEMIMONTHLY': 100, // $100 increments
            'MONTHLY': 100      // $100 increments
        };
        return steps[payFrequency.toUpperCase()] || 100;
    }
}

// Run if called directly
if (require.main === module) {
    const generator = new StateTableGenerator();
    generator.processAllStates()
        .then(() => {
            console.log('\n✅ Processing complete');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Processing failed:', err);
            process.exit(1);
        });
}

module.exports = { StateTableGenerator };

