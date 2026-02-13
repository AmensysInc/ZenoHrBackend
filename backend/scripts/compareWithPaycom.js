const { calculateStateWithholding } = require('../services/stateWithholdingService');

/**
 * Compare state withholding calculations with Paycom results
 * Useful for verifying official table imports
 */
async function compareWithPaycom(testCases) {
    console.log('🔍 Comparing State Withholding Calculations with Paycom\n');
    console.log('='.repeat(80));

    const results = [];

    for (const testCase of testCases) {
        const {
            state,
            grossPay,
            payFrequency = 'MONTHLY',
            filingStatus = 'SINGLE',
            annualGross,
            paycomResult,
            description = ''
        } = testCase;

        try {
            const calculated = await calculateStateWithholding(
                grossPay,
                payFrequency,
                state,
                filingStatus,
                annualGross || (grossPay * getPayPeriodsPerYear(payFrequency)),
                2026
            );

            const difference = calculated - paycomResult;
            const percentDiff = paycomResult > 0 ? ((difference / paycomResult) * 100).toFixed(2) : 0;
            const isMatch = Math.abs(difference) < 0.01; // Within 1 cent

            results.push({
                state,
                description,
                grossPay,
                payFrequency,
                filingStatus,
                calculated,
                paycomResult,
                difference,
                percentDiff,
                isMatch
            });

            const status = isMatch ? '✅' : Math.abs(difference) < 1 ? '⚠️' : '❌';
            console.log(`${status} ${state} - ${description || `${payFrequency} ${filingStatus}`}`);
            console.log(`   Gross: $${grossPay.toLocaleString()}`);
            console.log(`   Calculated: $${calculated.toFixed(2)}`);
            console.log(`   Paycom: $${paycomResult.toFixed(2)}`);
            console.log(`   Difference: $${difference.toFixed(2)} (${percentDiff}%)`);
            if (!isMatch) {
                console.log(`   ⚠️  Mismatch detected - verify official tables`);
            }
            console.log('');

        } catch (error) {
            console.error(`❌ Error calculating ${state}:`, error.message);
            results.push({
                state,
                description,
                error: error.message
            });
        }
    }

    // Summary
    console.log('='.repeat(80));
    console.log('📊 SUMMARY\n');

    const matched = results.filter(r => r.isMatch).length;
    const close = results.filter(r => !r.isMatch && r.difference && Math.abs(r.difference) < 1).length;
    const mismatched = results.filter(r => r.difference && Math.abs(r.difference) >= 1).length;
    const errors = results.filter(r => r.error).length;

    console.log(`   Total test cases: ${testCases.length}`);
    console.log(`   ✅ Matched (within $0.01): ${matched}`);
    console.log(`   ⚠️  Close (within $1.00): ${close}`);
    console.log(`   ❌ Mismatched (> $1.00): ${mismatched}`);
    console.log(`   ❌ Errors: ${errors}`);

    if (mismatched > 0 || errors > 0) {
        console.log('\n   ⚠️  Action Required:');
        console.log('   1. Verify official state withholding tables are imported');
        console.log('   2. Check if state uses allowances/exemptions');
        console.log('   3. Verify pay frequency and filing status match');
        console.log('   4. Some states may require state-specific adjustments');
    }

    return results;
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

// Example test cases - update with your Paycom results
const exampleTestCases = [
    {
        state: 'MO',
        grossPay: 8417,
        payFrequency: 'MONTHLY',
        filingStatus: 'SINGLE',
        annualGross: 8417 * 12,
        paycomResult: 285.00,
        description: 'Missouri - Known issue case'
    },
    {
        state: 'CA',
        grossPay: 5000,
        payFrequency: 'MONTHLY',
        filingStatus: 'SINGLE',
        annualGross: 5000 * 12,
        paycomResult: 252.40, // Update with actual Paycom result
        description: 'California - Monthly Single'
    },
    {
        state: 'NY',
        grossPay: 5000,
        payFrequency: 'MONTHLY',
        filingStatus: 'SINGLE',
        annualGross: 5000 * 12,
        paycomResult: 254.00, // Update with actual Paycom result
        description: 'New York - Monthly Single'
    }
];

// Run if called directly
if (require.main === module) {
    // You can pass test cases as JSON file or use examples
    const args = process.argv.slice(2);
    
    let testCases = exampleTestCases;

    if (args.length > 0 && args[0].endsWith('.json')) {
        const fs = require('fs');
        const testData = JSON.parse(fs.readFileSync(args[0], 'utf8'));
        testCases = testData.testCases || testData;
    }

    console.log(`Running ${testCases.length} test cases...\n`);

    compareWithPaycom(testCases)
        .then(() => {
            console.log('\n✅ Comparison complete');
            process.exit(0);
        })
        .catch((err) => {
            console.error('\n❌ Comparison failed:', err);
            process.exit(1);
        });
}

module.exports = { compareWithPaycom };

