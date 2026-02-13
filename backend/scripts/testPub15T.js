const { getDatabase } = require('../database/init');

const db = getDatabase();

// Test the exact query that would be used
const grossPay = 8749.70;
const payFrequency = 'MONTHLY';
const filingStatus = 'SINGLE';
const step2 = 0;
const taxYear = 2026;

console.log('Testing Pub 15-T lookup...');
console.log(`Gross Pay: $${grossPay}`);
console.log(`Pay Frequency: ${payFrequency}`);
console.log(`Filing Status: ${filingStatus}`);
console.log(`Step 2: ${step2}`);

db.get(`SELECT * FROM pub15t_percentage_tables 
        WHERE year = ? AND pay_frequency = ? AND filing_status = ? AND step2_checkbox = ?
        AND ? >= wage_min AND ? < wage_max
        LIMIT 1`,
    [taxYear, payFrequency, filingStatus, step2, grossPay, grossPay],
    (err, row) => {
        if (err) {
            console.error('Error:', err);
            db.close();
            return;
        }
        
        if (!row) {
            console.log('\n❌ No matching row found!');
            console.log('\nChecking what ranges exist...');
            
            db.all(`SELECT wage_min, wage_max FROM pub15t_percentage_tables 
                    WHERE year = ? AND pay_frequency = ? AND filing_status = ? AND step2_checkbox = ?
                    ORDER BY wage_min LIMIT 20`,
                [taxYear, payFrequency, filingStatus, step2],
                (err2, rows) => {
                    if (err2) {
                        console.error('Error:', err2);
                    } else {
                        console.log('\nAvailable wage ranges:');
                        rows.forEach(r => {
                            console.log(`  $${r.wage_min} - $${r.wage_max}`);
                        });
                    }
                    db.close();
                }
            );
        } else {
            console.log('\n✅ Found matching row:');
            console.log(JSON.stringify(row, null, 2));
            
            // Calculate withholding
            let withholding = 0;
            if (row.base_tax !== null && row.rate !== null && row.excess_over !== null) {
                withholding = row.base_tax + (row.rate * (grossPay - row.excess_over));
                console.log(`\nCalculation:`);
                console.log(`  base_tax: ${row.base_tax}`);
                console.log(`  rate: ${row.rate}`);
                console.log(`  excess_over: ${row.excess_over}`);
                console.log(`  Formula: ${row.base_tax} + (${row.rate} × (${grossPay} - ${row.excess_over}))`);
                console.log(`  Result: $${withholding.toFixed(2)}`);
            } else {
                console.log('\n⚠️ Row missing required columns!');
                console.log(`  base_tax: ${row.base_tax}`);
                console.log(`  rate: ${row.rate}`);
                console.log(`  excess_over: ${row.excess_over}`);
            }
            
            db.close();
        }
    }
);

