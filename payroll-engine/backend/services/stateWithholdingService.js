const { getDatabase } = require('../database/init');

/**
 * States with no state income tax
 * These states should return $0 withholding without table lookup
 */
const NO_INCOME_TAX_STATES = ['TX', 'FL', 'NV', 'NH', 'TN', 'WA', 'WY', 'SD', 'AK'];

/**
 * Calculate state withholding using official state withholding tables
 * Returns $0 for states with no income tax (TX, FL, NV, etc.)
 */
async function calculateStateWithholding(grossPay, payFrequency, state, filingStatus, annualGross, taxYear = 2026) {
    // Check if state has no income tax - return $0 immediately
    if (NO_INCOME_TAX_STATES.includes(state.toUpperCase())) {
        console.log(`[StateWithholding] ${state} has no state income tax. Returning $0.`);
        return Promise.resolve(0);
    }
    
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        // First, try to use state withholding tables
        db.get(`SELECT * FROM state_withholding_tables 
                WHERE year = ? AND state_code = ? AND pay_frequency = ? AND filing_status = ?
                AND ? >= wage_min AND ? < wage_max
                LIMIT 1`,
            [taxYear, state, payFrequency.toUpperCase(), filingStatus, grossPay, grossPay],
            async (err, row) => {
                if (err) {
                    reject(err);
                    return;
                }

                if (row) {
                    // Use withholding table
                    console.log(`[StateWithholding] Found table entry for ${state}:`, {
                        wageRange: `$${row.wage_min}-$${row.wage_max}`,
                        baseAmount: row.base_amount,
                        percentage: row.percentage,
                        withholdingAmount: row.withholding_amount,
                        grossPay: grossPay
                    });
                    
                    let withholding = 0;
                    
                    if (row.withholding_amount !== null && row.withholding_amount !== undefined) {
                        // Fixed withholding amount
                        withholding = row.withholding_amount;
                        console.log(`[StateWithholding] Using fixed withholding amount: $${withholding}`);
                    } else if (row.percentage !== null && row.percentage !== undefined) {
                        // Percentage-based withholding
                        // The percentage is an effective rate (tax/wage), so apply to full gross pay
                        if (row.base_amount !== null && row.base_amount !== undefined) {
                            // Base amount + percentage of full gross
                            withholding = row.base_amount + (grossPay * row.percentage);
                            console.log(`[StateWithholding] Base + percentage: ${row.base_amount} + (${grossPay} * ${row.percentage}) = $${withholding}`);
                        } else {
                            // Just percentage of full gross
                            withholding = grossPay * row.percentage;
                            console.log(`[StateWithholding] Percentage only: ${grossPay} * ${row.percentage} = $${withholding}`);
                        }
                    } else if (row.base_amount !== null && row.base_amount !== undefined) {
                        // Only base amount
                        withholding = row.base_amount;
                        console.log(`[StateWithholding] Using base amount only: $${withholding}`);
                    }
                    
                    const finalWithholding = Math.max(0, withholding);
                    console.log(`[StateWithholding] Final withholding for ${state}: $${finalWithholding}`);
                    resolve(finalWithholding);
                } else {
                    // No table found - CRITICAL: Enforce table-only calculations (no bracket fallback)
                    // Note: States with no income tax are handled above and return $0
                    const errorMsg = `CRITICAL: State withholding table not found for ${state} (${payFrequency}, ${filingStatus}). ` +
                                   `Official withholding tables are required. Bracket fallback is disabled for production accuracy. ` +
                                   `If ${state} has no state income tax, this should have been handled earlier.`;
                    console.error(`❌ ${errorMsg}`);
                    reject(new Error(errorMsg));
                    return;
                }
            }
        );
    });
}

/**
 * DEPRECATED: Bracket fallback removed for production accuracy
 * All states must have official withholding tables imported
 * This function is kept for reference only and should not be called
 */
async function calculateStateBracketMethod(annualGross, state, filingStatus, taxYear, payFrequency) {
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        // Get state standard deduction
        db.get(`SELECT standard_deduction FROM state_deductions 
                WHERE year = ? AND state_code = ? AND filing_status = ?`,
            [taxYear, state, filingStatus],
            (err, dedRow) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                const standardDeduction = dedRow ? dedRow.standard_deduction : 0;
                const taxableIncome = Math.max(0, annualGross - standardDeduction);
                
                // Check if flat rate state
                db.get(`SELECT rate FROM state_brackets 
                        WHERE year = ? AND state_code = ? AND filing_status = ?
                        AND bracket_min = 0
                        LIMIT 1`,
                    [taxYear, state, filingStatus],
                    (err2, flatRow) => {
                        if (err2) {
                            reject(err2);
                            return;
                        }
                        
                        if (flatRow && flatRow.rate) {
                            // Flat rate state
                            const annualTax = taxableIncome * flatRow.rate;
                            const payPeriodsPerYear = getPayPeriodsPerYear(payFrequency);
                            resolve(annualTax / payPeriodsPerYear);
                        } else {
                            // Bracket-based state
                            db.all(`SELECT bracket_min, bracket_max, rate FROM state_brackets 
                                    WHERE year = ? AND state_code = ? AND filing_status = ?
                                    ORDER BY bracket_min`,
                                [taxYear, state, filingStatus],
                                (err3, brackets) => {
                                    if (err3) {
                                        reject(err3);
                                        return;
                                    }
                                    
                                    if (!brackets || brackets.length === 0) {
                                        resolve(0); // No state tax
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
                                    
                                    const payPeriodsPerYear = getPayPeriodsPerYear(payFrequency);
                                    resolve(tax / payPeriodsPerYear);
                                }
                            );
                        }
                    }
                );
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
    calculateStateWithholding
};

