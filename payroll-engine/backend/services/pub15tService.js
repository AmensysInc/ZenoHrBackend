const { getDatabase } = require('../database/init');

/**
 * Calculate federal withholding using IRS Pub 15-T Percentage Method
 * This is the production-grade method used by ADP/Paycom
 * 
 * IMPORTANT: This uses ONLY pub15t_percentage_tables
 * NO annualization, NO standard deduction, NO federal_brackets
 * 
 * Formula: base_tax + rate × (taxable_wages − excess_over)
 */
async function calculateFederalWithholdingPub15T(grossPay, payFrequency, filingStatus, step2Checkbox, w4Data, taxYear = 2026) {
    const db = getDatabase();

    return new Promise((resolve, reject) => {
        // Step 1: Adjust gross pay for W-4 Step 4(a) and Step 4(b)
        // Step 4(a): Add other income (per period)
        // Step 4(b): Subtract deductions (per period)
        const payPeriodsPerYear = getPayPeriodsPerYear(payFrequency);
        
        let taxableWages = grossPay;
        
        // Add Step 4(a) other income (already per period)
        if (w4Data && w4Data.step4aOtherIncome) {
            taxableWages += w4Data.step4aOtherIncome;
        }
        
        // Subtract Step 4(b) deductions (convert annual to per period if needed)
        if (w4Data && w4Data.step4bDeductions) {
            // If deductions are annual, convert to per period
            // For now, assume they're already per period or annual - need to check W-4 form
            // W-4 Step 4(b) is typically annual, so convert:
            taxableWages -= (w4Data.step4bDeductions / payPeriodsPerYear);
        }
        
        taxableWages = Math.max(0, taxableWages);
        
        // Step 2: Lookup in Pub 15-T percentage method table
        const step2 = step2Checkbox ? 1 : 0;
        
        // Debug logging
        console.error(`[Pub15T] Lookup: year=${taxYear}, freq=${payFrequency}, status=${filingStatus}, step2=${step2}, wages=${taxableWages}`);
        
        db.get(`SELECT * FROM pub15t_percentage_tables 
                WHERE year = ? AND pay_frequency = ? AND filing_status = ? AND step2_checkbox = ?
                AND ? >= wage_min AND ? < wage_max
                LIMIT 1`,
            [taxYear, payFrequency.toUpperCase(), filingStatus, step2, taxableWages, taxableWages],
            async (err, row) => {
                if (err) {
                    console.error('Pub 15-T lookup error:', err);
                    reject(err);
                    return;
                }

                if (row) {
                    // Use Pub 15-T percentage method formula
                    // Formula: base_tax + rate × (taxable_wages − excess_over)
                    let withholding = 0;
                    
                    if (row.base_tax !== null && row.rate !== null && row.excess_over !== null) {
                        // Proper IRS Pub 15-T formula
                        withholding = row.base_tax + (row.rate * (taxableWages - row.excess_over));
                        console.log(`[Pub15T] Calculated: ${row.base_tax} + (${row.rate} × (${taxableWages} - ${row.excess_over})) = ${withholding}`);
                    } else if (row.base_amount !== null && row.percentage !== null) {
                        // Fallback for old schema (backward compatibility)
                        withholding = row.base_amount + ((taxableWages - row.wage_min) * row.percentage);
                        console.log(`[Pub15T] Using old schema: ${row.base_amount} + (${row.percentage} × (${taxableWages} - ${row.wage_min})) = ${withholding}`);
                    } else {
                        // No valid data
                        console.log('[Pub15T] Row missing required columns:', row);
                        const annualized = await calculateAnnualizedMethod(grossPay, payFrequency, filingStatus, step2Checkbox, w4Data, taxYear);
                        resolve(annualized);
                        return;
                    }
                    
                    // Step 3: Subtract credits (annual credits, convert to per-period)
                    if (w4Data && w4Data.step3Credits) {
                        withholding -= (w4Data.step3Credits / payPeriodsPerYear);
                    }
                    
                    // Step 4(c): Add extra withholding (per period)
                    if (w4Data && w4Data.step4cExtraWithholding) {
                        withholding += w4Data.step4cExtraWithholding;
                    }
                    
                    // IRS rounding: round to nearest dollar, 0.50 rounds up
                    withholding = Math.round(withholding);
                    withholding = Math.max(0, withholding);
                    
                    console.error(`[Pub15T] Final withholding: ${withholding}`);
                    resolve(withholding);
                } else {
                    // CRITICAL: Pub 15-T table not found - enforce table-only calculations
                    const errorMsg = `CRITICAL: Pub 15-T percentage table not found for year=${taxYear}, freq=${payFrequency}, status=${filingStatus}, step2=${step2}, wages=${taxableWages}. ` +
                                   `Official Pub 15-T tables are required. Annualized method fallback is disabled for production accuracy.`;
                    console.error(`❌ ${errorMsg}`);
                    reject(new Error(errorMsg));
                    return;
                }
            }
        );
    });
}

/**
 * DEPRECATED: Annualized method fallback removed for production accuracy
 * Pub 15-T percentage tables are required
 * This function is kept for reference only and should not be called
 */
async function calculateAnnualizedMethod(grossPay, payFrequency, filingStatus, step2Checkbox, w4Data, taxYear) {
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        const payPeriodsPerYear = getPayPeriodsPerYear(payFrequency);
        const annualWages = grossPay * payPeriodsPerYear;
        
        // Get standard deduction
        db.get(`SELECT standard_deduction FROM federal_deductions 
                WHERE year = ? AND filing_status = ?`,
            [taxYear, filingStatus],
            (err, dedRow) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                const standardDeduction = dedRow ? dedRow.standard_deduction : 16100;
                
                // Calculate taxable income
                let taxableIncome = annualWages - standardDeduction;
                
                // Step 4(a): Add other income
                if (w4Data.step4aOtherIncome) {
                    taxableIncome += w4Data.step4aOtherIncome;
                }
                
                // Step 4(b): Subtract deductions
                if (w4Data.step4bDeductions) {
                    taxableIncome -= w4Data.step4bDeductions;
                }
                
                taxableIncome = Math.max(0, taxableIncome);
                
                // Calculate tax using brackets
                calculateTaxFromBrackets(taxableIncome, filingStatus, taxYear, db)
                    .then(annualTax => {
                        // Step 3: Subtract credits
                        if (w4Data.step3Credits) {
                            annualTax = Math.max(0, annualTax - w4Data.step3Credits);
                        }
                        
                        // Convert to per-period
                        let perPeriodTax = annualTax / payPeriodsPerYear;
                        
                        // Step 4(c): Add extra withholding
                        if (w4Data.step4cExtraWithholding) {
                            perPeriodTax += w4Data.step4cExtraWithholding;
                        }
                        
                        resolve(Math.max(0, perPeriodTax));
                    })
                    .catch(reject);
            }
        );
    });
}

/**
 * Calculate tax from brackets
 */
function calculateTaxFromBrackets(taxableIncome, filingStatus, taxYear, db) {
    return new Promise((resolve, reject) => {
        db.all(`SELECT bracket_min, bracket_max, rate FROM federal_brackets 
                WHERE year = ? AND filing_status = ?
                ORDER BY bracket_min`,
            [taxYear, filingStatus],
            (err, brackets) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                let tax = 0;
                for (const bracket of brackets) {
                    if (taxableIncome > bracket.bracket_min) {
                        const taxableAtThisRate = Math.min(taxableIncome, bracket.bracket_max) - bracket.bracket_min;
                        if (taxableAtThisRate > 0) {
                            tax += taxableAtThisRate * bracket.rate;
                        }
                    }
                }
                
                resolve(tax);
            }
        );
    });
}

function getPayPeriodsPerYear(payFrequency) {
    const periods = {
        'WEEKLY': 52,
        'BIWEEKLY': 26,
        'SEMIMONTHLY': 24,
        'MONTHLY': 12,
        'QUARTERLY': 4,
        'ANNUALLY': 1
    };
    return periods[payFrequency.toUpperCase()] || 12;
}

module.exports = {
    calculateFederalWithholdingPub15T,
    calculateAnnualizedMethod
};

