# Paystub Calculator for All 50 US States

A comprehensive Python-based paystub calculator that supports all 50 US states with state-specific tax rates and deductions. The calculator handles different employee immigration statuses (OPT, H1B, Green Card, US Citizen) and uses IRS tax tables for accurate calculations.

## Features

- ✅ **All 50 States**: Complete tax calculations for all US states including DC
- ✅ **IRS Tax Tables**: Uses 2024 federal tax brackets and standard deductions
- ✅ **State-Specific Taxes**: Handles flat-rate and progressive bracket states
- ✅ **Employee Status Support**: 
  - OPT (F-1 visa holders with FICA exemption for first 2 years)
  - H1B (subject to FICA taxes)
  - Green Card holders (full tax treatment)
  - US Citizens (full tax treatment)
- ✅ **FICA Calculations**: Social Security (6.2% up to wage base) and Medicare (1.45% + 0.9% additional for high earners)
- ✅ **Filing Status**: Supports Single, Married Filing Jointly, Married Filing Separately, Head of Household
- ✅ **Year-to-Date Tracking**: Maintains YTD gross and net pay

## Installation

No external dependencies required! Uses only Python standard library.

```bash
# Just ensure you have Python 3.6+
python --version
```

## Usage

### Basic Example

```python
from paystub_calculator import PaystubCalculator, EmployeeStatus, FilingStatus
from decimal import Decimal

calculator = PaystubCalculator()

# Calculate paystub for a US Citizen in California
result = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),  # $5,000 per pay period
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,  # Bi-weekly
    allowances=1
)

# Print formatted paystub
print(calculator.format_paystub(result, "John Doe", "Bi-weekly"))
```

### OPT Employee (FICA Exempt)

```python
# OPT employees in first 2 years are exempt from FICA taxes
result = calculator.calculate_paystub(
    gross_pay=Decimal('4500'),
    state='TX',  # No state tax
    employee_status=EmployeeStatus.OPT,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,
    is_f1_opt_first_2_years=True  # FICA exempt
)
```

### H1B Employee

```python
# H1B employees are subject to FICA taxes (unless tax treaty applies)
result = calculator.calculate_paystub(
    gross_pay=Decimal('6000'),
    state='NY',
    employee_status=EmployeeStatus.H1B,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,
    allowances=0
)
```

### Green Card Holder

```python
# Green Card holders are treated as US residents for tax purposes
result = calculator.calculate_paystub(
    gross_pay=Decimal('5500'),
    state='FL',  # No state tax
    employee_status=EmployeeStatus.GREEN_CARD,
    filing_status=FilingStatus.MARRIED_JOINTLY,
    pay_periods_per_year=26,
    allowances=2
)
```

## State Tax Information

### States with No Income Tax
- Alaska (AK)
- Florida (FL)
- Nevada (NV)
- New Hampshire (NH) - only on interest/dividends
- South Dakota (SD)
- Tennessee (TN)
- Texas (TX)
- Washington (WA)
- Wyoming (WY)

### States with Flat Tax Rates
- Arizona (AZ): 2.5%
- Colorado (CO): 4.4%
- Idaho (ID): 5.8%
- Illinois (IL): 4.95%
- Indiana (IN): 3.15%
- Kentucky (KY): 4.5%
- Massachusetts (MA): 5%
- Michigan (MI): 4.25%
- North Carolina (NC): 4.75%
- Pennsylvania (PA): 3.07%
- Utah (UT): 4.85%

### States with Progressive Tax Brackets
All other states use progressive tax brackets with varying rates and standard deductions.

## Tax Rates (2024)

### Federal Tax Brackets (Single)
- 10%: $0 - $11,600
- 12%: $11,600 - $47,150
- 22%: $47,150 - $100,525
- 24%: $100,525 - $191,950
- 32%: $191,950 - $243,725
- 35%: $243,725 - $609,350
- 37%: Over $609,350

### Standard Deductions (2024)
- Single: $14,600
- Married Filing Jointly: $29,200
- Married Filing Separately: $14,600
- Head of Household: $21,900

### FICA Taxes
- Social Security: 6.2% (up to $168,600 wage base in 2024)
- Medicare: 1.45% (all wages)
- Additional Medicare: 0.9% (wages over $200,000)

## Employee Status Tax Treatment

### OPT (F-1 Visa)
- **FICA Exempt**: First 2 years on OPT are exempt from Social Security and Medicare taxes
- **Federal/State Tax**: Subject to federal and state income taxes
- Set `is_f1_opt_first_2_years=True` to apply FICA exemption

### H1B
- **FICA Subject**: Generally subject to Social Security and Medicare taxes
- **Federal/State Tax**: Subject to federal and state income taxes
- Note: Some tax treaties may provide exemptions (not automatically handled)

### Green Card
- **Full Tax Treatment**: Same as US Citizens
- Subject to all federal, state, and FICA taxes

### US Citizen
- **Full Tax Treatment**: Standard tax calculations apply
- Subject to all federal, state, and FICA taxes

## API Reference

### `PaystubCalculator.calculate_paystub()`

Calculate a complete paystub.

**Parameters:**
- `gross_pay` (Decimal): Gross pay for this pay period
- `state` (str): Two-letter state code (e.g., 'CA', 'NY', 'TX')
- `employee_status` (EmployeeStatus): OPT, H1B, GREEN_CARD, or US_CITIZEN
- `filing_status` (FilingStatus): SINGLE, MARRIED_JOINTLY, MARRIED_SEPARATELY, or HEAD_OF_HOUSEHOLD
- `pay_periods_per_year` (int): Number of pay periods (26=bi-weekly, 24=semi-monthly, 12=monthly)
- `allowances` (int): Number of allowances from W-4 (default: 0)
- `additional_federal_withholding` (Decimal): Additional federal withholding (default: 0)
- `year_to_date_gross` (Decimal): YTD gross pay (default: 0)
- `year_to_date_net` (Decimal): YTD net pay (default: 0)
- `is_f1_opt_first_2_years` (bool): Whether OPT employee is FICA exempt (default: False)

**Returns:** `PaystubResult` object with all calculated values

### `PaystubResult`

Data class containing:
- `gross_pay`: Gross pay for period
- `federal_income_tax`: Federal income tax withheld
- `state_income_tax`: State income tax withheld
- `social_security_tax`: Social Security tax (FICA)
- `medicare_tax`: Medicare tax (FICA)
- `additional_medicare_tax`: Additional Medicare tax (if applicable)
- `net_pay`: Net pay after all deductions
- `year_to_date_gross`: Updated YTD gross pay
- `year_to_date_net`: Updated YTD net pay

## Running Examples

```bash
python paystub_calculator.py
```

This will run example calculations for different employee types and states.

## Important Notes

1. **Tax Year**: This calculator uses 2024 tax rates and brackets. Update annually for accuracy.
2. **FICA Exemptions**: OPT employees are only FICA exempt for the first 2 years. After that, they become subject to FICA taxes.
3. **Tax Treaties**: Some H1B holders may have tax treaty benefits. This calculator does not automatically apply treaty exemptions.
4. **Local Taxes**: Some cities/counties have additional local income taxes (not included).
5. **Accuracy**: While based on official IRS and state tax tables, always verify with a tax professional for critical applications.

## License

This calculator is provided as-is for educational and business use. Always consult with a tax professional for official tax advice.

