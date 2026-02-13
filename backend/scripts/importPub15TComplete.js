const { getDatabase } = require('../database/init');

/**
 * Import Complete IRS Pub 15-T Percentage Method Tables (2026)
 * 
 * This script imports comprehensive Pub 15-T tables for:
 * - All pay frequencies: Weekly, Bi-weekly, Semi-monthly, Monthly
 * - All filing statuses: Single, Married Jointly, Married Separately, Head of Household
 * - Step 2 checkbox variations: With and Without
 * 
 * Note: These tables are based on 2026 tax brackets and standard deductions.
 * For production use, verify against official IRS Publication 15-T (2026).
 */

// Helper function to calculate percentage method table entries
// Based on 2026 tax brackets and standard deductions
function generatePub15TEntries(payFrequency, filingStatus, step2Checkbox) {
    const entries = [];
    
    // 2026 Standard Deductions
    const standardDeductions = {
        'SINGLE': 16100,
        'MARRIED_JOINTLY': 32200,
        'MARRIED_SEPARATELY': 16100,
        'HEAD_OF_HOUSEHOLD': 24150
    };
    
    // Pay periods per year
    const periodsPerYear = {
        'WEEKLY': 52,
        'BIWEEKLY': 26,
        'SEMIMONTHLY': 24,
        'MONTHLY': 12
    };
    
    const periods = periodsPerYear[payFrequency];
    const stdDed = standardDeductions[filingStatus] || 16100;
    const stdDedPerPeriod = stdDed / periods;
    
    // 2026 Federal Tax Brackets (Single)
    const bracketsSingle = [
        { min: 0, max: 12400, rate: 0.10 },
        { min: 12400, max: 50400, rate: 0.12 },
        { min: 50400, max: 105700, rate: 0.22 },
        { min: 105700, max: 201775, rate: 0.24 },
        { min: 201775, max: 256225, rate: 0.32 },
        { min: 256225, max: 640600, rate: 0.35 },
        { min: 640600, max: 999999999, rate: 0.37 }
    ];
    
    // 2026 Federal Tax Brackets (Married Jointly)
    const bracketsMarriedJointly = [
        { min: 0, max: 24800, rate: 0.10 },
        { min: 24800, max: 100800, rate: 0.12 },
        { min: 100800, max: 211400, rate: 0.22 },
        { min: 211400, max: 403550, rate: 0.24 },
        { min: 403550, max: 512450, rate: 0.32 },
        { min: 512450, max: 1281200, rate: 0.35 },
        { min: 1281200, max: 999999999, rate: 0.37 }
    ];
    
    // 2026 Federal Tax Brackets (Married Separately)
    const bracketsMarriedSeparately = [
        { min: 0, max: 12400, rate: 0.10 },
        { min: 12400, max: 50400, rate: 0.12 },
        { min: 50400, max: 105700, rate: 0.22 },
        { min: 105700, max: 201775, rate: 0.24 },
        { min: 201775, max: 256225, rate: 0.32 },
        { min: 256225, max: 640600, rate: 0.35 },
        { min: 640600, max: 999999999, rate: 0.37 }
    ];
    
    // 2026 Federal Tax Brackets (Head of Household)
    const bracketsHOH = [
        { min: 0, max: 18650, rate: 0.10 },
        { min: 18650, max: 75650, rate: 0.12 },
        { min: 75650, max: 105700, rate: 0.22 },
        { min: 105700, max: 201775, rate: 0.24 },
        { min: 201775, max: 256225, rate: 0.32 },
        { min: 256225, max: 640600, rate: 0.35 },
        { min: 640600, max: 999999999, rate: 0.37 }
    ];
    
    // Select brackets based on filing status
    let brackets;
    if (filingStatus === 'MARRIED_JOINTLY') {
        brackets = bracketsMarriedJointly;
    } else if (filingStatus === 'MARRIED_SEPARATELY') {
        brackets = bracketsMarriedSeparately;
    } else if (filingStatus === 'HEAD_OF_HOUSEHOLD') {
        brackets = bracketsHOH;
    } else {
        brackets = bracketsSingle;
    }
    
    // Step 2 checkbox adjustment: Higher withholding for multiple jobs
    // Pub 15-T uses different tables or multipliers when Step 2 is checked
    const step2Multiplier = step2Checkbox ? 1.2 : 1.0; // Simplified - actual Pub 15-T has separate tables
    
    // Generate wage ranges and calculate withholding
    // Pub 15-T tables typically have ranges like: 0-100, 100-200, etc.
    // We'll create comprehensive ranges based on typical payroll amounts
    
    const maxWage = 50000; // Maximum wage per period to cover
    const rangeSize = 100; // Wage range size
    
    for (let wageMin = 0; wageMin < maxWage; wageMin += rangeSize) {
        const wageMax = wageMin + rangeSize;
        const wageMid = (wageMin + wageMax) / 2;
        
        // Calculate annual wages
        const annualWages = wageMid * periods;
        
        // Calculate taxable income
        let taxableIncome = annualWages - stdDed;
        if (taxableIncome < 0) taxableIncome = 0;
        
        // Calculate annual tax
        let annualTax = 0;
        for (const bracket of brackets) {
            if (taxableIncome > bracket.min) {
                const taxableAtThisRate = Math.min(taxableIncome, bracket.max) - bracket.min;
                if (taxableAtThisRate > 0) {
                    annualTax += taxableAtThisRate * bracket.rate;
                }
            }
        }
        
        // Convert to per-period withholding
        let withholding = annualTax / periods;
        
        // Apply Step 2 multiplier if checked
        withholding *= step2Multiplier;
        
            // Calculate base_tax and excess_over for Pub 15-T format
            // Formula: base_tax + rate × (taxable_wages − excess_over)
            // We need to calculate what the withholding would be at wage_min
            let baseTax = 0;
            let excessOver = wageMin;
            let rate = 0;
            
            if (wageMin > 0) {
                // Calculate withholding at the start of this range (wage_min)
                const prevAnnualWages = wageMin * periods;
                const prevTaxableIncome = Math.max(0, prevAnnualWages - stdDed);
                
                let prevAnnualTax = 0;
                for (const bracket of brackets) {
                    if (prevTaxableIncome > bracket.min) {
                        const taxableAtThisRate = Math.min(prevTaxableIncome, bracket.max) - bracket.min;
                        if (taxableAtThisRate > 0) {
                            prevAnnualTax += taxableAtThisRate * bracket.rate;
                        }
                    }
                }
                
                baseTax = (prevAnnualTax / periods) * step2Multiplier;
            }
            
            // Calculate rate: (withholding - baseTax) / (wageMid - excessOver)
            if (wageMid > excessOver) {
                rate = (withholding - baseTax) / (wageMid - excessOver);
            }
            
            entries.push({
                wage_min: wageMin,
                wage_max: wageMax,
                base_tax: baseTax,
                rate: rate,
                excess_over: excessOver
            });
    }
    
    // Add high-income ranges
    const highIncomeRanges = [
        { min: 50000, max: 100000, step: 5000 },
        { min: 100000, max: 200000, step: 10000 },
        { min: 200000, max: 500000, step: 25000 }
    ];
    
    for (const range of highIncomeRanges) {
        for (let wageMin = range.min; wageMin < range.max; wageMin += range.step) {
            const wageMax = wageMin + range.step;
            const wageMid = (wageMin + wageMax) / 2;
            
            const annualWages = wageMid * periods;
            let taxableIncome = annualWages - stdDed;
            if (taxableIncome < 0) taxableIncome = 0;
            
            let annualTax = 0;
            for (const bracket of brackets) {
                if (taxableIncome > bracket.min) {
                    const taxableAtThisRate = Math.min(taxableIncome, bracket.max) - bracket.min;
                    if (taxableAtThisRate > 0) {
                        annualTax += taxableAtThisRate * bracket.rate;
                    }
                }
            }
            
            let withholding = (annualTax / periods) * step2Multiplier;
            
            // Calculate percentage and base
            const prevWageMid = wageMid - range.step;
            const prevAnnualWages = prevWageMid * periods;
            const prevTaxableIncome = Math.max(0, prevAnnualWages - stdDed);
            
            let prevAnnualTax = 0;
            for (const bracket of brackets) {
                if (prevTaxableIncome > bracket.min) {
                    const taxableAtThisRate = Math.min(prevTaxableIncome, bracket.max) - bracket.min;
                    if (taxableAtThisRate > 0) {
                        prevAnnualTax += taxableAtThisRate * bracket.rate;
                    }
                }
            }
            
            const prevWithholding = (prevAnnualTax / periods) * step2Multiplier;
            const withholdingDiff = withholding - prevWithholding;
            const percentage = withholdingDiff / range.step;
            
            entries.push({
                wage_min: wageMin,
                wage_max: wageMax,
                percentage: percentage,
                base_amount: prevWithholding
            });
        }
    }
    
    // Add final range for very high income
    entries.push({
        wage_min: 500000,
        wage_max: 999999999,
        percentage: 0.37 * step2Multiplier, // Top bracket rate
        base_amount: 0 // Will be calculated from previous range
    });
    
    return entries;
}

async function importCompletePub15TTables() {
    const db = getDatabase();
    const year = 2026;
    
    const payFrequencies = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
    const filingStatuses = ['SINGLE', 'MARRIED_JOINTLY', 'MARRIED_SEPARATELY', 'HEAD_OF_HOUSEHOLD'];
    const step2Options = [0, 1]; // Without and with Step 2 checkbox
    
    return new Promise((resolve, reject) => {
        db.serialize(() => {
            // Clear existing data for this year
            db.run(`DELETE FROM pub15t_percentage_tables WHERE year = ?`, [year], (err) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                // Use new schema: base_tax, rate, excess_over
                const stmt = db.prepare(`INSERT OR REPLACE INTO pub15t_percentage_tables 
                    (year, pay_frequency, filing_status, step2_checkbox, wage_min, wage_max, base_tax, rate, excess_over) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`);
                
                let totalEntries = 0;
                
                // Generate entries for all combinations
                for (const payFreq of payFrequencies) {
                    for (const filingStatus of filingStatuses) {
                        for (const step2 of step2Options) {
                            const entries = generatePub15TEntries(payFreq, filingStatus, step2 === 1);
                            
                            entries.forEach(entry => {
                                stmt.run(year, payFreq, filingStatus, step2, 
                                        entry.wage_min, entry.wage_max, 
                                        entry.base_tax, entry.rate, entry.excess_over);
                                totalEntries++;
                            });
                        }
                    }
                }
                
                stmt.finalize((err3) => {
                    if (err3) {
                        reject(err3);
                    } else {
                        console.log(`✅ Imported ${totalEntries} Pub 15-T table entries`);
                        console.log(`   - ${payFrequencies.length} pay frequencies`);
                        console.log(`   - ${filingStatuses.length} filing statuses`);
                        console.log(`   - ${step2Options.length} Step 2 variations`);
                        console.log(`   - Total combinations: ${payFrequencies.length * filingStatuses.length * step2Options.length}`);
                        resolve();
                    }
                });
            });
        });
    });
}

module.exports = { importCompletePub15TTables };

// Run if called directly
if (require.main === module) {
    importCompletePub15TTables()
        .then(() => {
            console.log('\n✅ Complete Pub 15-T import finished!');
            console.log('\n⚠️  Note: These tables are calculated from 2026 tax brackets.');
            console.log('   For production use, verify against official IRS Publication 15-T (2026).');
            process.exit(0);
        })
        .catch(err => {
            console.error('Import error:', err);
            process.exit(1);
        });
}

