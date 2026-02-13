# Quick Reference Guide

## State Tax Summary

### No State Income Tax (9 states)
- **AK** - Alaska
- **FL** - Florida
- **NV** - Nevada
- **NH** - New Hampshire (only on interest/dividends)
- **SD** - South Dakota
- **TN** - Tennessee
- **TX** - Texas
- **WA** - Washington
- **WY** - Wyoming

### Flat Tax Rate States (11 states)
| State | Rate | State | Rate |
|-------|------|-------|------|
| AZ | 2.5% | IN | 3.15% |
| CO | 4.4% | KY | 4.5% |
| ID | 5.8% | MA | 5.0% |
| IL | 4.95% | MI | 4.25% |
| NC | 4.75% | PA | 3.07% |
| UT | 4.85% | | |

### Progressive Tax States (30 states + DC)
All other states use progressive tax brackets with varying rates.

## Employee Status Quick Guide

| Status | FICA Taxes | Federal Tax | State Tax | Notes |
|--------|------------|-------------|-----------|-------|
| **US Citizen** | Yes | Yes | Yes | Full tax treatment |
| **Green Card** | Yes | Yes | Yes | Same as US Citizen |
| **H1B** | Yes* | Yes | Yes | *Subject to FICA unless tax treaty |
| **OPT (Year 1-2)** | No | Yes | Yes | FICA exempt for first 2 years |
| **OPT (Year 3+)** | Yes | Yes | Yes | FICA applies after 2 years |

## Common Pay Periods

| Period | Pay Periods/Year | Description |
|--------|------------------|-------------|
| Weekly | 52 | Every week |
| Bi-weekly | 26 | Every 2 weeks (most common) |
| Semi-monthly | 24 | Twice per month |
| Monthly | 12 | Once per month |

## 2024 Federal Tax Brackets (Single)

| Bracket | Rate | Income Range |
|---------|------|--------------|
| 10% | $0 - $11,600 |
| 12% | $11,600 - $47,150 |
| 22% | $47,150 - $100,525 |
| 24% | $100,525 - $191,950 |
| 32% | $191,950 - $243,725 |
| 35% | $243,725 - $609,350 |
| 37% | Over $609,350 |

## 2024 Standard Deductions

| Filing Status | Deduction |
|---------------|-----------|
| Single | $14,600 |
| Married Filing Jointly | $29,200 |
| Married Filing Separately | $14,600 |
| Head of Household | $21,900 |

## FICA Tax Rates (2024)

| Tax | Rate | Wage Base |
|-----|------|-----------|
| Social Security | 6.2% | $168,600 |
| Medicare | 1.45% | No limit |
| Additional Medicare | 0.9% | Over $200,000 |

## Quick Calculation Examples

### Example 1: US Citizen in California
```python
from paystub_calculator import PaystubCalculator, EmployeeStatus, FilingStatus
from decimal import Decimal

calc = PaystubCalculator()
result = calc.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

### Example 2: OPT Employee (FICA Exempt)
```python
result = calc.calculate_paystub(
    gross_pay=Decimal('4500'),
    state='TX',
    employee_status=EmployeeStatus.OPT,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26,
    is_f1_opt_first_2_years=True  # FICA exempt
)
```

### Example 3: H1B in New York
```python
result = calc.calculate_paystub(
    gross_pay=Decimal('6000'),
    state='NY',
    employee_status=EmployeeStatus.H1B,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)
```

## State Codes Reference

All 50 states + DC:
AL, AK, AZ, AR, CA, CO, CT, DE, FL, GA, HI, ID, IL, IN, IA, KS, KY, LA, ME, MD, MA, MI, MN, MS, MO, MT, NE, NV, NH, NJ, NM, NY, NC, ND, OH, OK, OR, PA, RI, SC, SD, TN, TX, UT, VT, VA, WA, WV, WI, WY, DC

## Common Use Cases

### Calculate for Multiple Employees
```python
employees = [
    {'name': 'John', 'gross': 5000, 'state': 'CA', 'status': EmployeeStatus.US_CITIZEN},
    {'name': 'Jane', 'gross': 6000, 'state': 'NY', 'status': EmployeeStatus.H1B},
    # ... more employees
]

for emp in employees:
    result = calc.calculate_paystub(
        gross_pay=Decimal(str(emp['gross'])),
        state=emp['state'],
        employee_status=emp['status'],
        filing_status=FilingStatus.SINGLE,
        pay_periods_per_year=26
    )
    print(f"{emp['name']}: ${result.net_pay:,.2f}")
```

### Calculate Annual Tax Burden
```python
result = calc.calculate_paystub(
    gross_pay=Decimal('5000'),
    state='CA',
    employee_status=EmployeeStatus.US_CITIZEN,
    filing_status=FilingStatus.SINGLE,
    pay_periods_per_year=26
)

annual_tax = (
    result.federal_income_tax +
    result.state_income_tax +
    result.social_security_tax +
    result.medicare_tax
) * 26

print(f"Annual tax burden: ${annual_tax:,.2f}")
```

## Tips

1. **OPT Employees**: Remember to set `is_f1_opt_first_2_years=True` only for the first 2 years
2. **High Earners**: Additional Medicare tax (0.9%) applies to wages over $200,000
3. **Social Security**: Stops at $168,600 wage base (2024)
4. **State Taxes**: Some cities have additional local taxes (not included)
5. **Tax Treaties**: H1B holders may have treaty benefits (not automatically applied)

## Need Help?

- Check `README.md` for detailed documentation
- Run `python test_calculator.py` to see examples
- Use `python cli.py` for interactive calculation
- Use `export_json.py` for API integration

