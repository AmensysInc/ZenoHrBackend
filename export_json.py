"""
JSON export utility for paystub calculations
Useful for API integration or data export
"""

import json
from decimal import Decimal
from paystub_calculator import (
    PaystubCalculator,
    EmployeeStatus,
    FilingStatus
)


def calculate_and_export_json(
    gross_pay: float,
    state: str,
    employee_status: str,
    filing_status: str = "single",
    pay_periods_per_year: int = 26,
    allowances: int = 0,
    additional_withholding: float = 0.0,
    ytd_gross: float = 0.0,
    ytd_net: float = 0.0,
    is_f1_opt_first_2_years: bool = False
) -> dict:
    """
    Calculate paystub and return as JSON-serializable dictionary
    
    Args:
        gross_pay: Gross pay per pay period
        state: Two-letter state code
        employee_status: "OPT", "H1B", "GREEN_CARD", or "US_CITIZEN"
        filing_status: "SINGLE", "MARRIED_JOINTLY", "MARRIED_SEPARATELY", or "HEAD_OF_HOUSEHOLD"
        pay_periods_per_year: Number of pay periods (26=bi-weekly, 24=semi-monthly, 12=monthly)
        allowances: Number of allowances from W-4
        additional_withholding: Additional federal withholding
        ytd_gross: Year-to-date gross pay
        ytd_net: Year-to-date net pay
        is_f1_opt_first_2_years: Whether OPT employee is FICA exempt
    
    Returns:
        Dictionary with all paystub information
    """
    calculator = PaystubCalculator()
    
    # Convert employee status
    status_map = {
        'OPT': EmployeeStatus.OPT,
        'H1B': EmployeeStatus.H1B,
        'GREEN_CARD': EmployeeStatus.GREEN_CARD,
        'US_CITIZEN': EmployeeStatus.US_CITIZEN,
    }
    emp_status = status_map.get(employee_status.upper(), EmployeeStatus.US_CITIZEN)
    
    # Convert filing status
    filing_map = {
        'SINGLE': FilingStatus.SINGLE,
        'MARRIED_JOINTLY': FilingStatus.MARRIED_JOINTLY,
        'MARRIED_SEPARATELY': FilingStatus.MARRIED_SEPARATELY,
        'HEAD_OF_HOUSEHOLD': FilingStatus.HEAD_OF_HOUSEHOLD,
    }
    file_status = filing_map.get(filing_status.upper(), FilingStatus.SINGLE)
    
    # Calculate paystub
    result = calculator.calculate_paystub(
        gross_pay=Decimal(str(gross_pay)),
        state=state.upper(),
        employee_status=emp_status,
        filing_status=file_status,
        pay_periods_per_year=pay_periods_per_year,
        allowances=allowances,
        additional_federal_withholding=Decimal(str(additional_withholding)),
        year_to_date_gross=Decimal(str(ytd_gross)),
        year_to_date_net=Decimal(str(ytd_net)),
        is_f1_opt_first_2_years=is_f1_opt_first_2_years
    )
    
    # Build result dictionary
    output = {
        'input': {
            'gross_pay': gross_pay,
            'state': state.upper(),
            'employee_status': employee_status.upper(),
            'filing_status': filing_status.upper(),
            'pay_periods_per_year': pay_periods_per_year,
            'allowances': allowances,
            'additional_withholding': additional_withholding,
            'year_to_date_gross': ytd_gross,
            'year_to_date_net': ytd_net,
            'is_f1_opt_first_2_years': is_f1_opt_first_2_years,
        },
        'paystub': result.to_dict(),
        'summary': {
            'annual_gross_pay': float(result.gross_pay * Decimal(pay_periods_per_year)),
            'total_deductions': float(
                result.federal_income_tax +
                result.state_income_tax +
                result.social_security_tax +
                result.medicare_tax +
                result.additional_medicare_tax
            ),
            'effective_tax_rate': float((1 - (result.net_pay / result.gross_pay)) * 100),
            'federal_tax_rate': float((result.federal_income_tax / result.gross_pay) * 100),
            'state_tax_rate': float((result.state_income_tax / result.gross_pay) * 100),
            'fica_tax_rate': float(
                ((result.social_security_tax + result.medicare_tax + result.additional_medicare_tax) / result.gross_pay) * 100
            ),
        }
    }
    
    return output


def export_to_json_file(output_dict: dict, filename: str = "paystub.json"):
    """Export paystub result to JSON file"""
    with open(filename, 'w') as f:
        json.dump(output_dict, f, indent=2)
    print(f"Paystub exported to {filename}")


# Example usage
if __name__ == "__main__":
    # Example: Calculate and export paystub
    result = calculate_and_export_json(
        gross_pay=5000.0,
        state='CA',
        employee_status='US_CITIZEN',
        filing_status='SINGLE',
        pay_periods_per_year=26,
        allowances=1
    )
    
    # Print JSON
    print(json.dumps(result, indent=2))
    
    # Export to file
    export_to_json_file(result, "example_paystub.json")

