"""
Command-line interface for the Paystub Calculator
"""

import sys
from decimal import Decimal
from paystub_calculator import (
    PaystubCalculator,
    EmployeeStatus,
    FilingStatus
)


def get_employee_status(status_str: str) -> EmployeeStatus:
    """Convert string to EmployeeStatus enum"""
    status_map = {
        'opt': EmployeeStatus.OPT,
        'h1b': EmployeeStatus.H1B,
        'green card': EmployeeStatus.GREEN_CARD,
        'greencard': EmployeeStatus.GREEN_CARD,
        'us citizen': EmployeeStatus.US_CITIZEN,
        'citizen': EmployeeStatus.US_CITIZEN,
    }
    return status_map.get(status_str.lower(), EmployeeStatus.US_CITIZEN)


def get_filing_status(status_str: str) -> FilingStatus:
    """Convert string to FilingStatus enum"""
    status_map = {
        'single': FilingStatus.SINGLE,
        'married': FilingStatus.MARRIED_JOINTLY,
        'married jointly': FilingStatus.MARRIED_JOINTLY,
        'married separately': FilingStatus.MARRIED_SEPARATELY,
        'head of household': FilingStatus.HEAD_OF_HOUSEHOLD,
        'hoh': FilingStatus.HEAD_OF_HOUSEHOLD,
    }
    return status_map.get(status_str.lower(), FilingStatus.SINGLE)


def main():
    """Main CLI function"""
    print("=" * 60)
    print("Paystub Calculator for All 50 US States")
    print("=" * 60)
    print()
    
    calculator = PaystubCalculator()
    
    # Get user input
    try:
        gross_pay = Decimal(input("Enter gross pay per pay period: $"))
        state = input("Enter state code (2 letters, e.g., CA, NY, TX): ").upper().strip()
        
        if state not in calculator.STATE_TAX_DATA:
            print(f"Error: Invalid state code '{state}'")
            return
        
        print("\nEmployee Status Options:")
        print("  1. OPT")
        print("  2. H1B")
        print("  3. Green Card")
        print("  4. US Citizen")
        status_choice = input("Select employee status (1-4): ").strip()
        status_map = {'1': 'OPT', '2': 'H1B', '3': 'GREEN CARD', '4': 'US CITIZEN'}
        employee_status = get_employee_status(status_map.get(status_choice, '4'))
        
        is_f1_opt_first_2_years = False
        if employee_status == EmployeeStatus.OPT:
            opt_exempt = input("Is this OPT employee in first 2 years (FICA exempt)? (y/n): ").lower()
            is_f1_opt_first_2_years = opt_exempt == 'y'
        
        print("\nFiling Status Options:")
        print("  1. Single")
        print("  2. Married Filing Jointly")
        print("  3. Married Filing Separately")
        print("  4. Head of Household")
        filing_choice = input("Select filing status (1-4): ").strip()
        filing_map = {'1': 'SINGLE', '2': 'MARRIED', '3': 'MARRIED SEPARATELY', '4': 'HEAD OF HOUSEHOLD'}
        filing_status = get_filing_status(filing_map.get(filing_choice, '1'))
        
        print("\nPay Period Options:")
        print("  1. Weekly (52 periods)")
        print("  2. Bi-weekly (26 periods)")
        print("  3. Semi-monthly (24 periods)")
        print("  4. Monthly (12 periods)")
        period_choice = input("Select pay period (1-4): ").strip()
        period_map = {'1': 52, '2': 26, '3': 24, '4': 12}
        pay_periods = period_map.get(period_choice, 26)
        
        allowances = int(input("Enter number of allowances (from W-4, default 0): ") or "0")
        additional_withholding = Decimal(input("Enter additional federal withholding (default $0): $") or "0")
        
        ytd_gross = Decimal(input("Enter year-to-date gross pay (default $0): $") or "0")
        ytd_net = Decimal(input("Enter year-to-date net pay (default $0): $") or "0")
        
        # Calculate paystub
        result = calculator.calculate_paystub(
            gross_pay=gross_pay,
            state=state,
            employee_status=employee_status,
            filing_status=filing_status,
            pay_periods_per_year=pay_periods,
            allowances=allowances,
            additional_federal_withholding=additional_withholding,
            year_to_date_gross=ytd_gross,
            year_to_date_net=ytd_net,
            is_f1_opt_first_2_years=is_f1_opt_first_2_years
        )
        
        # Display results
        print("\n" + "=" * 60)
        print("PAYSTUB CALCULATION RESULTS")
        print("=" * 60)
        print(calculator.format_paystub(result))
        
        # Show breakdown
        print("\n" + "=" * 60)
        print("DETAILED BREAKDOWN")
        print("=" * 60)
        print(f"Annual Gross Pay:              ${result.gross_pay * Decimal(pay_periods):,.2f}")
        print(f"Effective Tax Rate:            {(1 - (result.net_pay / result.gross_pay)) * 100:.2f}%")
        print(f"Federal Tax Rate:              {(result.federal_income_tax / result.gross_pay) * 100:.2f}%")
        print(f"State Tax Rate:                {(result.state_income_tax / result.gross_pay) * 100:.2f}%")
        print(f"FICA Tax Rate:                 {((result.social_security_tax + result.medicare_tax + result.additional_medicare_tax) / result.gross_pay) * 100:.2f}%")
        
    except ValueError as e:
        print(f"Error: Invalid input - {e}")
    except KeyboardInterrupt:
        print("\n\nCalculation cancelled.")
    except Exception as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    main()

