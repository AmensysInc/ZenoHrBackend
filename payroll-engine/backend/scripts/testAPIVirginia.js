const http = require('http');

// Test the API endpoint with Virginia data
const testData = {
    grossPay: 9408,
    taxableGrossPay: 8013,
    preTaxDeductions: {
        advance: 0,
        medical: 0,
        miscellaneous: 1395,
        total: 1395
    },
    state: 'VA',
    localJurisdiction: 'NONE',
    employeeStatus: 'H1B',
    filingStatus: 'SINGLE',
    payPeriods: 12, // Monthly
    w4Data: {},
    yearToDateGross: 9408,
    yearToDateNet: 0,
    taxYear: 2026
};

const postData = JSON.stringify(testData);

const options = {
    hostname: 'localhost',
    port: 3000,
    path: '/api/v1/payroll/calculate',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
    }
};

console.log('🧪 Testing API Endpoint for Virginia State Tax\n');
console.log('Request Data:');
console.log(JSON.stringify(testData, null, 2));
console.log('\nSending request...\n');

const req = http.request(options, (res) => {
    let data = '';

    res.on('data', (chunk) => {
        data += chunk;
    });

    res.on('end', () => {
        try {
            const response = JSON.parse(data);
            console.log('Response Status:', res.statusCode);
            console.log('\nResponse Data:');
            console.log(JSON.stringify(response, null, 2));
            
            if (response.paystub) {
                console.log('\n📊 Key Values:');
                console.log(`  State Income Tax: $${response.paystub.stateIncomeTax?.toFixed(2) || 'N/A'}`);
                console.log(`  Taxable Gross Pay: $${response.paystub.taxableGrossPay?.toFixed(2) || 'N/A'}`);
                console.log(`  Gross Pay: $${response.paystub.grossPay?.toFixed(2) || 'N/A'}`);
                
                if (response.paystub.stateIncomeTax < 10) {
                    console.log('\n⚠️  WARNING: State income tax seems too low!');
                }
            }
        } catch (error) {
            console.error('Error parsing response:', error);
            console.log('Raw response:', data);
        }
    });
});

req.on('error', (error) => {
    console.error('Request error:', error.message);
    console.log('\n💡 Make sure the backend server is running on port 3000');
    console.log('   Run: cd backend && node server.js');
});

req.write(postData);
req.end();

