"""
Comprehensive Paystub Calculator for All 50 US States
Supports: OPT, H1B, Green Card, and US Citizen employees
Uses IRS tax tables and state-specific tax rates
"""

from enum import Enum
from dataclasses import dataclass
from typing import Dict, List, Tuple, Optional
from decimal import Decimal, ROUND_HALF_UP


class EmployeeStatus(Enum):
    """Employee immigration/status types"""
    OPT = "OPT"
    H1B = "H1B"
    GREEN_CARD = "Green Card"
    US_CITIZEN = "US Citizen"


class FilingStatus(Enum):
    """Tax filing status"""
    SINGLE = "Single"
    MARRIED_JOINTLY = "Married Filing Jointly"
    MARRIED_SEPARATELY = "Married Filing Separately"
    HEAD_OF_HOUSEHOLD = "Head of Household"


@dataclass
class PaystubResult:
    """Paystub calculation result"""
    gross_pay: Decimal
    federal_income_tax: Decimal
    state_income_tax: Decimal
    social_security_tax: Decimal
    medicare_tax: Decimal
    additional_medicare_tax: Decimal
    net_pay: Decimal
    year_to_date_gross: Decimal
    year_to_date_net: Decimal
    
    def to_dict(self):
        return {
            'gross_pay': float(self.gross_pay),
            'federal_income_tax': float(self.federal_income_tax),
            'state_income_tax': float(self.state_income_tax),
            'social_security_tax': float(self.social_security_tax),
            'medicare_tax': float(self.medicare_tax),
            'additional_medicare_tax': float(self.additional_medicare_tax),
            'net_pay': float(self.net_pay),
            'year_to_date_gross': float(self.year_to_date_gross),
            'year_to_date_net': float(self.year_to_date_net)
        }


class PaystubCalculator:
    """Main paystub calculator class"""
    
    # 2024 Federal Tax Brackets (Single)
    FEDERAL_TAX_BRACKETS_2024 = [
        (0, 0.10),          # 10% up to $11,600
        (11600, 0.12),      # 12% $11,600 - $47,150
        (47150, 0.22),      # 22% $47,150 - $100,525
        (100525, 0.24),     # 24% $100,525 - $191,950
        (191950, 0.32),     # 32% $191,950 - $243,725
        (243725, 0.35),     # 35% $243,725 - $609,350
        (609350, 0.37),     # 37% over $609,350
    ]
    
    # 2024 Standard Deductions
    STANDARD_DEDUCTIONS_2024 = {
        FilingStatus.SINGLE: 14600,
        FilingStatus.MARRIED_JOINTLY: 29200,
        FilingStatus.MARRIED_SEPARATELY: 14600,
        FilingStatus.HEAD_OF_HOUSEHOLD: 21900,
    }
    
    # FICA Tax Rates
    SOCIAL_SECURITY_RATE = 0.062  # 6.2%
    SOCIAL_SECURITY_WAGE_BASE = 168600  # 2024 wage base
    MEDICARE_RATE = 0.0145  # 1.45%
    ADDITIONAL_MEDICARE_RATE = 0.009  # 0.9% on wages over $200,000
    ADDITIONAL_MEDICARE_THRESHOLD = 200000
    
    # State Tax Data - 2024 rates and brackets
    STATE_TAX_DATA = {
        'AL': {'type': 'brackets', 'brackets': [(0, 0.02), (500, 0.04), (3000, 0.05)], 'standard_deduction': 2500},
        'AK': {'type': 'none'},  # No state income tax
        'AZ': {'type': 'flat', 'rate': 0.025},  # 2.5% flat rate
        'AR': {'type': 'brackets', 'brackets': [(0, 0.02), (4400, 0.04), (8800, 0.047)], 'standard_deduction': 2200},
        'CA': {'type': 'brackets', 'brackets': [(0, 0.01), (10099, 0.02), (23942, 0.04), (37788, 0.06), (52455, 0.08), (66295, 0.093), (338639, 0.103), (406364, 0.113), (677275, 0.123)], 'standard_deduction': 5202},
        'CO': {'type': 'flat', 'rate': 0.044},  # 4.4% flat rate
        'CT': {'type': 'brackets', 'brackets': [(0, 0.03), (10000, 0.05), (50000, 0.055), (100000, 0.06), (200000, 0.065), (250000, 0.069), (500000, 0.0699)], 'standard_deduction': 0},
        'DE': {'type': 'brackets', 'brackets': [(0, 0.022), (2000, 0.039), (5000, 0.048), (10000, 0.052), (20000, 0.0555), (25000, 0.066)], 'standard_deduction': 3250},
        'FL': {'type': 'none'},  # No state income tax
        'GA': {'type': 'brackets', 'brackets': [(0, 0.01), (750, 0.02), (2250, 0.03), (3750, 0.04), (5250, 0.05), (7000, 0.0575)], 'standard_deduction': 5400},
        'HI': {'type': 'brackets', 'brackets': [(0, 0.014), (2400, 0.032), (4800, 0.055), (9600, 0.064), (14400, 0.068), (19200, 0.072), (24000, 0.076), (36000, 0.079), (48000, 0.0825), (150000, 0.10), (175000, 0.11), (200000, 0.12)], 'standard_deduction': 2200},
        'ID': {'type': 'flat', 'rate': 0.058},  # 5.8% flat rate
        'IL': {'type': 'flat', 'rate': 0.0495},  # 4.95% flat rate
        'IN': {'type': 'flat', 'rate': 0.0315},  # 3.15% flat rate
        'IA': {'type': 'brackets', 'brackets': [(0, 0.0333), (6000, 0.0067), (30000, 0.0225), (75000, 0.0414), (150000, 0.0563)], 'standard_deduction': 2100},
        'KS': {'type': 'brackets', 'brackets': [(0, 0.031), (15000, 0.0525), (30000, 0.057)], 'standard_deduction': 3500},
        'KY': {'type': 'flat', 'rate': 0.045},  # 4.5% flat rate
        'LA': {'type': 'brackets', 'brackets': [(0, 0.0185), (12500, 0.035), (50000, 0.0425)], 'standard_deduction': 4500},
        'ME': {'type': 'brackets', 'brackets': [(0, 0.058), (24500, 0.0675), (58050, 0.0715)], 'standard_deduction': 13850},
        'MD': {'type': 'brackets', 'brackets': [(0, 0.02), (1000, 0.03), (2000, 0.04), (3000, 0.0475), (100000, 0.05), (125000, 0.0525), (150000, 0.055), (250000, 0.0575)], 'standard_deduction': 2400},
        'MA': {'type': 'flat', 'rate': 0.05},  # 5% flat rate
        'MI': {'type': 'flat', 'rate': 0.0425},  # 4.25% flat rate
        'MN': {'type': 'brackets', 'brackets': [(0, 0.0535), (30410, 0.068), (100220, 0.0785), (198970, 0.0985)], 'standard_deduction': 14025},
        'MS': {'type': 'brackets', 'brackets': [(0, 0.00), (5000, 0.03), (10000, 0.04), (5000, 0.05)], 'standard_deduction': 2300},
        'MO': {'type': 'brackets', 'brackets': [(0, 0.015), (1121, 0.02), (2242, 0.025), (3363, 0.03), (4484, 0.035), (5605, 0.04), (6726, 0.045), (7847, 0.05), (8968, 0.0525), (10089, 0.055), (11210, 0.0575)], 'standard_deduction': 12950},
        'MT': {'type': 'brackets', 'brackets': [(0, 0.01), (2100, 0.02), (3600, 0.03), (5400, 0.04), (7200, 0.05), (9300, 0.06), (12000, 0.0675), (15400, 0.069)], 'standard_deduction': 5400},
        'NE': {'type': 'brackets', 'brackets': [(0, 0.0246), (3700, 0.0351), (22170, 0.0501), (35730, 0.0684)], 'standard_deduction': 7900},
        'NV': {'type': 'none'},  # No state income tax
        'NH': {'type': 'none'},  # No state income tax (only on interest/dividends)
        'NJ': {'type': 'brackets', 'brackets': [(0, 0.014), (20000, 0.0175), (35000, 0.035), (40000, 0.05525), (75000, 0.0637), (500000, 0.0897), (1000000, 0.1075)], 'standard_deduction': 0},
        'NM': {'type': 'brackets', 'brackets': [(0, 0.017), (5500, 0.032), (11000, 0.047), (16000, 0.049), (210000, 0.059)], 'standard_deduction': 13850},
        'NY': {'type': 'brackets', 'brackets': [(0, 0.04), (8500, 0.045), (11700, 0.0525), (13900, 0.055), (21400, 0.06), (80650, 0.0625), (215400, 0.0685), (1077550, 0.0965), (5000000, 0.103), (25000000, 0.109)], 'standard_deduction': 8000},
        'NC': {'type': 'flat', 'rate': 0.0475},  # 4.75% flat rate
        'ND': {'type': 'brackets', 'brackets': [(0, 0.011), (44825, 0.0204), (216675, 0.0227), (458350, 0.0251)], 'standard_deduction': 14600},
        'OH': {'type': 'brackets', 'brackets': [(0, 0.00), (26050, 0.0275), (41700, 0.0325), (83350, 0.0375), (104250, 0.0425), (208500, 0.0475)], 'standard_deduction': 0},
        'OK': {'type': 'brackets', 'brackets': [(0, 0.0025), (1000, 0.0075), (2500, 0.0175), (3750, 0.0275), (4900, 0.0375), (7200, 0.0475)], 'standard_deduction': 6350},
        'OR': {'type': 'brackets', 'brackets': [(0, 0.0475), (4050, 0.0675), (10200, 0.0875), (125000, 0.099)], 'standard_deduction': 2460},
        'PA': {'type': 'flat', 'rate': 0.0307},  # 3.07% flat rate
        'RI': {'type': 'brackets', 'brackets': [(0, 0.0375), (68200, 0.0475), (155050, 0.0599)], 'standard_deduction': 10000},
        'SC': {'type': 'brackets', 'brackets': [(0, 0.00), (3200, 0.03), (6410, 0.04), (9620, 0.05), (12820, 0.06), (16040, 0.07)], 'standard_deduction': 13850},
        'SD': {'type': 'none'},  # No state income tax
        'TN': {'type': 'none'},  # No state income tax
        'TX': {'type': 'none'},  # No state income tax
        'UT': {'type': 'flat', 'rate': 0.0485},  # 4.85% flat rate
        'VT': {'type': 'brackets', 'brackets': [(0, 0.0335), (45200, 0.066), (109450, 0.076), (232950, 0.0875)], 'standard_deduction': 6500},
        'VA': {'type': 'brackets', 'brackets': [(0, 0.02), (3000, 0.03), (5000, 0.05), (17000, 0.0575)], 'standard_deduction': 8000},
        'WA': {'type': 'none'},  # No state income tax
        'WV': {'type': 'brackets', 'brackets': [(0, 0.03), (10000, 0.04), (25000, 0.045), (40000, 0.06), (60000, 0.065)], 'standard_deduction': 0},
        'WI': {'type': 'brackets', 'brackets': [(0, 0.0354), (14020, 0.0405), (28040, 0.0535), (308910, 0.0765)], 'standard_deduction': 12760},
        'WY': {'type': 'none'},  # No state income tax
        'DC': {'type': 'brackets', 'brackets': [(0, 0.04), (10000, 0.06), (40000, 0.065), (60000, 0.085), (250000, 0.0925), (500000, 0.0975), (1000000, 0.1075)], 'standard_deduction': 13850},
    }
    
    def __init__(self):
        """Initialize the calculator"""
        pass
    
    def calculate_federal_income_tax(
        self, 
        annual_gross: Decimal, 
        filing_status: FilingStatus,
        allowances: int = 0,
        additional_withholding: Decimal = Decimal('0')
    ) -> Decimal:
        """
        Calculate federal income tax using 2024 IRS tax brackets
        """
        # Standard deduction
        standard_deduction = Decimal(self.STANDARD_DEDUCTIONS_2024[filing_status])
        
        # Personal exemption equivalent (allowances)
        # Each allowance is approximately $4,700 (2024 value)
        allowance_value = Decimal(4700) * Decimal(allowances)
        
        # Taxable income
        taxable_income = max(Decimal('0'), annual_gross - standard_deduction - allowance_value)
        
        # Calculate tax using brackets
        tax = Decimal('0')
        previous_bracket = Decimal('0')
        
        for bracket_threshold, rate in self.FEDERAL_TAX_BRACKETS_2024:
            if taxable_income > bracket_threshold:
                bracket_amount = min(taxable_income, Decimal(bracket_threshold)) - previous_bracket
                if bracket_amount > 0:
                    tax += bracket_amount * Decimal(rate)
                previous_bracket = Decimal(bracket_threshold)
            else:
                if taxable_income > previous_bracket:
                    tax += (taxable_income - previous_bracket) * Decimal(rate)
                break
        
        # Add additional withholding
        tax += additional_withholding
        
        return tax.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
    
    def calculate_state_income_tax(
        self,
        annual_gross: Decimal,
        state: str,
        filing_status: FilingStatus = FilingStatus.SINGLE
    ) -> Decimal:
        """
        Calculate state income tax based on state-specific rules
        """
        state = state.upper()
        if state not in self.STATE_TAX_DATA:
            raise ValueError(f"Invalid state code: {state}")
        
        state_data = self.STATE_TAX_DATA[state]
        
        # States with no income tax
        if state_data['type'] == 'none':
            return Decimal('0')
        
        # Get standard deduction for state
        standard_deduction = Decimal(state_data.get('standard_deduction', 0))
        taxable_income = max(Decimal('0'), annual_gross - standard_deduction)
        
        # Flat rate states
        if state_data['type'] == 'flat':
            rate = Decimal(state_data['rate'])
            return (taxable_income * rate).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        # Bracket-based states
        if state_data['type'] == 'brackets':
            tax = Decimal('0')
            previous_bracket = Decimal('0')
            brackets = state_data['brackets']
            
            for bracket_threshold, rate in brackets:
                if taxable_income > bracket_threshold:
                    bracket_amount = min(taxable_income, Decimal(bracket_threshold)) - previous_bracket
                    if bracket_amount > 0:
                        tax += bracket_amount * Decimal(rate)
                    previous_bracket = Decimal(bracket_threshold)
                else:
                    if taxable_income > previous_bracket:
                        tax += (taxable_income - previous_bracket) * Decimal(rate)
                    break
            
            return tax.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        return Decimal('0')
    
    def calculate_fica_taxes(
        self,
        gross_pay: Decimal,
        employee_status: EmployeeStatus,
        year_to_date_gross: Decimal = Decimal('0'),
        is_f1_opt_first_2_years: bool = False
    ) -> Tuple[Decimal, Decimal, Decimal]:
        """
        Calculate FICA taxes (Social Security and Medicare)
        
        OPT/H1B employees may be exempt from FICA for first 2 years if:
        - They are F-1 students on OPT (first 2 years)
        - They are J-1 students/scholars (first 2 years)
        - They have a tax treaty exemption
        
        Green Card holders and US Citizens are always subject to FICA
        """
        social_security = Decimal('0')
        medicare = Decimal('0')
        additional_medicare = Decimal('0')
        
        # Check if employee is exempt from FICA
        is_exempt = False
        if employee_status in [EmployeeStatus.OPT, EmployeeStatus.H1B]:
            # F-1 OPT students are exempt from FICA for first 2 years
            # H1B holders are generally NOT exempt unless they have a tax treaty
            if employee_status == EmployeeStatus.OPT and is_f1_opt_first_2_years:
                is_exempt = True
            # Note: H1B holders are typically subject to FICA unless specific treaty applies
        
        if not is_exempt:
            # Social Security Tax (6.2% up to wage base)
            ytd_plus_current = year_to_date_gross + gross_pay
            if year_to_date_gross < self.SOCIAL_SECURITY_WAGE_BASE:
                taxable_for_ss = min(
                    gross_pay,
                    Decimal(self.SOCIAL_SECURITY_WAGE_BASE) - year_to_date_gross
                )
                social_security = taxable_for_ss * Decimal(self.SOCIAL_SECURITY_RATE)
            
            # Medicare Tax (1.45% on all wages)
            medicare = gross_pay * Decimal(self.MEDICARE_RATE)
            
            # Additional Medicare Tax (0.9% on wages over $200,000)
            ytd_plus_current = year_to_date_gross + gross_pay
            if ytd_plus_current > self.ADDITIONAL_MEDICARE_THRESHOLD:
                if year_to_date_gross < self.ADDITIONAL_MEDICARE_THRESHOLD:
                    additional_medicare_base = ytd_plus_current - Decimal(self.ADDITIONAL_MEDICARE_THRESHOLD)
                else:
                    additional_medicare_base = gross_pay
                additional_medicare = additional_medicare_base * Decimal(self.ADDITIONAL_MEDICARE_RATE)
        
        return (
            social_security.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP),
            medicare.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP),
            additional_medicare.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        )
    
    def calculate_paystub(
        self,
        gross_pay: Decimal,
        state: str,
        employee_status: EmployeeStatus,
        filing_status: FilingStatus = FilingStatus.SINGLE,
        pay_periods_per_year: int = 26,  # Bi-weekly
        allowances: int = 0,
        additional_federal_withholding: Decimal = Decimal('0'),
        year_to_date_gross: Decimal = Decimal('0'),
        year_to_date_net: Decimal = Decimal('0'),
        is_f1_opt_first_2_years: bool = False
    ) -> PaystubResult:
        """
        Calculate complete paystub for an employee
        
        Args:
            gross_pay: Gross pay for this pay period
            state: Two-letter state code (e.g., 'CA', 'NY', 'TX')
            employee_status: Employee immigration status
            filing_status: Tax filing status
            pay_periods_per_year: Number of pay periods per year (26 for bi-weekly, 24 for semi-monthly, 12 for monthly)
            allowances: Number of allowances (from W-4)
            additional_federal_withholding: Additional federal withholding amount
            year_to_date_gross: Year-to-date gross pay
            year_to_date_net: Year-to-date net pay
            is_f1_opt_first_2_years: Whether F-1 OPT employee is in first 2 years (FICA exempt)
        
        Returns:
            PaystubResult with all calculations
        """
        # Annualize gross pay
        annual_gross = gross_pay * Decimal(pay_periods_per_year)
        
        # Calculate federal income tax
        annual_federal_tax = self.calculate_federal_income_tax(
            annual_gross,
            filing_status,
            allowances,
            additional_federal_withholding * Decimal(pay_periods_per_year)
        )
        federal_income_tax = (annual_federal_tax / Decimal(pay_periods_per_year)).quantize(
            Decimal('0.01'), rounding=ROUND_HALF_UP
        )
        
        # Calculate state income tax
        annual_state_tax = self.calculate_state_income_tax(annual_gross, state, filing_status)
        state_income_tax = (annual_state_tax / Decimal(pay_periods_per_year)).quantize(
            Decimal('0.01'), rounding=ROUND_HALF_UP
        )
        
        # Calculate FICA taxes
        social_security, medicare, additional_medicare = self.calculate_fica_taxes(
            gross_pay,
            employee_status,
            year_to_date_gross,
            is_f1_opt_first_2_years
        )
        
        # Calculate net pay
        total_deductions = (
            federal_income_tax +
            state_income_tax +
            social_security +
            medicare +
            additional_medicare
        )
        net_pay = gross_pay - total_deductions
        
        # Update year-to-date
        new_ytd_gross = year_to_date_gross + gross_pay
        new_ytd_net = year_to_date_net + net_pay
        
        return PaystubResult(
            gross_pay=gross_pay,
            federal_income_tax=federal_income_tax,
            state_income_tax=state_income_tax,
            social_security_tax=social_security,
            medicare_tax=medicare,
            additional_medicare_tax=additional_medicare,
            net_pay=net_pay,
            year_to_date_gross=new_ytd_gross,
            year_to_date_net=new_ytd_net
        )
    
    def format_paystub(self, result: PaystubResult, employee_name: str = "", pay_period: str = "") -> str:
        """
        Format paystub as a readable string
        """
        lines = []
        if employee_name:
            lines.append(f"Employee: {employee_name}")
        if pay_period:
            lines.append(f"Pay Period: {pay_period}")
        lines.append("=" * 50)
        lines.append(f"Gross Pay:                    ${result.gross_pay:,.2f}")
        lines.append("-" * 50)
        lines.append("Deductions:")
        lines.append(f"  Federal Income Tax:         ${result.federal_income_tax:,.2f}")
        lines.append(f"  State Income Tax:           ${result.state_income_tax:,.2f}")
        lines.append(f"  Social Security Tax:        ${result.social_security_tax:,.2f}")
        lines.append(f"  Medicare Tax:               ${result.medicare_tax:,.2f}")
        if result.additional_medicare_tax > 0:
            lines.append(f"  Additional Medicare Tax:    ${result.additional_medicare_tax:,.2f}")
        lines.append("-" * 50)
        total_deductions = (
            result.federal_income_tax +
            result.state_income_tax +
            result.social_security_tax +
            result.medicare_tax +
            result.additional_medicare_tax
        )
        lines.append(f"Total Deductions:             ${total_deductions:,.2f}")
        lines.append("=" * 50)
        lines.append(f"Net Pay:                      ${result.net_pay:,.2f}")
        lines.append("")
        lines.append("Year-to-Date:")
        lines.append(f"  Gross Pay:                  ${result.year_to_date_gross:,.2f}")
        lines.append(f"  Net Pay:                    ${result.year_to_date_net:,.2f}")
        
        return "\n".join(lines)


# Example usage
if __name__ == "__main__":
    calculator = PaystubCalculator()
    
    # Example 1: US Citizen in California
    print("Example 1: US Citizen in California")
    print("=" * 60)
    result1 = calculator.calculate_paystub(
        gross_pay=Decimal('5000'),  # $5,000 bi-weekly
        state='CA',
        employee_status=EmployeeStatus.US_CITIZEN,
        filing_status=FilingStatus.SINGLE,
        pay_periods_per_year=26,
        allowances=1
    )
    print(calculator.format_paystub(result1, "John Doe", "Bi-weekly"))
    print("\n")
    
    # Example 2: H1B in New York
    print("Example 2: H1B Employee in New York")
    print("=" * 60)
    result2 = calculator.calculate_paystub(
        gross_pay=Decimal('6000'),
        state='NY',
        employee_status=EmployeeStatus.H1B,
        filing_status=FilingStatus.SINGLE,
        pay_periods_per_year=26,
        allowances=0
    )
    print(calculator.format_paystub(result2, "Jane Smith", "Bi-weekly"))
    print("\n")
    
    # Example 3: OPT (FICA exempt) in Texas (no state tax)
    print("Example 3: OPT Employee in Texas (FICA Exempt, No State Tax)")
    print("=" * 60)
    result3 = calculator.calculate_paystub(
        gross_pay=Decimal('4500'),
        state='TX',
        employee_status=EmployeeStatus.OPT,
        filing_status=FilingStatus.SINGLE,
        pay_periods_per_year=26,
        allowances=0,
        is_f1_opt_first_2_years=True
    )
    print(calculator.format_paystub(result3, "Ali Khan", "Bi-weekly"))
    print("\n")
    
    # Example 4: Green Card holder in Florida (no state tax)
    print("Example 4: Green Card Holder in Florida (No State Tax)")
    print("=" * 60)
    result4 = calculator.calculate_paystub(
        gross_pay=Decimal('5500'),
        state='FL',
        employee_status=EmployeeStatus.GREEN_CARD,
        filing_status=FilingStatus.MARRIED_JOINTLY,
        pay_periods_per_year=26,
        allowances=2
    )
    print(calculator.format_paystub(result4, "Maria Garcia", "Bi-weekly"))

