const { calculateStateWithholding } = require('../services/stateWithholdingService');
const { getDatabase } = require('../database/init');

/**
 * Test Kentucky withholding calculation
 */
async function testKentuckyWithholding() {
    console.log('🧪 Testing Kentucky State Withholding\n');
    console.log('='.repeat(80));
    
    // Test case from user
    const rate = 69.77;
    const hrs = 168;
    const grossPay = rate * hrs; // $11,721.36
    const preTaxMedical = 2553;
    const taxableGrossPay = grossPay - preTaxMedical; // $9,168.36
    const annualGross = taxableGrossPay * 12; // $110,020.32
    const payFrequency = 'MONTHLY';
    const filingStatus = 'SINGLE';
    
    console.log('Test Case:');
    console.log(`   Rate: $${rate}/hr`);
    console.log(`   Hours: ${hrs}`);
    console.log(`   Gross Pay: $${grossPay.toFixed(2)}`);
    console.log(`   Pre-tax Medical: $${preTaxMedical}`);
    console.log(`   Taxable Gross: $${taxableGrossPay.toFixed(2)}`);
    console.log(`   Annual: $${annualGross.toFixed(2)}`);
    console.log(`   Pay Frequency: ${payFrequency}`);
    console.log(`   Filing Status: ${filingStatus}`);
    console.log('');
    
    // Expected from Paycom
    const expectedState = 311.08;
    const expectedCounty = 257.87;
    
    console.log('Expected (Paycom):');
    console.log(`   State W/H: $${expectedState}`);
    console.log(`   Jefferson County: $${expectedCounty}`);
    console.log('');
    
    // Calculate current
    const calculated = await calculateStateWithholding(
        taxableGrossPay,
        payFrequency,
        'KY',
        filingStatus,
        annualGross,
        2026
    );
    
    console.log('Current Calculation:');
    console.log(`   State W/H: $${calculated.toFixed(2)}`);
    console.log(`   Difference: $${(calculated - expectedState).toFixed(2)}`);
    console.log('');
    
    // Check database values
    const db = getDatabase();
    
    console.log('Database Values:');
    db.get('SELECT standard_deduction FROM state_deductions WHERE year = 2026 AND state_code = ? AND filing_status = ?',
        ['KY', 'SINGLE'],
        (err, dedRow) => {
            if (err) {
                console.error('Error:', err);
                return;
            }
            console.log(`   Standard Deduction: $${dedRow ? dedRow.standard_deduction : 'Not found'}`);
            
            db.get('SELECT rate FROM state_brackets WHERE year = 2026 AND state_code = ? AND filing_status = ? AND bracket_min = 0 LIMIT 1',
                ['KY', 'SINGLE'],
                (err2, bracketRow) => {
                    if (err2) {
                        console.error('Error:', err2);
                        return;
                    }
                    console.log(`   Tax Rate: ${bracketRow ? (bracketRow.rate * 100).toFixed(2) + '%' : 'Not found'}`);
                    
                    // Check withholding table entry
                    db.get('SELECT * FROM state_withholding_tables WHERE year = 2026 AND state_code = ? AND pay_frequency = ? AND filing_status = ? AND ? >= wage_min AND ? < wage_max LIMIT 1',
                        [2026, 'KY', 'MONTHLY', 'SINGLE', taxableGrossPay, taxableGrossPay],
                        (err3, tableRow) => {
                            if (err3) {
                                console.error('Error:', err3);
                                return;
                            }
                            if (tableRow) {
                                console.log(`   Withholding Table Entry:`);
                                console.log(`      Wage Range: $${tableRow.wage_min}-$${tableRow.wage_max}`);
                                console.log(`      Withholding Amount: $${tableRow.withholding_amount}`);
                                console.log(`      Percentage: ${tableRow.percentage ? (tableRow.percentage * 100).toFixed(4) + '%' : 'N/A'}`);
                                console.log(`      Base Amount: $${tableRow.base_amount || 'N/A'}`);
                            } else {
                                console.log(`   No withholding table entry found`);
                            }
                            
                            console.log('');
                            console.log('Analysis:');
                            console.log(`   Current: $${calculated.toFixed(2)}`);
                            console.log(`   Expected: $${expectedState}`);
                            console.log(`   Difference: $${(calculated - expectedState).toFixed(2)}`);
                            console.log('');
                            console.log('Possible Issues:');
                            console.log('   1. Kentucky may use allowances/exemptions (not just deduction)');
                            console.log('   2. Kentucky may have official withholding tables that differ from bracket calculation');
                            console.log('   3. Deduction amount might be different for withholding vs tax liability');
                            console.log('   4. H1B status might affect withholding calculation');
                            
                            process.exit(0);
                        }
                    );
                }
            );
        }
    );
}

testKentuckyWithholding().catch(console.error);

