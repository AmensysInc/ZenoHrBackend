const { calculateFederalWithholdingPub15T } = require('./pub15tService');
const { calculateStateWithholding } = require('./stateWithholdingService');
const { getDatabase } = require('../database/init');

/**
 * Main payroll calculation service
 * Orchestrates all tax calculations
 */
async function calculatePaystub(input) {
    const {
        grossPay,
        taxableGrossPay, // Gross pay after pre-tax deductions
        preTaxDeductions,
        state,
        localJurisdiction,
        employeeStatus,
        filingStatus,
        payPeriods,
        w4Data,
        yearToDateGross,
        yearToDateNet,
        taxYear
    } = input;

    // Calculate taxable gross pay (after pre-tax deductions)
    let totalPreTaxDeductions = 0;
    if (preTaxDeductions) {
        console.error('[PayrollService] Pre-tax deductions received:', JSON.stringify(preTaxDeductions));
        totalPreTaxDeductions = (preTaxDeductions.advance || 0) + 
                               (preTaxDeductions.medical || 0) + 
                               (preTaxDeductions.miscellaneous || 0);
        console.error('[PayrollService] Total pre-tax deductions:', totalPreTaxDeductions);
    } else {
        console.error('[PayrollService] No pre-tax deductions provided');
    }
    const taxableGross = taxableGrossPay || Math.max(0, grossPay - totalPreTaxDeductions);
    console.error('[PayrollService] Gross Pay:', grossPay, 'Taxable Gross:', taxableGross, 'Taxable Gross Pay param:', taxableGrossPay);

    // Convert pay periods to frequency string
    const payFrequency = getPayFrequencyFromPeriods(payPeriods);
    const annualGross = grossPay * payPeriods; // Use original gross for annual display
    const annualTaxableGross = taxableGross * payPeriods;

    // Calculate federal withholding (Pub 15-T) - use taxable gross
    const federalIncomeTax = await calculateFederalWithholdingPub15T(
        taxableGross,
        payFrequency,
        filingStatus,
        w4Data.step2Checkbox || false,
        w4Data,
        taxYear
    );

    // Calculate state withholding - use taxable gross
    const stateIncomeTax = await calculateStateWithholding(
        taxableGross,
        payFrequency,
        state,
        filingStatus,
        annualTaxableGross,
        taxYear
    );

    // Calculate state payroll taxes - use taxable gross
    const statePayrollTaxes = await calculateStatePayrollTaxes(
        state,
        taxableGross,
        yearToDateGross,
        taxYear
    );

    // Calculate FICA - use taxable gross
    const fica = await calculateFICA(
        taxableGross,
        employeeStatus,
        yearToDateGross,
        taxYear
    );

    // Calculate local taxes - use taxable gross
    const localTaxes = await calculateLocalTaxes(
        state,
        localJurisdiction,
        taxableGross,
        annualTaxableGross,
        payPeriods,
        taxYear
    );

    // Calculate totals
    let totalStatePayrollTaxes = 0;
    for (const taxType in statePayrollTaxes) {
        totalStatePayrollTaxes += statePayrollTaxes[taxType].amount;
    }

    let totalLocalTaxes = 0;
    for (const taxType in localTaxes) {
        totalLocalTaxes += localTaxes[taxType].amount;
    }

    const totalDeductions = totalPreTaxDeductions + federalIncomeTax + stateIncomeTax + totalStatePayrollTaxes + 
                           totalLocalTaxes + fica.socialSecurity + fica.medicare + fica.additionalMedicare;
    const netPay = grossPay - totalDeductions;

    // Update YTD
    const newYtdGross = yearToDateGross + grossPay;
    const newYtdNet = yearToDateNet + netPay;

    const resultPreTaxDeductions = preTaxDeductions || {
        advance: 0,
        medical: 0,
        miscellaneous: 0,
        total: 0
    };
    
    console.error('[PayrollService] Returning pre-tax deductions:', JSON.stringify(resultPreTaxDeductions));
    
    return {
        grossPay,
        preTaxDeductions: resultPreTaxDeductions,
        taxableGrossPay: taxableGross,
        federalIncomeTax,
        stateIncomeTax,
        statePayrollTaxes,
        localTaxes,
        socialSecurity: fica.socialSecurity,
        medicare: fica.medicare,
        additionalMedicare: fica.additionalMedicare,
        totalDeductions,
        netPay,
        ytdGross: newYtdGross,
        ytdNet: newYtdNet,
        annualGross
    };
}

async function calculateStatePayrollTaxes(state, grossPay, yearToDateGross, taxYear) {
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        db.all(`SELECT tax_type, rate, wage_base, employee_paid FROM state_payroll_taxes 
                WHERE year = ? AND state_code = ? AND employee_paid = 1`,
            [taxYear, state],
            (err, rows) => {
                if (err) {
                    reject(err);
                    return;
                }

                const taxes = {};
                for (const row of rows) {
                    let amount = 0;
                    if (row.wage_base && yearToDateGross < row.wage_base) {
                        const taxable = Math.min(grossPay, row.wage_base - yearToDateGross);
                        amount = taxable * row.rate;
                    } else if (!row.wage_base) {
                        amount = grossPay * row.rate;
                    }

                    taxes[row.tax_type] = {
                        amount: Math.round(amount * 100) / 100,
                        name: getTaxTypeName(row.tax_type)
                    };
                }

                resolve(taxes);
            }
        );
    });
}

async function calculateFICA(grossPay, employeeStatus, yearToDateGross, taxYear) {
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        db.get(`SELECT * FROM fica_rates WHERE year = ?`, [taxYear], (err, fica) => {
            if (err) {
                reject(err);
                return;
            }

            const isExempt = (employeeStatus === 'OPT'); // Simplified - add more logic as needed

            let socialSecurity = 0;
            let medicare = 0;
            let additionalMedicare = 0;

            if (!isExempt) {
                // Social Security
                if (yearToDateGross < fica.social_security_wage_base) {
                    const taxable = Math.min(grossPay, fica.social_security_wage_base - yearToDateGross);
                    socialSecurity = taxable * fica.social_security_rate;
                }

                // Medicare
                medicare = grossPay * fica.medicare_rate;

                // Additional Medicare
                const ytdAfter = yearToDateGross + grossPay;
                if (ytdAfter > fica.additional_medicare_threshold) {
                    const wagesAbove = yearToDateGross < fica.additional_medicare_threshold
                        ? ytdAfter - fica.additional_medicare_threshold
                        : grossPay;
                    additionalMedicare = wagesAbove * fica.additional_medicare_rate;
                }
            }

            resolve({
                socialSecurity: Math.round(socialSecurity * 100) / 100,
                medicare: Math.round(medicare * 100) / 100,
                additionalMedicare: Math.round(additionalMedicare * 100) / 100
            });
        });
    });
}

async function calculateLocalTaxes(state, jurisdiction, grossPay, annualGross, payPeriods, taxYear) {
    if (!jurisdiction || jurisdiction === 'NONE') {
        return {};
    }

    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        db.get(`SELECT * FROM local_taxes 
                WHERE year = ? AND state_code = ? AND jurisdiction = ?`,
            [taxYear, state, jurisdiction],
            (err, row) => {
                if (err) {
                    reject(err);
                    return;
                }

                if (!row) {
                    resolve({});
                    return;
                }

                let amount = 0;
                if (row.tax_type === 'flat') {
                    if (row.fixed_amount) {
                        amount = row.fixed_amount / payPeriods;
                    } else {
                        amount = grossPay * row.rate;
                    }
                } else if (row.tax_type === 'brackets' && row.brackets_json) {
                    // Parse brackets and calculate
                    const brackets = JSON.parse(row.brackets_json);
                    const annualTax = calculateBracketTax(annualGross, brackets);
                    amount = annualTax / payPeriods;
                }

                resolve({
                    [jurisdiction]: {
                        amount: Math.round(amount * 100) / 100,
                        name: jurisdiction
                    }
                });
            }
        );
    });
}

function calculateBracketTax(income, brackets) {
    let tax = 0;
    for (const bracket of brackets) {
        if (income > bracket.min) {
            const taxable = Math.min(income, bracket.max) - bracket.min;
            if (taxable > 0) {
                tax += taxable * bracket.rate;
            }
        }
    }
    return tax;
}

function getPayFrequencyFromPeriods(periods) {
    if (periods === 52) return 'WEEKLY';
    if (periods === 26) return 'BIWEEKLY';
    if (periods === 24) return 'SEMIMONTHLY';
    if (periods === 12) return 'MONTHLY';
    if (periods === 4) return 'QUARTERLY';
    if (periods === 1) return 'ANNUALLY';
    return 'MONTHLY';
}

function getTaxTypeName(taxType) {
    const names = {
        'UI': 'Unemployment Insurance',
        'DI': 'Disability Insurance',
        'FLI': 'Family Leave Insurance',
        'SDI': 'State Disability Insurance',
        'PFL': 'Paid Family Leave',
        'TDI': 'Temporary Disability Insurance',
        'PFML': 'Paid Family Medical Leave',
        'FAMLI': 'Family Medical Leave Insurance',
        'PLO': 'Paid Leave Oregon',
        'PL': 'Paid Leave',
        'WA_CARES_LTC': 'WA Cares Long-Term Care'
    };
    return names[taxType] || taxType;
}

module.exports = {
    calculatePaystub
};

