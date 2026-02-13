const { getDatabase } = require('../database/init');

/**
 * Import state withholding tables for all states with income tax
 * This generates withholding tables based on state brackets
 * NOTE: These are approximate. For production accuracy, import official state withholding tables.
 */
async function importStateWithholdingTables(clearExisting = false) {
    const db = getDatabase();
    const year = 2026;

    // States with income tax (exclude: AK, FL, NV, NH, SD, TN, TX, WA, WY)
    const statesWithTax = [
        'AL', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'GA', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY',
        'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NJ', 'NM', 'NY', 'NC', 'ND',
        'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 'UT', 'VT', 'VA', 'WV', 'WI', 'DC'
    ];

    const payFrequencies = ['WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'];
    const filingStatuses = ['SINGLE', 'MARRIED', 'MARRIED_SEPARATE', 'HEAD_OF_HOUSEHOLD'];

    console.log('Starting state withholding table import...');
    console.log(`States to process: ${statesWithTax.length}`);
    console.log(`Pay frequencies: ${payFrequencies.length}`);
    console.log(`Filing statuses: ${filingStatuses.length}`);
    console.log(`Total combinations: ${statesWithTax.length * payFrequencies.length * filingStatuses.length}\n`);

    // Clear existing data if requested
    if (clearExisting) {
        console.log('Clearing existing data for year', year, '...');
        await new Promise((resolve, reject) => {
            db.run(`DELETE FROM state_withholding_tables WHERE year = ?`, [year], (err) => {
                if (err) reject(err);
                else {
                    console.log('✅ Existing data cleared\n');
                    resolve();
                }
            });
        });
    }

    let totalInserted = 0;

    for (const stateCode of statesWithTax) {
        console.log(`Processing ${stateCode}...`);

        // Get state brackets and deduction from database
        const stateData = await getStateData(db, year, stateCode);
        
        if (!stateData) {
            console.log(`  ⚠️  No state data found for ${stateCode}, skipping...`);
            continue;
        }

        for (const payFreq of payFrequencies) {
            for (const filingStatus of filingStatuses) {
                const tables = generateWithholdingTables(stateCode, stateData, payFreq, filingStatus, year);
                
                for (const table of tables) {
                    // Use INSERT OR IGNORE to prevent duplicates
                    db.run(`INSERT OR IGNORE INTO state_withholding_tables 
                            (year, state_code, pay_frequency, filing_status, wage_min, wage_max, base_amount, percentage, withholding_amount)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                        [
                            year,
                            stateCode,
                            table.payFrequency,
                            table.filingStatus,
                            table.wageMin,
                            table.wageMax,
                            table.baseAmount || null,
                            table.percentage !== undefined && table.percentage !== null ? table.percentage : 0,
                            table.withholdingAmount || null
                        ],
                        (err) => {
                            if (err) {
                                console.error(`  ❌ Error inserting ${stateCode} ${payFreq} ${filingStatus}:`, err.message);
                            } else {
                                totalInserted++;
                            }
                        }
                    );
                }
            }
        }
    }

    // Wait for all inserts to complete
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    console.log(`\n✅ Import complete! Total tables inserted: ${totalInserted}`);
    console.log(`\n⚠️  IMPORTANT: These are approximate tables based on state brackets.`);
    console.log(`   For production accuracy, import official state withholding tables from each state's revenue department.`);
}

function getStateData(db, year, stateCode) {
    return new Promise((resolve, reject) => {
        // Get brackets
        db.all(`SELECT bracket_min, bracket_max, rate FROM state_brackets 
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

                        // Check if flat rate
                        if (brackets.length === 1 && brackets[0].bracket_min === 0 && brackets[0].bracket_max === null) {
                            resolve({
                                type: 'flat',
                                rate: brackets[0].rate,
                                deduction: dedRow ? dedRow.standard_deduction : 0
                            });
                        } else if (brackets.length > 0) {
                            resolve({
                                type: 'brackets',
                                brackets: brackets,
                                deduction: dedRow ? dedRow.standard_deduction : 0
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

function generateWithholdingTables(stateCode, stateData, payFrequency, filingStatus, year) {
    const tables = [];
    const payPeriodsPerYear = getPayPeriodsPerYear(payFrequency);
    
    // Generate wage ranges for withholding table
    // Start from $0 and go up to $50,000 per period (reasonable max for most states)
    const maxWage = 50000;
    const stepSize = getStepSize(payFrequency);
    
    if (stateData.type === 'flat') {
        // Flat rate state - simple percentage
        for (let wageMin = 0; wageMin < maxWage; wageMin += stepSize) {
            const wageMax = Math.min(wageMin + stepSize, maxWage);
            tables.push({
                payFrequency,
                filingStatus,
                wageMin,
                wageMax,
                baseAmount: null,
                percentage: stateData.rate,
                withholdingAmount: null
            });
        }
    } else if (stateData.type === 'brackets') {
        // Bracket-based state - generate approximate withholding
        // This is a simplified approach - official tables may differ
        for (let wageMin = 0; wageMin < maxWage; wageMin += stepSize) {
            const wageMax = Math.min(wageMin + stepSize, maxWage);
            
            // Calculate approximate withholding for midpoint of range
            const midWage = (wageMin + wageMax) / 2;
            const annualWage = midWage * payPeriodsPerYear;
            const taxableIncome = Math.max(0, annualWage - stateData.deduction);
            
            // Calculate tax using brackets
            let tax = 0;
            for (const bracket of stateData.brackets) {
                if (taxableIncome > bracket.bracket_min) {
                    const maxInBracket = bracket.bracket_max === null ? taxableIncome : Math.min(taxableIncome, bracket.bracket_max);
                    const taxableAtThisRate = maxInBracket - bracket.bracket_min;
                    if (taxableAtThisRate > 0) {
                        tax += taxableAtThisRate * bracket.rate;
                    }
                }
            }
            
            const perPeriodTax = tax / payPeriodsPerYear;
            
            // Store as percentage for easier lookup
            let effectiveRate;
            if (midWage > 0 && perPeriodTax > 0) {
                effectiveRate = perPeriodTax / midWage;
            } else if (taxableIncome <= 0) {
                // No taxable income, no withholding
                effectiveRate = 0;
            } else if (stateData.brackets && stateData.brackets.length > 0) {
                // Use first bracket rate as fallback
                effectiveRate = stateData.brackets[0].rate;
            } else {
                // Fallback to 0 if no brackets
                effectiveRate = 0;
            }
            
            // Ensure rate is a valid number
            if (isNaN(effectiveRate) || !isFinite(effectiveRate)) {
                effectiveRate = 0;
            }
            
            tables.push({
                payFrequency,
                filingStatus,
                wageMin,
                wageMax,
                baseAmount: null,
                percentage: effectiveRate,
                withholdingAmount: null
            });
        }
    }
    
    return tables;
}

function getPayPeriodsPerYear(payFrequency) {
    const periods = {
        'WEEKLY': 52,
        'BIWEEKLY': 26,
        'SEMIMONTHLY': 24,
        'MONTHLY': 12
    };
    return periods[payFrequency.toUpperCase()] || 12;
}

function getStepSize(payFrequency) {
    // Step size for wage ranges in withholding table
    const steps = {
        'WEEKLY': 50,        // $50 increments
        'BIWEEKLY': 100,     // $100 increments
        'SEMIMONTHLY': 100,  // $100 increments
        'MONTHLY': 200       // $200 increments
    };
    return steps[payFrequency.toUpperCase()] || 200;
}

// Run if called directly
if (require.main === module) {
    importStateWithholdingTables()
        .then(() => {
            console.log('\n✅ Script completed successfully');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Script failed:', err);
            process.exit(1);
        });
}

module.exports = { importStateWithholdingTables };

