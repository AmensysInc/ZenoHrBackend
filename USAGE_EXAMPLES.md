# Usage Examples

## Basic Usage

### Python Script
```python
from paystub_calculator import PaystubCalculator, EmployeeStatus, FilingStatus
from decimal import Decimal

# Initialize calculator
calculator = PaystubCalculator()

# Calculate paystub
result = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),  # $5,000 per pay period
    state='CA',                  # California
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,     # Bi-weekly
    allowances=1
)

# Display results
print(calculator.format_paystub(result, "John Doe", "Bi-weekly"))
```

## Different Employee Statuses

### OPT Employee (FICA Exempt - First 2 Years)
```python
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
result = calculator.calculate_paystub(
    gross_pay=Decimal('6000'),
    state='NY',
    employee_status=EmployeeStatus.H1B,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

### Green Card Holder
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('5500'),
    state='FL',  # No state tax
    employee_status=EmployeeStatus.GREEN_CARD,
    filing_status=FilingStatus.MARRIED_JOINTLY,
    pay_periods_per_year=26,
    allowances=2
)
```

## Different States

### High Tax State (California)
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CA',  # Progressive brackets, high rates
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

### No State Tax (Texas)
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='TX',  # No state income tax
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

### Flat Tax State (Colorado)
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CO',  # 4.4% flat rate
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

## Different Pay Periods

### Weekly
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('2000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=52  # Weekly
)
```

### Semi-Monthly
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('5416.67'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=24  # Semi-monthly
)
```

### Monthly
```python
result = calculator.calculate_paystub(
    gross_pay=Decimal('13000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=12  # Monthly
)
```

## Year-to-Date Tracking

```python
# First pay period
result1 = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)

# Second pay period (with YTD)
result2 = calculator.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,
    year_to_date_gross=result1.year_to_date_gross,
    year_to_date_net=result1.year_to_date_net
)
```

## Accessing Results

### As Dictionary
```python
result = calculator.calculate_paystub(...)
data = result.to_dict()

print(f"Net Pay: ${data['net_pay']:,.2f}")
print(f"Federal Tax: ${data['federal_income_tax']:,.2f}")
```

### Individual Fields
```python
result = calculator.calculate_paystub(...)

print(f"Gross Pay: ${result.gross_pay:,.2f}")
print(f"Federal Tax: ${result.federal_income_tax:,.2f}")
print(f"State Tax: ${result.state_income_tax:,.2f}")
print(f"Social Security: ${result.social_security_tax:,.2f}")
print(f"Medicare: ${result.medicare_tax:,.2f}")
print(f"Net Pay: ${result.net_pay:,.2f}")
```

## Calculate Tax Rates

```python
result = calculator.calculate_paystub(...)

# Effective tax rate
effective_rate = (1 - (result.net_pay / result.gross_pay)) * 100

# Federal tax rate
federal_rate = (result.federal_income_tax / result.gross_pay) * 100

# State tax rate
state_rate = (result.state_income_tax / result.gross_pay) * 100

# FICA tax rate
fica_rate = (
    (result.social_security_tax + result.medicare_tax) / result.gross_pay
) * 100

print(f"Effective Tax Rate: {effective_rate:.2f}%")
print(f"Federal Tax Rate: {federal_rate:.2f}%")
print(f"State Tax Rate: {state_rate:.2f}%")
print(f"FICA Tax Rate: {fica_rate:.2f}%")
```

## Batch Processing

```python
employees = [
    {
        'name': 'John Doe',
        'gross': 5000,
        'state': 'CA',
        'status': EmployeeStatus.US_CITIZEN,
        'filing': FilingStatus.SINGLE
    },
    {
        'name': 'Jane Smith',
        'gross': 6000,
        'state': 'NY',
        'status': EmployeeStatus.H1B,
        'filing': FilingStatus.SINGLE
    },
    {
        'name': 'Ali Khan',
        'gross': 4500,
        'state': 'TX',
        'status': EmployeeStatus.OPT,
        'filing': FilingStatus.SINGLE,
        'opt_exempt': True
    }
]

for emp in employees:
    result = calculator.calculate_paystub(
        gross_pay=Decimal(str(emp['gross'])),
        state=emp['state'],
        employee_status=emp['status'],
        filing_status=emp['filing'],
        pay_periods_per_year=26,
        is_f1_opt_first_2_years=emp.get('opt_exempt', False)
    )
    
    print(f"\n{emp['name']}:")
    print(f"  Gross: ${result.gross_pay:,.2f}")
    print(f"  Net: ${result.net_pay:,.2f}")
    print(f"  Tax Rate: {(1 - (result.net_pay / result.gross_pay)) * 100:.2f}%")
```

## JSON Export

```python
from export_json import calculate_and_export_json, export_to_json_file

# Calculate and get JSON
result = calculate_and_export_json(
    gross_pay=5000.0,
    state='CA',
    employee_status='US_CITIZEN',
    filing_status='SINGLE',
    pay_periods_per_year=26
)

# Export to file
export_to_json_file(result, "paystub.json")

# Or use the dictionary directly
import json
print(json.dumps(result, indent=2))
```

## Command Line Interface

Run the interactive CLI:
```bash
python cli.py
```

This will prompt you for:
- Gross pay
- State code
- Employee status
- Filing status
- Pay period frequency
- Allowances
- Additional withholding
- Year-to-date information

## Testing

Run the test suite:
```bash
python test_calculator.py
```

This will:
- Test various employee statuses
- Test different states
- Show a summary of all 50 states + DC
- Compare tax rates across different scenarios

