const { calculateStateWithholding } = require('../services/stateWithholdingService');

async function testMultipleStates() {
    const testCases = [
        { state: 'VA', grossPay: 8013, payFreq: 'MONTHLY', filing: 'SINGLE', expected: '~$390-415' },
        { state: 'VA', grossPay: 5000, payFreq: 'MONTHLY', filing: 'SINGLE', expected: '~$200-250' },
        { state: 'CA', grossPay: 8000, payFreq: 'MONTHLY', filing: 'SINGLE', expected: '~$200-300' },
        { state: 'NY', grossPay: 8000, payFreq: 'MONTHLY', filing: 'SINGLE', expected: '~$300-400' },
    ];

    console.log('🧪 Testing State Tax Calculations\n');

    for (const test of testCases) {
        const annualGross = test.grossPay * 12;
        try {
            const result = await calculateStateWithholding(
                test.grossPay,
                test.payFreq,
                test.state,
                test.filing,
                annualGross,
                2026
            );
            console.log(`${test.state} - $${test.grossPay.toFixed(2)}/${test.payFreq} (${test.filing}):`);
            console.log(`  Result: $${result.toFixed(2)} (Expected: ${test.expected})`);
            console.log(`  Annual: $${(result * 12).toFixed(2)}\n`);
        } catch (error) {
            console.error(`${test.state}: Error - ${error.message}\n`);
        }
    }
}

if (require.main === module) {
    testMultipleStates()
        .then(() => {
            console.log('✅ Tests completed');
            process.exit(0);
        })
        .catch(err => {
            console.error('❌ Tests failed:', err);
            process.exit(1);
        });
}

module.exports = { testMultipleStates };

