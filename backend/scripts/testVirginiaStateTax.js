const { getDatabase } = require('../database/init');
const { calculateStateWithholding } = require('../services/stateWithholdingService');

async function testVirginiaStateTax() {
    const db = getDatabase();
    const year = 2026;
    
    // Test case from the UI: $8,013 taxable gross per month, Single filing
    const grossPay = 8013;
    const payFrequency = 'MONTHLY';
    const state = 'VA';
    const filingStatus = 'SINGLE';
    const annualGross = 8013 * 12; // $96,156
    
    console.log('🔍 Testing Virginia State Tax Calculation\n');
    console.log(`Input:`);
    console.log(`  Gross Pay per Period: $${grossPay.toFixed(2)}`);
    console.log(`  Pay Frequency: ${payFrequency}`);
    console.log(`  Filing Status: ${filingStatus}`);
    console.log(`  Annual Gross: $${annualGross.toFixed(2)}\n`);
    
    // Check what's in the withholding table
    console.log('📊 Checking State Withholding Table:');
    const tableRow = await new Promise((resolve, reject) => {
        db.get(`SELECT * FROM state_withholding_tables 
                WHERE year = ? AND state_code = ? AND pay_frequency = ? AND filing_status = ?
                AND ? >= wage_min AND ? < wage_max
                LIMIT 1`,
            [year, state, payFrequency, filingStatus, grossPay, grossPay],
            (err, row) => {
                if (err) reject(err);
                else resolve(row);
            });
    });
    
    if (tableRow) {
        console.log(`  Found table entry:`);
        console.log(`    Wage Range: $${tableRow.wage_min} - $${tableRow.wage_max}`);
        console.log(`    Base Amount: ${tableRow.base_amount || 'NULL'}`);
        console.log(`    Percentage: ${tableRow.percentage ? (tableRow.percentage * 100).toFixed(4) + '%' : 'NULL'}`);
        console.log(`    Withholding Amount: ${tableRow.withholding_amount || 'NULL'}`);
        
        // Calculate what the current code would return
        let withholding = tableRow.base_amount || 0;
        if (tableRow.percentage) {
            withholding += (grossPay - tableRow.wage_min) * tableRow.percentage;
        } else if (tableRow.withholding_amount) {
            withholding = tableRow.withholding_amount;
        }
        console.log(`\n  Current calculation result: $${withholding.toFixed(2)}`);
        console.log(`    Formula: base_amount (${tableRow.base_amount || 0}) + (grossPay - wage_min) * percentage`);
        console.log(`    = ${tableRow.base_amount || 0} + (${grossPay} - ${tableRow.wage_min}) * ${tableRow.percentage}`);
        console.log(`    = ${tableRow.base_amount || 0} + ${(grossPay - tableRow.wage_min) * tableRow.percentage}`);
        
        // What it should be (if percentage is effective rate)
        const shouldBe = grossPay * tableRow.percentage;
        console.log(`\n  If percentage is effective rate: $${shouldBe.toFixed(2)}`);
        console.log(`    Formula: grossPay * percentage = ${grossPay} * ${tableRow.percentage}`);
    } else {
        console.log('  ❌ No table entry found - will use bracket method');
    }
    
    // Check bracket method
    console.log('\n📊 Checking Bracket Method (Fallback):');
    const deduction = await new Promise((resolve, reject) => {
        db.get(`SELECT standard_deduction FROM state_deductions 
                WHERE year = ? AND state_code = ? AND filing_status = ?`,
            [year, state, filingStatus],
            (err, row) => {
                if (err) reject(err);
                else resolve(row ? row.standard_deduction : 0);
            });
    });
    
    console.log(`  Standard Deduction: $${deduction.toFixed(2)}`);
    const taxableIncome = Math.max(0, annualGross - deduction);
    console.log(`  Taxable Income: $${taxableIncome.toFixed(2)}`);
    
    const brackets = await new Promise((resolve, reject) => {
        db.all(`SELECT bracket_min, bracket_max, rate FROM state_brackets 
                WHERE year = ? AND state_code = ? AND filing_status = ?
                ORDER BY bracket_min`,
            [year, state, filingStatus],
            (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
    });
    
    if (brackets && brackets.length > 0) {
        console.log(`  Brackets:`);
        let tax = 0;
        for (const bracket of brackets) {
            if (taxableIncome > bracket.bracket_min) {
                const maxInBracket = bracket.bracket_max === null ? taxableIncome : Math.min(taxableIncome, bracket.bracket_max);
                const taxableAtThisRate = maxInBracket - bracket.bracket_min;
                if (taxableAtThisRate > 0) {
                    const bracketTax = taxableAtThisRate * bracket.rate;
                    tax += bracketTax;
                    console.log(`    $${bracket.bracket_min.toFixed(2)} - $${bracket.bracket_max === null ? '∞' : bracket.bracket_max.toFixed(2)} @ ${(bracket.rate * 100).toFixed(2)}%: $${bracketTax.toFixed(2)}`);
                }
            }
        }
        const monthlyTax = tax / 12;
        console.log(`\n  Annual Tax: $${tax.toFixed(2)}`);
        console.log(`  Monthly Tax (bracket method): $${monthlyTax.toFixed(2)}`);
    }
    
    // Run the actual calculation
    console.log('\n📊 Running Actual Calculation:');
    try {
        const result = await calculateStateWithholding(grossPay, payFrequency, state, filingStatus, annualGross, year);
        console.log(`  Result: $${result.toFixed(2)}`);
    } catch (error) {
        console.error(`  Error: ${error.message}`);
    }
}

if (require.main === module) {
    testVirginiaStateTax()
        .then(() => {
            console.log('\n✅ Test completed');
            process.exit(0);
        })
        .catch(err => {
            console.error('\n❌ Test failed:', err);
            process.exit(1);
        });
}

module.exports = { testVirginiaStateTax };

