const fetch = require('node-fetch');

const testData = {
    grossPay: 8749.70,
    state: "MI",
    localJurisdiction: "NONE",
    employeeStatus: "H1B",
    filingStatus: "SINGLE",
    payPeriods: 12,
    w4Data: {
        step2Checkbox: false,
        step3Credits: 0,
        step4aOtherIncome: 0,
        step4bDeductions: 0,
        step4cExtraWithholding: 0
    },
    yearToDateGross: 0,
    yearToDateNet: 0,
    taxYear: 2026
};

fetch('http://localhost:3000/api/v1/payroll/calculate', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(testData)
})
.then(res => res.json())
.then(data => {
    console.log('API Response:');
    console.log(JSON.stringify(data, null, 2));
    
    if (data.paystub) {
        console.log('\nFederal Income Tax:', data.paystub.federalIncomeTax);
    }
})
.catch(err => {
    console.error('Error:', err.message);
});

