const { getDatabase } = require('../database/init');

/**
 * Import state tax data from frontend calculator
 * This imports brackets, deductions, and payroll taxes
 */
async function importStateData() {
    const db = getDatabase();
    const year = 2026;

    // State tax data - comprehensive import
    
    // Instead, we'll create a comprehensive import script
    const states = {
        'AL': { type: 'brackets', brackets: [[0, 0.02], [500, 0.04], [3000, 0.05]], deduction: 2500 },
        'AK': { type: 'none' },
        'AZ': { type: 'flat', rate: 0.025, deduction: 12950 },
        'AR': { type: 'brackets', brackets: [[0, 0.02], [4400, 0.04], [8800, 0.055], [14700, 0.06]], deduction: 2200 },
        'CA': { type: 'brackets', brackets: [[0, 0.01], [10099, 0.02], [23942, 0.04], [37788, 0.06], [52455, 0.08], [66295, 0.093], [338639, 0.103], [406364, 0.113], [677275, 0.123]], deduction: 5202 },
        'CO': { type: 'flat', rate: 0.044, deduction: 0 },
        'CT': { type: 'brackets', brackets: [[0, 0.03], [10000, 0.05], [50000, 0.055], [100000, 0.06], [200000, 0.065], [250000, 0.069], [500000, 0.0699]], deduction: 0 },
        'DE': { type: 'brackets', brackets: [[0, 0.022], [2000, 0.039], [5000, 0.048], [10000, 0.052], [20000, 0.0555], [25000, 0.066]], deduction: 3250 },
        'FL': { type: 'none' },
        'GA': { type: 'brackets', brackets: [[0, 0.01], [750, 0.02], [2250, 0.03], [3750, 0.04], [5250, 0.05], [7000, 0.0575]], deduction: 5400 },
        'HI': { type: 'brackets', brackets: [[0, 0.014], [2400, 0.032], [4800, 0.055], [9600, 0.064], [14400, 0.068], [19200, 0.072], [24000, 0.076], [36000, 0.079], [48000, 0.0825]], deduction: 2200 },
        'ID': { type: 'flat', rate: 0.058, deduction: 12950 },
        'IL': { type: 'flat', rate: 0.0495, deduction: 0 },
        'IN': { type: 'flat', rate: 0.0315, deduction: 0 },
        'IA': { type: 'brackets', brackets: [[0, 0.0033], [1740, 0.0067], [3480, 0.0225], [6960, 0.0414], [15660, 0.0563], [26100, 0.0596], [34800, 0.0625], [52200, 0.0744], [78300, 0.0853]], deduction: 2100 },
        'KS': { type: 'brackets', brackets: [[0, 0.031], [15000, 0.0525], [30000, 0.057]], deduction: 3500 },
        'KY': { type: 'flat', rate: 0.045, deduction: 2810 },
        'LA': { type: 'brackets', brackets: [[0, 0.0185], [12500, 0.035], [50000, 0.0425]], deduction: 4500 },
        'ME': { type: 'brackets', brackets: [[0, 0.058], [24500, 0.0675], [58050, 0.0715]], deduction: 12950 },
        'MD': { type: 'brackets', brackets: [[0, 0.02], [1000, 0.03], [2000, 0.04], [3000, 0.0475], [100000, 0.05], [125000, 0.0525], [150000, 0.055], [250000, 0.0575]], deduction: 2400 },
        'MA': { type: 'flat', rate: 0.05, deduction: 0 },
        'MI': { type: 'flat', rate: 0.0425, deduction: 0 },
        'MN': { type: 'brackets', brackets: [[0, 0.0535], [30420, 0.068], [100230, 0.0785]], deduction: 12950 },
        'MS': { type: 'brackets', brackets: [[0, 0.00], [5000, 0.03], [10000, 0.04], [50000, 0.05]], deduction: 2300 },
        'MO': { type: 'brackets', brackets: [[0, 0.015], [1121, 0.02], [2242, 0.025], [3363, 0.03], [4484, 0.035], [5605, 0.04], [6726, 0.045], [7847, 0.05], [8968, 0.0525], [10089, 0.055], [11210, 0.0575]], deduction: 12950 },
        'MT': { type: 'brackets', brackets: [[0, 0.01], [2100, 0.02], [3600, 0.03], [5400, 0.04], [7200, 0.05], [9300, 0.06], [12000, 0.0675], [15400, 0.069]], deduction: 5400 },
        'NE': { type: 'brackets', brackets: [[0, 0.0246], [3700, 0.0351], [22170, 0.0501], [35730, 0.0684]], deduction: 7900 },
        'NV': { type: 'none' },
        'NH': { type: 'flat', rate: 0.05, deduction: 0 }, // Interest and dividends only
        'NJ': { type: 'brackets', brackets: [[0, 0.014], [20000, 0.0175], [35000, 0.035], [40000, 0.05525], [75000, 0.0637], [500000, 0.0897], [1000000, 0.1075]], deduction: 0 },
        'NM': { type: 'brackets', brackets: [[0, 0.017], [5500, 0.032], [11000, 0.047], [16000, 0.049], [210000, 0.059]], deduction: 12950 },
        'NY': { type: 'brackets', brackets: [[0, 0.04], [8500, 0.045], [11700, 0.0525], [13900, 0.055], [21400, 0.06], [80650, 0.0625], [215400, 0.0685], [1077550, 0.0965], [5000000, 0.103], [25000000, 0.109]], deduction: 8000 },
        'NC': { type: 'flat', rate: 0.0475, deduction: 12950 },
        'ND': { type: 'brackets', brackets: [[0, 0.011], [44825, 0.0204], [212750, 0.0227], [458350, 0.0264]], deduction: 12950 },
        'OH': { type: 'brackets', brackets: [[0, 0.0], [26050, 0.025], [41700, 0.035], [83350, 0.0375], [104250, 0.04], [208500, 0.05]], deduction: 0 },
        'OK': { type: 'brackets', brackets: [[0, 0.0025], [1000, 0.0075], [2500, 0.0175], [3750, 0.0275], [4900, 0.0375], [7200, 0.0475]], deduction: 6350 },
        'OR': { type: 'brackets', brackets: [[0, 0.0475], [4050, 0.0675], [10200, 0.0875]], deduction: 2460 },
        'PA': { type: 'flat', rate: 0.0307, deduction: 0 },
        'RI': { type: 'brackets', brackets: [[0, 0.0375], [68200, 0.0475], [155050, 0.0599]], deduction: 9950 },
        'SC': { type: 'brackets', brackets: [[0, 0.0], [3200, 0.03], [6410, 0.04], [9620, 0.05], [12820, 0.06], [16030, 0.07]], deduction: 12950 },
        'SD': { type: 'none' },
        'TN': { type: 'none' },
        'TX': { type: 'none' },
        'UT': { type: 'flat', rate: 0.0485, deduction: 0 },
        'VT': { type: 'brackets', brackets: [[0, 0.0335], [45200, 0.066], [109450, 0.076], [232950, 0.0875]], deduction: 6500 },
        'VA': { type: 'brackets', brackets: [[0, 0.02], [3000, 0.03], [5000, 0.05], [17000, 0.0575]], deduction: 8000 },
        'WA': { type: 'none' },
        'WV': { type: 'brackets', brackets: [[0, 0.03], [10000, 0.04], [25000, 0.045], [40000, 0.06], [60000, 0.065]], deduction: 0 },
        'WI': { type: 'brackets', brackets: [[0, 0.0354], [14020, 0.0405], [28040, 0.0535], [308910, 0.0765]], deduction: 12760 },
        'WY': { type: 'none' },
        'DC': { type: 'brackets', brackets: [[0, 0.04], [10000, 0.06], [40000, 0.065], [60000, 0.085], [250000, 0.0925], [500000, 0.0975], [1000000, 0.1075]], deduction: 13850 }
    };

    // State payroll taxes
    const statePayrollTaxes = {
        'NJ': [
            { tax_type: 'UI', rate: 0.003825, wage_base: 42900, employee: true },
            { tax_type: 'DI', rate: 0.0026, wage_base: 156800, employee: true },
            { tax_type: 'FLI', rate: 0.0009, wage_base: 156800, employee: true }
        ],
        'CA': [
            { tax_type: 'SDI', rate: 0.011, wage_base: 153164, employee: true }
        ],
        'NY': [
            { tax_type: 'SDI', rate: 0.005, wage_base: 12000, employee: true },
            { tax_type: 'PFL', rate: 0.00373, wage_base: 153164, employee: true }
        ],
        'HI': [
            { tax_type: 'TDI', rate: 0.005, wage_base: 56700, employee: true }
        ],
        'RI': [
            { tax_type: 'TDI', rate: 0.012, wage_base: 84900, employee: true }
        ],
        'WA': [
            { tax_type: 'PFL', rate: 0.006, wage_base: 168600, employee: true }
        ],
        'MA': [
            { tax_type: 'PFML', rate: 0.0034, wage_base: 160000, employee: true }
        ],
        'CO': [
            { tax_type: 'FAMLI', rate: 0.0045, wage_base: 153164, employee: true }
        ],
        'OR': [
            { tax_type: 'PLO', rate: 0.006, wage_base: 132900, employee: true }
        ],
        'CT': [
            { tax_type: 'PL', rate: 0.005, wage_base: 160000, employee: true }
        ],
        'DC': [
            { tax_type: 'PFL', rate: 0.0026, wage_base: 132900, employee: true }
        ]
    };

    return new Promise((resolve, reject) => {
        db.serialize(() => {
            // Import state brackets and deductions
            const bracketStmt = db.prepare(`INSERT OR REPLACE INTO state_brackets 
                (year, state_code, filing_status, bracket_min, bracket_max, rate) 
                VALUES (?, ?, ?, ?, ?, ?)`);

            const dedStmt = db.prepare(`INSERT OR REPLACE INTO state_deductions 
                (year, state_code, filing_status, standard_deduction) 
                VALUES (?, ?, ?, ?)`);

            const payrollStmt = db.prepare(`INSERT OR REPLACE INTO state_payroll_taxes 
                (year, state_code, tax_type, rate, wage_base, employee_paid) 
                VALUES (?, ?, ?, ?, ?, ?)`);

            Object.keys(states).forEach(stateCode => {
                const state = states[stateCode];
                
                // Insert deductions (simplified - using SINGLE for all)
                if (state.deduction !== undefined) {
                    dedStmt.run(year, stateCode, 'SINGLE', state.deduction);
                }

                // Insert brackets
                if (state.type === 'brackets' && state.brackets) {
                    for (let i = 0; i < state.brackets.length; i++) {
                        const [threshold, rate] = state.brackets[i];
                        const min = i === 0 ? 0 : state.brackets[i - 1][0];
                        const max = i === state.brackets.length - 1 ? 999999999 : threshold;
                        bracketStmt.run(year, stateCode, 'SINGLE', min, max, rate);
                    }
                } else if (state.type === 'flat' && state.rate) {
                    bracketStmt.run(year, stateCode, 'SINGLE', 0, 999999999, state.rate);
                }

                // Insert payroll taxes
                if (statePayrollTaxes[stateCode]) {
                    statePayrollTaxes[stateCode].forEach(tax => {
                        payrollStmt.run(year, stateCode, tax.tax_type, tax.rate, 
                                       tax.wage_base, tax.employee ? 1 : 0);
                    });
                }
            });

            bracketStmt.finalize();
            dedStmt.finalize();
            payrollStmt.finalize((err) => {
                if (err) {
                    reject(err);
                } else {
                    console.log('State data imported successfully');
                    resolve();
                }
            });
        });
    });
}

module.exports = { importStateData };

if (require.main === module) {
    importStateData()
        .then(() => {
            console.log('State data import complete');
            process.exit(0);
        })
        .catch(err => {
            console.error('Import error:', err);
            process.exit(1);
        });
}

