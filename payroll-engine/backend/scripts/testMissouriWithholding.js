const { calculateStateWithholding } = require('../services/stateWithholdingService');
const { getDatabase } = require('../database/init');

/**
 * Test Missouri withholding calculation
 */
async function testMissouriWithholding() {
    console.log('🧪 Testing Missouri State Withholding\n');
    console.log('='.repeat(80));
    
    // Test case from user
    const grossPay = 8417;
    const taxableGrossPay = 7729; // After $688 pre-tax deduction
    const annualGross = taxableGrossPay * 12; // $92,748
    const payFrequency = 'MONTHLY';
    const filingStatus = 'SINGLE';
    
    console.log('Test Case:');
    console.log(`   Gross Pay: $${grossPay.toFixed(2)}`);
    console.log(`   Taxable Gross: $${taxableGrossPay.toFixed(2)}`);
    console.log(`   Annual: $${annualGross.toFixed(2)}`);
    console.log(`   Pay Frequency: ${payFrequency}`);
    console.log(`   Filing Status: ${filingStatus}`);
    console.log('');
    
    // Expected from Paycom
    const expectedState = 285.00;
    
    console.log('Expected (Paycom):');
    console.log(`   State W/H: $${expectedState}`);
    console.log('');
    
    // Calculate current
    const calculated = await calculateStateWithholding(
        taxableGrossPay,
        payFrequency,
        'MO',
        filingStatus,
        annualGross,
        2026
    );
    
    console.log('Current Calculation:');
    console.log(`   State W/H: $${calculated.toFixed(2)}`);
    console.log(`   Difference: $${(calculated - expectedState).toFixed(2)}`);
    console.log(`   Percent Difference: ${((calculated - expectedState) / expectedState * 100).toFixed(2)}%`);
    console.log('');
    
    // Check database values
    const db = getDatabase();
    
    console.log('Database Values:');
    db.get(`SELECT * FROM state_withholding_tables 
            WHERE year = 2026 AND state_code = 'MO' 
            AND pay_frequency = 'MONTHLY' AND filing_status = 'SINGLE'
            AND ? >= wage_min AND ? < wage_max
            LIMIT 1`,
        [taxableGrossPay, taxableGrossPay],
        (err, row) => {
            if (err) {
                console.error('Error:', err);
                return;
            }
            
            if (row) {
                console.log(`   Found table entry:`);
                console.log(`   Wage Range: $${row.wage_min} - $${row.wage_max}`);
                console.log(`   Percentage: ${row.percentage}`);
                console.log(`   Base Amount: ${row.base_amount}`);
                console.log(`   Withholding Amount: ${row.withholding_amount}`);
                
                // Calculate manually
                let manual = 0;
                if (row.withholding_amount !== null) {
                    manual = row.withholding_amount;
                } else if (row.percentage !== null) {
                    if (row.base_amount !== null) {
                        manual = row.base_amount + (taxableGrossPay * row.percentage);
                    } else {
                        manual = taxableGrossPay * row.percentage;
                    }
                }
                console.log(`   Manual Calculation: $${manual.toFixed(2)}`);
            } else {
                console.log('   ⚠️  No table entry found - using bracket method');
            }
            
            console.log('');
            console.log('='.repeat(80));
            
            if (Math.abs(calculated - expectedState) < 1) {
                console.log('✅ PASS: Calculation matches Paycom (within $1)');
            } else {
                console.log('❌ FAIL: Calculation does not match Paycom');
            }
            
            process.exit(0);
        }
    );
}

testMissouriWithholding().catch(console.error);

