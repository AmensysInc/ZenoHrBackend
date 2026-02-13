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
        console.log(`[Pub15T] Lookup: year=${taxYear}, freq=${payFrequency}, status=${filingStatus}, step2=${step2}, wages=${taxableWages}`);
        
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
                        // CRITICAL: No valid data in table
                        // All calculations MUST use Pub 15-T percentage tables
                        // Bracket methods are NOT allowed
                        console.error('[Pub15T] CRITICAL: Row missing required columns:', row);
                        reject(new Error(`Pub 15-T table entry missing required data. Official Pub 15-T percentage tables required.`));
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
                    
                    console.log(`[Pub15T] Final withholding: ${withholding}`);
                    resolve(withholding);
                } else {
                    // CRITICAL: Table not found - calculation cannot proceed
                    // All calculations MUST use Pub 15-T percentage tables
                    // Bracket methods are NOT allowed as they do not match IRS Pub 15-T
                    console.error(`[Pub15T] CRITICAL ERROR: Table not found for: year=${taxYear}, freq=${payFrequency}, status=${filingStatus}, step2=${step2}, wages=${taxableWages}`);
                    console.error(`   Pub 15-T percentage tables are required for all calculations.`);
                    console.error(`   Calculation blocked - cannot use bracket method fallback.`);
                    reject(new Error(`Pub 15-T percentage table not found for year=${taxYear}, freq=${payFrequency}, status=${filingStatus}, step2=${step2}, wages=${taxableWages}. Official Pub 15-T tables required.`));
                }
            }
        );
    });
}

/**
 * REMOVED: calculateAnnualizedMethod and calculateTaxFromBrackets
 * 
 * All calculations MUST use IRS Pub 15-T percentage method tables.
 * Bracket methods are NOT allowed as they do not match IRS Pub 15-T.
 * If tables are missing, calculation will fail with an error.
 */

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
    calculateFederalWithholdingPub15T
};

