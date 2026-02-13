"""
Test script for the Paystub Calculator
Tests various scenarios across different states and employee statuses
"""

from decimal import Decimal
from paystub_calculator import (
    PaystubCalculator,
    EmployeeStatus,
    FilingStatus
)


def test_all_states():
    """Test calculator with all 50 states"""
    calculator = PaystubCalculator()
    
    print("Testing Paystub Calculator for All 50 States")
    print("=" * 70)
    print()
    
    test_cases = [
        {
            'name': 'US Citizen - California',
            'gross': Decimal('5000'),
            'state': 'CA',
            'status': EmployeeStatus.US_CITIZEN,
            'filing': FilingStatus.SINGLE,
            'periods': 26
        },
        {
            'name': 'H1B - New York',
            'gross': Decimal('6000'),
            'state': 'NY',
            'status': EmployeeStatus.H1B,
            'filing': FilingStatus.SINGLE,
            'periods': 26
        },
        {
            'name': 'OPT (FICA Exempt) - Texas',
            'gross': Decimal('4500'),
            'state': 'TX',
            'status': EmployeeStatus.OPT,
            'filing': FilingStatus.SINGLE,
            'periods': 26,
            'opt_exempt': True
        },
        {
            'name': 'Green Card - Florida',
            'gross': Decimal('5500'),
            'state': 'FL',
            'status': EmployeeStatus.GREEN_CARD,
            'filing': FilingStatus.MARRIED_JOINTLY,
            'periods': 26
        },
        {
            'name': 'US Citizen - No State Tax (Nevada)',
            'gross': Decimal('5000'),
            'state': 'NV',
            'status': EmployeeStatus.US_CITIZEN,
            'filing': FilingStatus.SINGLE,
            'periods': 26
        },
        {
            'name': 'US Citizen - Flat Tax State (Colorado)',
            'gross': Decimal('5000'),
            'state': 'CO',
            'status': EmployeeStatus.US_CITIZEN,
            'filing': FilingStatus.SINGLE,
            'periods': 26
        },
    ]
    
    for i, test in enumerate(test_cases, 1):
        print(f"Test {i}: {test['name']}")
        print("-" * 70)
        
        result = calculator.calculate_paystub(
            gross_pay=test['gross'],
            state=test['state'],
            employee_status=test['status'],
            filing_status=test['filing'],
            pay_periods_per_year=test['periods'],
            is_f1_opt_first_2_years=test.get('opt_exempt', False)
        )
        
        print(f"State: {test['state']}")
        print(f"Gross Pay: ${result.gross_pay:,.2f}")
        print(f"Federal Tax: ${result.federal_income_tax:,.2f}")
        print(f"State Tax: ${result.state_income_tax:,.2f}")
        print(f"Social Security: ${result.social_security_tax:,.2f}")
        print(f"Medicare: ${result.medicare_tax:,.2f}")
        if result.additional_medicare_tax > 0:
            print(f"Additional Medicare: ${result.additional_medicare_tax:,.2f}")
        print(f"Net Pay: ${result.net_pay:,.2f}")
        print(f"Effective Tax Rate: {(1 - (result.net_pay / result.gross_pay)) * 100:.2f}%")
        print()
    
    # Test all states
    print("=" * 70)
    print("Testing All 50 States + DC (Quick Summary)")
    print("=" * 70)
    print()
    
    states = sorted(calculator.STATE_TAX_DATA.keys())
    test_gross = Decimal('5000')
    
    print(f"{'State':<5} {'State Tax':<12} {'Net Pay':<12} {'Tax Rate':<10}")
    print("-" * 45)
    
    for state in states:
        result = calculator.calculate_paystub(
            gross_pay=test_gross,
            state=state,
            employee_status=EmployeeStatus.US_CITIZEN,
            filing_status=FilingStatus.SINGLE,
            pay_periods_per_year=26
        )
        tax_rate = (1 - (result.net_pay / result.gross_pay)) * 100
        state_tax_str = "No Tax" if result.state_income_tax == 0 else f"${result.state_income_tax:,.2f}"
        print(f"{state:<5} {state_tax_str:<12} ${result.net_pay:,.2f}  {tax_rate:>6.2f}%")


def test_employee_statuses():
    """Test different employee statuses"""
    calculator = PaystubCalculator()
    
    print("\n" + "=" * 70)
    print("Testing Different Employee Statuses (Same Gross Pay)")
    print("=" * 70)
    print()
    
    gross = Decimal('5000')
    state = 'CA'
    
    statuses = [
        (EmployeeStatus.US_CITIZEN, False, "US Citizen"),
        (EmployeeStatus.GREEN_CARD, False, "Green Card"),
        (EmployeeStatus.H1B, False, "H1B"),
        (EmployeeStatus.OPT, False, "OPT (FICA Subject)"),
        (EmployeeStatus.OPT, True, "OPT (FICA Exempt)"),
    ]
    
    print(f"{'Status':<25} {'FICA':<15} {'Net Pay':<12} {'Tax Rate':<10}")
    print("-" * 65)
    
    for status, opt_exempt, name in statuses:
        result = calculator.calculate_paystub(
            gross_pay=gross,
            state=state,
            employee_status=status,
            filing_status=FilingStatus.SINGLE,
            pay_periods_per_year=26,
            is_f1_opt_first_2_years=opt_exempt
        )
        fica_total = result.social_security_tax + result.medicare_tax
        fica_str = f"${fica_total:,.2f}" if fica_total > 0 else "Exempt"
        tax_rate = (1 - (result.net_pay / result.gross_pay)) * 100
        print(f"{name:<25} {fica_str:<15} ${result.net_pay:,.2f}  {tax_rate:>6.2f}%")


if __name__ == "__main__":
    test_all_states()
    test_employee_statuses()
    print("\n" + "=" * 70)
    print("All tests completed!")
    print("=" * 70)

