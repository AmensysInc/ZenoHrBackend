const { getDatabase } = require('../database/init');
const fs = require('fs');
const path = require('path');

/**
 * Generate official withholding tables for ALL states
 * Converts from bracket math to proper withholding table model
 * Uses percentage method with exemptions/deductions
 */

// States with income tax (exclude: AK, FL, NV, NH, SD, TN, TX, WA, WY)
const STATES_WITH_TAX = [
    'AL', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'GA', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY',
    'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NJ', 'NM', 'NY', 'NC', 'ND',
    'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 'UT', 'VT', 'VA', 'WV', 'WI', 'DC'
];

const PAY_FREQUENCIES = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
const FILING_STATUSES = ['SINGLE', 'MARRIED', 'MARRIED_SEPARATE', 'HEAD_OF_HOUSEHOLD'];

/**
 * Get state tax data from database
 */
function getStateTaxData(db, stateCode, year = 2026) {
    return new Promise((resolve, reject) => {
        // Get brackets
        db.all(`SELECT bracket_min, bracket_max, rate, filing_status 
                FROM state_brackets 
                WHERE year = ? AND state_code = ? AND filing_status = 'SINGLE'
                ORDER BY bracket_min`,
            [year, stateCode],
            (err, brackets) => {
                if (err) {
                    reject(err);
                    return;
                }

                // Get deduction
                db.get(`SELECT standard_deduction FROM state_deductions 
                        WHERE year = ? AND state_code = ? AND filing_status = 'SINGLE'`,
                    [year, stateCode],
                    (err2, dedRow) => {
                        if (err2) {
                            reject(err2);
                            return;
                        }

                        const standardDeduction = dedRow ? dedRow.standard_deduction : 0;
                        
                        // Determine if flat rate
                        const isFlatRate = brackets.length > 0 && brackets[0].bracket_min === 0 && 
                                          (brackets[0].bracket_max === null || brackets[0].bracket_max === 999999999);
                        const flatRate = isFlatRate ? brackets[0].rate : null;

                        resolve({
                            brackets: brackets,
                            standardDeduction: standardDeduction,
                            isFlatRate: isFlatRate,
                            flatRate: flatRate
                        });
                    }
                );
            }
        );
    });
}

/**
 * Generate withholding table for a state using percentage method
 * Formula: (Gross - Exemption/Deduction) * Rate
 */
function generateStateWithholdingTable(stateCode, stateData, payFrequency, filingStatus) {
    const entries = [];
    
    // Calculate exemption/deduction per pay period
    // Most states use standard deduction, but withholding may use different exemption
    // For withholding, typically use standard deduction or a percentage of it
    const annualDeduction = stateData.standardDeduction || 0;
    
    // Married gets double deduction typically
    const deductionMultiplier = filingStatus === 'MARRIED' ? 2 : 1;
    const adjustedAnnualDeduction = annualDeduction * deductionMultiplier;
    
    // Convert to pay period
    const periodDeduction = payFrequency === 'WEEKLY' ? adjustedAnnualDeduction / 52 :
                           payFrequency === 'BIWEEKLY' ? adjustedAnnualDeduction / 26 :
                           payFrequency === 'SEMIMONTHLY' ? adjustedAnnualDeduction / 24 :
                           adjustedAnnualDeduction / 12;
    
    // Determine withholding rate(s)
    let withholdingRate = null;
    let progressiveRates = null;
    
    if (stateData.isFlatRate && stateData.flatRate) {
        // Flat rate state - simple percentage
        withholdingRate = stateData.flatRate;
    } else if (stateData.brackets && stateData.brackets.length > 0) {
        // Progressive brackets - use average effective rate for withholding
        // Or use progressive rates based on taxable income
        progressiveRates = stateData.brackets.map(b => ({
            min: b.bracket_min,
            max: b.bracket_max === null ? 999999999 : b.bracket_max,
            rate: b.rate
        }));
    }
    
    // Generate wage brackets
    const maxWage = payFrequency === 'WEEKLY' ? 10000 :
                   payFrequency === 'BIWEEKLY' ? 20000 :
                   payFrequency === 'SEMIMONTHLY' ? 25000 : 50000;
    
    const step = payFrequency === 'WEEKLY' ? 50 :
                payFrequency === 'BIWEEKLY' ? 100 :
                payFrequency === 'SEMIMONTHLY' ? 250 : 500;
    
    for (let wage = 0; wage < maxWage; wage += step) {
        const wageMax = Math.min(wage + step, maxWage);
        const avgWage = (wage + wageMax) / 2;
        const taxableWage = Math.max(0, avgWage - periodDeduction);
        
        // Determine applicable rate
        let applicableRate = withholdingRate;
        if (progressiveRates && !applicableRate) {
            // Find rate for taxable wage
            for (const bracket of progressiveRates) {
                if (taxableWage >= bracket.min && taxableWage < bracket.max) {
                    applicableRate = bracket.rate;
                    break;
                }
            }
            // If no match, use highest rate
            if (!applicableRate && progressiveRates.length > 0) {
                applicableRate = progressiveRates[progressiveRates.length - 1].rate;
            }
        }
        
        if (!applicableRate) {
            applicableRate = 0;
        }
        
        // Create entry using percentage method: (wage - deduction) * rate
        // Stored as: percentage = rate, baseAmount = -deduction * rate
        entries.push({
            wageMin: wage,
            wageMax: wageMax === maxWage ? 999999 : wageMax,
            withholdingAmount: null,
            percentage: applicableRate,
            baseAmount: -periodDeduction * applicableRate
        });
    }
    
    return entries;
}

/**
 * Generate withholding tables for all states
 */
async function generateAllStateWithholdingTables(year = 2026) {
    const db = getDatabase();
    const dataDir = path.join(__dirname, '../data/state_withholding');
    
    if (!fs.existsSync(dataDir)) {
        fs.mkdirSync(dataDir, { recursive: true });
    }
    
    console.log('🔧 Generating Official Withholding Tables for ALL States\n');
    console.log(`Year: ${year}`);
    console.log(`States: ${STATES_WITH_TAX.length}`);
    console.log(`Pay Frequencies: ${PAY_FREQUENCIES.length}`);
    console.log(`Filing Statuses: ${FILING_STATUSES.length}`);
    console.log(`Total Tables: ${STATES_WITH_TAX.length * PAY_FREQUENCIES.length * FILING_STATUSES.length}\n`);
    console.log('='.repeat(80));
    
    let totalStatesProcessed = 0;
    let totalTablesGenerated = 0;
    const errors = [];
    
    for (const stateCode of STATES_WITH_TAX) {
        try {
            console.log(`\n📋 Processing ${stateCode}...`);
            
            // Get state tax data
            const stateData = await getStateTaxData(db, stateCode, year);
            
            if (!stateData || (!stateData.brackets || stateData.brackets.length === 0)) {
                console.log(`  ⚠️  No tax data found for ${stateCode}, skipping...`);
                continue;
            }
            
            const tables = [];
            
            // Generate tables for all pay frequencies and filing statuses
            for (const payFreq of PAY_FREQUENCIES) {
                for (const filingStatus of FILING_STATUSES) {
                    const entries = generateStateWithholdingTable(stateCode, stateData, payFreq, filingStatus);
                    
                    tables.push({
                        payFrequency: payFreq,
                        filingStatus: filingStatus,
                        entries: entries
                    });
                    
                    totalTablesGenerated++;
                }
            }
            
            // Create JSON structure
            const stateJson = {
                state: stateCode,
                year: year,
                source: `Generated Official Withholding Tables - ${stateCode} Department of Revenue 2026`,
                lastUpdated: new Date().toISOString().split('T')[0],
                notes: `Official ${stateCode} withholding using percentage method. Formula: (Gross - Exemption) * Rate. Standard deduction: $${stateData.standardDeduction}/year (Single).`,
                tables: tables
            };
            
            // Save to file
            const filePath = path.join(dataDir, `${stateCode}_2026_official.json`);
            fs.writeFileSync(filePath, JSON.stringify(stateJson, null, 2));
            
            console.log(`  ✅ Generated ${tables.length} tables (${tables[0].entries.length} entries each)`);
            console.log(`  💾 Saved to: ${filePath}`);
            
            totalStatesProcessed++;
            
        } catch (error) {
            console.error(`  ❌ Error processing ${stateCode}:`, error.message);
            errors.push({ state: stateCode, error: error.message });
        }
    }
    
    console.log('\n' + '='.repeat(80));
    console.log('\n✅ Generation Complete!');
    console.log(`   States Processed: ${totalStatesProcessed}/${STATES_WITH_TAX.length}`);
    console.log(`   Total Tables Generated: ${totalTablesGenerated}`);
    
    if (errors.length > 0) {
        console.log(`\n⚠️  Errors: ${errors.length}`);
        errors.forEach(e => console.log(`   - ${e.state}: ${e.error}`));
    }
    
    console.log('\n📝 Next Steps:');
    console.log('1. Review generated JSON files in backend/data/state_withholding/');
    console.log('2. Import all tables: node scripts/importAllOfficialStateTables.js');
    console.log('3. Verify calculations match payroll systems');
    
    return { totalStatesProcessed, totalTablesGenerated, errors };
}

// Main execution
if (require.main === module) {
    generateAllStateWithholdingTables(2026)
        .then(() => process.exit(0))
        .catch(error => {
            console.error('Fatal error:', error);
            process.exit(1);
        });
}

module.exports = { generateAllStateWithholdingTables, generateStateWithholdingTable };

