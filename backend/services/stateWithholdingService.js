const { getDatabase } = require('../database/init');

/**
 * Calculate state withholding using official state withholding tables
 * Falls back to bracket calculation if tables not available
 */
async function calculateStateWithholding(grossPay, payFrequency, state, filingStatus, annualGross, taxYear = 2026) {
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
                    // CRITICAL: No table found - calculation cannot proceed
                    // All calculations MUST use official withholding tables
                    // Bracket methods are NOT allowed as they do not match official withholding tables
                    console.error(`❌ CRITICAL ERROR: State withholding table not found for ${state} (${payFrequency}, ${filingStatus})`);
                    console.error(`   This indicates missing official withholding tables.`);
                    console.error(`   All 42 states MUST have official tables imported for 2026.`);
                    console.error(`   Calculation blocked - cannot use bracket method fallback.`);
                    reject(new Error(`State withholding table not found for ${state} (${payFrequency}, ${filingStatus}). Official withholding tables required.`));
                }
            }
        );
    });
}

/**
 * REMOVED: calculateStateBracketMethod
 * 
 * All calculations MUST use official state withholding tables.
 * Bracket methods are NOT allowed as they do not match official withholding tables.
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
    calculateStateWithholding
};

