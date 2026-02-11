package com.application.employee.service.services;

import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.EmployeeDetails;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.entities.YTDData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class HTMLPaystubTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String generateHTML(PayrollRecord payrollRecord, Employee employee, YTDData ytdData) {
        // Get company information
        Companies company = employee.getCompany();
        String companyName = company != null && company.getCompanyName() != null 
            ? company.getCompanyName() 
            : "Ingenious Heads LLC";
        String companyAddress = getCompanyAddress(company);
        
        // Get employee information
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        String employeeAddress = getEmployeeAddress(employee);
        String employeeId = employee.getEmployeeID() != null ? employee.getEmployeeID() : "";
        
        // Get check number
        String checkNumber = payrollRecord.getCheckNumber() != null 
            ? String.valueOf(payrollRecord.getCheckNumber()) 
            : "";
        
        // Format dates
        String periodStart = payrollRecord.getPayPeriodStart().format(DATE_FORMATTER);
        String periodEnd = payrollRecord.getPayPeriodEnd().format(DATE_FORMATTER);
        String payDate = payrollRecord.getPayDate().format(DATE_FORMATTER);
        
        // Get YTD values
        BigDecimal ytdGrossPay = ytdData != null && ytdData.getYtdGrossPay() != null 
            ? ytdData.getYtdGrossPay() 
            : payrollRecord.getYtdGrossPay() != null ? payrollRecord.getYtdGrossPay() : BigDecimal.ZERO;
        BigDecimal ytdNetPay = ytdData != null && ytdData.getYtdNetPay() != null 
            ? ytdData.getYtdNetPay() 
            : payrollRecord.getYtdNetPay() != null ? payrollRecord.getYtdNetPay() : BigDecimal.ZERO;
        BigDecimal ytdFederalTax = ytdData != null && ytdData.getYtdFederalTax() != null 
            ? ytdData.getYtdFederalTax() : BigDecimal.ZERO;
        BigDecimal ytdStateTax = ytdData != null && ytdData.getYtdStateTax() != null 
            ? ytdData.getYtdStateTax() : BigDecimal.ZERO;
        BigDecimal ytdLocalTax = ytdData != null && ytdData.getYtdLocalTax() != null 
            ? ytdData.getYtdLocalTax() : BigDecimal.ZERO;
        BigDecimal ytdSocialSecurity = ytdData != null && ytdData.getYtdSocialSecurity() != null 
            ? ytdData.getYtdSocialSecurity() : BigDecimal.ZERO;
        BigDecimal ytdMedicare = ytdData != null && ytdData.getYtdMedicare() != null 
            ? ytdData.getYtdMedicare() : BigDecimal.ZERO;
        
        // Parse custom deductions
        Map<String, Object> customDeductions = parseCustomDeductions(payrollRecord.getCustomDeductionsJson());
        
        // Build HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\" />\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n");
        html.append("    <title>Paystub</title>\n");
        html.append(getCSS());
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"paystub-html-container\">\n");
        html.append("        <div class=\"paystub-html\">\n");
        
        // Header Section
        html.append(generateHeader(companyName, companyAddress, employeeId, periodStart, periodEnd, payDate, employeeName, employeeAddress));
        
        // Main Content
        html.append(generateMainContent(payrollRecord, ytdGrossPay, ytdFederalTax, ytdStateTax, ytdLocalTax, 
            ytdSocialSecurity, ytdMedicare, customDeductions, employee));
        
        // Check Stub Section
        html.append(generateCheckStub(companyName, companyAddress, employeeName, employeeAddress, 
            payrollRecord.getGrossPay(), payrollRecord.getNetPay(), checkNumber, payDate, employee));
        
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    private String generateHeader(String companyName, String companyAddress, String employeeId, 
                                  String periodStart, String periodEnd, String payDate,
                                  String employeeName, String employeeAddress) {
        StringBuilder html = new StringBuilder();
        html.append("            <!-- Header Section -->\n");
        html.append("            <div class=\"paystub-header\">\n");
        html.append("                <div class=\"header-left\">\n");
        html.append("                    <div class=\"company-code\">Company Code K5 / MEL ").append(employeeId).append(" 01/</div>\n");
        html.append("                    <div class=\"loc-dept\">Loc/Dept 50838</div>\n");
        html.append("                    <div class=\"page-number\">Number Page 1 of 1</div>\n");
        html.append("                    <div class=\"company-name\">").append(escapeHtml(companyName)).append("</div>\n");
        html.append("                    <div class=\"company-address\">").append(escapeHtml(companyAddress)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"header-right\">\n");
        html.append("                    <div class=\"earnings-statement-title\">Earnings Statement</div>\n");
        html.append("                    <div class=\"period-info\">\n");
        html.append("                        <div>Period Starting: ").append(periodStart).append("</div>\n");
        html.append("                        <div>Period Ending: ").append(periodEnd).append("</div>\n");
        html.append("                        <div>Pay Date: ").append(payDate).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                    <!-- Employee Info moved to right side -->\n");
        html.append("                    <div class=\"employee-info\" style=\"margin-top: 8px; text-align: right;\">\n");
        html.append("                        <div class=\"employee-name\">").append(escapeHtml(employeeName)).append("</div>\n");
        html.append("                        <div class=\"employee-address\">").append(escapeHtml(employeeAddress)).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        return html.toString();
    }

    private String generateMainContent(PayrollRecord payrollRecord, BigDecimal ytdGrossPay,
                                      BigDecimal ytdFederalTax, BigDecimal ytdStateTax, BigDecimal ytdLocalTax,
                                      BigDecimal ytdSocialSecurity, BigDecimal ytdMedicare,
                                      Map<String, Object> customDeductions, Employee employee) {
        StringBuilder html = new StringBuilder();
        html.append("            <!-- Main Content Section -->\n");
        html.append("            <div class=\"paystub-main\">\n");
        html.append("                <div class=\"main-left\">\n");
        
        // Tax Information and Tax Override - Side by Side
        html.append(generateTaxInfoWithOverride(employee));
        
        // Earnings Table
        html.append(generateEarningsTable(payrollRecord.getGrossPay(), ytdGrossPay));
        
        // Statutory Deductions
        html.append(generateStatutoryDeductions(payrollRecord, ytdFederalTax, ytdStateTax, ytdLocalTax, 
            ytdSocialSecurity, ytdMedicare, customDeductions));
        
        // Voluntary Deductions
        html.append(generateVoluntaryDeductions(payrollRecord, customDeductions));
        
        // Net Pay
        html.append(generateNetPay(payrollRecord.getNetPay()));
        
        html.append("                </div>\n");
        html.append("                <div class=\"main-right\">\n");
        html.append("                    <!-- Important Notes -->\n");
        html.append("                    <div class=\"notes-section\" style=\"margin-top: 0;\">\n");
        html.append("                        <div class=\"section-title\">Important Notes</div>\n");
        html.append("                        <div>Basis of pay: Salaried</div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <!-- Spacer between Net Pay and Check Stub -->\n");
        html.append("            <div class=\"section-spacer\"></div>\n");
        return html.toString();
    }

    private String generateTaxInfoWithOverride(Employee employee) {
        StringBuilder html = new StringBuilder();
        html.append("                    <!-- Tax Information and Tax Override - Side by Side -->\n");
        html.append("                    <div class=\"tax-info-wrapper\">\n");
        html.append("                        <div class=\"tax-info-section\">\n");
        html.append("                            <div class=\"tax-info-row\">\n");
        html.append("                                <span>Taxable Filing Status: Single</span>\n");
        html.append("                            </div>\n");
        html.append("                            <div class=\"tax-info-row\">\n");
        html.append("                                <span>Exemptions/Allowances:</span>\n");
        html.append("                            </div>\n");
        html.append("                            <div class=\"tax-info-row\">\n");
        html.append("                                <span>Federal: Std W/H</span>\n");
        html.append("                                <span>State: Table 0 0</span>\n");
        html.append("                                <span>Local:</span>\n");
        html.append("                            </div>\n");
        html.append("                            <div class=\"tax-info-row\">\n");
        html.append("                                <span>Social Security Number: XXX-XX-XXXX</span>\n");
        html.append("                            </div>\n");
        html.append("                        </div>\n");
        html.append("                        \n");
        html.append("                        <!-- Tax Override - beside Taxable Filing Status -->\n");
        html.append("                        <div class=\"tax-override-section\" style=\"margin-top: 0; margin-left: 20px;\">\n");
        html.append("                            <div class=\"section-title\">Tax Override:</div>\n");
        html.append("                            <div>Federal: 0.00 Addnl</div>\n");
        html.append("                            <div>State:</div>\n");
        html.append("                            <div>Local:</div>\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        return html.toString();
    }

    private String generateEarningsTable(BigDecimal grossPay, BigDecimal ytdGrossPay) {
        StringBuilder html = new StringBuilder();
        html.append("                    <!-- Earnings Table -->\n");
        html.append("                    <div class=\"earnings-section\">\n");
        html.append("                        <table class=\"paystub-table\">\n");
        html.append("                            <thead>\n");
        html.append("                                <tr>\n");
        html.append("                                    <th>Earnings</th>\n");
        html.append("                                    <th>rate</th>\n");
        html.append("                                    <th>hours/units</th>\n");
        html.append("                                    <th class=\"text-right\">this period</th>\n");
        html.append("                                    <th class=\"text-right\">year to date</th>\n");
        html.append("                                </tr>\n");
        html.append("                            </thead>\n");
        html.append("                            <tbody>\n");
        html.append("                                <tr>\n");
        html.append("                                    <td>Regular</td>\n");
        html.append("                                    <td>$0.00</td>\n");
        html.append("                                    <td>0.00</td>\n");
        html.append("                                    <td class=\"text-right\">").append(formatCurrency(grossPay)).append("</td>\n");
        html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdGrossPay)).append("</td>\n");
        html.append("                                </tr>\n");
        html.append("                                <tr class=\"total-row\">\n");
        html.append("                                    <td colspan=\"3\"><strong>Gross Pay</strong></td>\n");
        html.append("                                    <td class=\"text-right\"><strong>").append(formatCurrency(grossPay)).append("</strong></td>\n");
        html.append("                                    <td class=\"text-right\"><strong>").append(formatCurrency(ytdGrossPay)).append("</strong></td>\n");
        html.append("                                </tr>\n");
        html.append("                            </tbody>\n");
        html.append("                        </table>\n");
        html.append("                    </div>\n");
        return html.toString();
    }

    private String generateStatutoryDeductions(PayrollRecord payrollRecord, BigDecimal ytdFederalTax, 
                                               BigDecimal ytdStateTax, BigDecimal ytdLocalTax,
                                               BigDecimal ytdSocialSecurity, BigDecimal ytdMedicare,
                                               Map<String, Object> customDeductions) {
        StringBuilder html = new StringBuilder();
        html.append("                    <!-- Statutory Deductions -->\n");
        html.append("                    <div class=\"deductions-section\">\n");
        html.append("                        <table class=\"paystub-table deductions-table\">\n");
        html.append("                            <thead>\n");
        html.append("                                <tr>\n");
        html.append("                                    <th><span class=\"section-label\">Statutory Deductions</span></th>\n");
        html.append("                                    <th class=\"text-right\">this period</th>\n");
        html.append("                                    <th class=\"text-right\">year to date</th>\n");
        html.append("                                </tr>\n");
        html.append("                            </thead>\n");
        html.append("                            <tbody>\n");
        
        // Federal Tax
        if (payrollRecord.getFederalTax() != null && payrollRecord.getFederalTax().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                                <tr>\n");
            html.append("                                    <td>Federal Income</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getFederalTax())).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdFederalTax)).append("</td>\n");
            html.append("                                </tr>\n");
        }
        
        // Social Security
        if (payrollRecord.getSocialSecurity() != null && payrollRecord.getSocialSecurity().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                                <tr>\n");
            html.append("                                    <td>Social Security</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getSocialSecurity())).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdSocialSecurity)).append("</td>\n");
            html.append("                                </tr>\n");
        }
        
        // Medicare
        if (payrollRecord.getMedicare() != null && payrollRecord.getMedicare().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                                <tr>\n");
            html.append("                                    <td>Medicare</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getMedicare())).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdMedicare)).append("</td>\n");
            html.append("                                </tr>\n");
        }
        
        // State Tax
        String stateTaxName = payrollRecord.getStateTaxName() != null && !payrollRecord.getStateTaxName().trim().isEmpty()
            ? payrollRecord.getStateTaxName()
            : "State Income";
        if (payrollRecord.getStateTax() != null && payrollRecord.getStateTax().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                                <tr>\n");
            html.append("                                    <td>").append(escapeHtml(stateTaxName)).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getStateTax())).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdStateTax)).append("</td>\n");
            html.append("                                </tr>\n");
        }
        
        // Local Tax
        if (payrollRecord.getLocalTax() != null && payrollRecord.getLocalTax().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                                <tr>\n");
            html.append("                                    <td>Local Tax</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getLocalTax())).append("</td>\n");
            html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdLocalTax)).append("</td>\n");
            html.append("                                </tr>\n");
        }
        
        html.append("                            </tbody>\n");
        html.append("                        </table>\n");
        html.append("                    </div>\n");
        return html.toString();
    }

    private String generateVoluntaryDeductions(PayrollRecord payrollRecord, Map<String, Object> customDeductions) {
        StringBuilder html = new StringBuilder();
        
        // Check if there are any voluntary deductions
        boolean hasHealthInsurance = payrollRecord.getHealthInsurance() != null && payrollRecord.getHealthInsurance().compareTo(BigDecimal.ZERO) > 0;
        boolean hasOtherDeductions = payrollRecord.getOtherDeductions() != null && payrollRecord.getOtherDeductions().compareTo(BigDecimal.ZERO) > 0;
        boolean hasCustomDeductions = customDeductions != null && !customDeductions.isEmpty();
        
        if (hasHealthInsurance || hasOtherDeductions || hasCustomDeductions) {
            html.append("                    <!-- Voluntary Deductions -->\n");
            html.append("                    <div class=\"deductions-section\">\n");
            html.append("                        <table class=\"paystub-table deductions-table\">\n");
            html.append("                            <thead>\n");
            html.append("                                <tr>\n");
            html.append("                                    <th><span class=\"section-label\">Voluntary Deductions</span></th>\n");
            html.append("                                    <th class=\"text-right\">this period</th>\n");
            html.append("                                    <th class=\"text-right\">year to date</th>\n");
            html.append("                                </tr>\n");
            html.append("                            </thead>\n");
            html.append("                            <tbody>\n");
            
            // Health Insurance
            if (hasHealthInsurance) {
                html.append("                                <tr>\n");
                html.append("                                    <td>Health Insurance</td>\n");
                html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getHealthInsurance())).append("</td>\n");
                html.append("                                    <td class=\"text-right\">$0.00</td>\n");
                html.append("                                </tr>\n");
            }
            
            // Custom Deductions (Miscellaneous, Advance, etc.)
            if (hasCustomDeductions) {
                for (Map.Entry<String, Object> entry : customDeductions.entrySet()) {
                    String deductionName = getDeductionName(entry.getKey(), entry.getValue());
                    BigDecimal thisPeriodAmount = getDeductionAmount(entry.getValue(), false);
                    BigDecimal ytdAmount = getDeductionAmount(entry.getValue(), true);
                    
                    html.append("                                <tr>\n");
                    html.append("                                    <td>").append(escapeHtml(deductionName)).append("</td>\n");
                    html.append("                                    <td class=\"text-right\">").append(formatCurrency(thisPeriodAmount)).append("</td>\n");
                    html.append("                                    <td class=\"text-right\">").append(formatCurrency(ytdAmount)).append("</td>\n");
                    html.append("                                </tr>\n");
                }
            }
            
            // Other Deductions
            if (hasOtherDeductions) {
                html.append("                                <tr>\n");
                html.append("                                    <td>Other Deductions</td>\n");
                html.append("                                    <td class=\"text-right\">").append(formatCurrency(payrollRecord.getOtherDeductions())).append("</td>\n");
                html.append("                                    <td class=\"text-right\">$0.00</td>\n");
                html.append("                                </tr>\n");
            }
            
            html.append("                            </tbody>\n");
            html.append("                        </table>\n");
            html.append("                    </div>\n");
        }
        return html.toString();
    }

    private String generateNetPay(BigDecimal netPay) {
        StringBuilder html = new StringBuilder();
        html.append("                    <!-- Net Pay -->\n");
        html.append("                    <div class=\"net-pay-section\">\n");
        html.append("                        <div class=\"net-pay-row\">\n");
        html.append("                            <span class=\"net-pay-label\">Net Pay</span>\n");
        html.append("                            <span class=\"net-pay-amount\">").append(formatCurrency(netPay)).append("</span>\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        return html.toString();
    }

    private String generateCheckStub(String companyName, String companyAddress, String employeeName, 
                                     String employeeAddress, BigDecimal grossPay, BigDecimal netPay,
                                     String checkNumber, String payDate, Employee employee) {
        StringBuilder html = new StringBuilder();
        html.append("            <!-- Check Stub Section -->\n");
        html.append("            <div class=\"check-stub-section\" style=\"position: relative;\">\n");
        html.append("                <div class=\"check-watermark\">THIS IS NOT A CHECK</div>\n");
        html.append("                <div class=\"federal-taxable\">\n");
        html.append("                    Your federal taxable wages this period are ").append(formatCurrency(grossPay)).append("\n");
        html.append("                </div>\n");
        html.append("                \n");
        html.append("                <div class=\"check-stub-main\">\n");
        html.append("                    <div class=\"check-stub-left\">\n");
        html.append("                        <div class=\"company-name\">").append(escapeHtml(companyName)).append("</div>\n");
        html.append("                        <div class=\"company-address\">").append(escapeHtml(companyAddress)).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"check-stub-right\">\n");
        
        // Get routing and account numbers
        String routingNumber = "68-426";
        String accountNumber = "514";
        if (employee.getEmployeeDetails() != null) {
            if (employee.getEmployeeDetails().getRoutingNumber() != null) {
                routingNumber = employee.getEmployeeDetails().getRoutingNumber();
            }
            if (employee.getEmployeeDetails().getAccNumber() != null) {
                String accNum = employee.getEmployeeDetails().getAccNumber().replaceAll("[^0-9]", "");
                if (accNum.length() >= 3) {
                    accountNumber = accNum.substring(accNum.length() - 3);
                }
            }
        }
        
        html.append("                        <div>").append(routingNumber).append("/").append(accountNumber).append("</div>\n");
        html.append("                        <div>Payroll Check Number: ").append(checkNumber).append("</div>\n");
        html.append("                        <div>Pay Date: ").append(payDate).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("\n");
        html.append("                <div class=\"payee-section\">\n");
        html.append("                    <div class=\"payee-line\">\n");
        html.append("                        <span>Pay to the order of: ").append(escapeHtml(employeeName)).append("</span>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"amount-line\">\n");
        html.append("                        <span>This amount: ").append(numberToWords(netPay)).append("</span>\n");
        html.append("                        <div class=\"check-amount-section\">\n");
        html.append("                            <div class=\"check-amount\">").append(formatCurrency(netPay)).append("</div>\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"void-text\">VOID - NON NEGOTIABLE</div>\n");
        html.append("                </div>\n");
        html.append("\n");
        html.append("                <div class=\"bank-section\">\n");
        
        // Get bank name
        String bankName = "Chase";
        if (employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getBankName() != null) {
            bankName = employee.getEmployeeDetails().getBankName();
        }
        
        html.append("                    <div class=\"bank-name\">").append(escapeHtml(bankName)).append("</div>\n");
        html.append("                    <div class=\"bank-account\">").append(escapeHtml(employeeName)).append("</div>\n");
        html.append("                    <div class=\"bank-address\">").append(escapeHtml(employeeAddress)).append("</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        return html.toString();
    }

    private String getCompanyAddress(Companies company) {
        if (company == null) {
            return "21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279";
        }

        if (company.getAddressLine1() != null && !company.getAddressLine1().trim().isEmpty()) {
            StringBuilder address = new StringBuilder();
            address.append(company.getAddressLine1());
            if (company.getAddressLine2() != null && !company.getAddressLine2().trim().isEmpty()) {
                address.append(", ").append(company.getAddressLine2());
            }
            if (company.getCity() != null && !company.getCity().trim().isEmpty()) {
                address.append(", ").append(company.getCity());
            }
            if (company.getState() != null && !company.getState().trim().isEmpty()) {
                address.append(", ").append(company.getState());
            }
            if (company.getZipCode() != null && !company.getZipCode().trim().isEmpty()) {
                address.append(" ").append(company.getZipCode());
            }
            return address.toString();
        }

        String companyName = company.getCompanyName() != null ? company.getCompanyName().toLowerCase() : "";
        
        if (companyName.contains("saibersys")) {
            return "2840 Keller Springs Rd., Suite 401, Carrollton, TX 75006";
        } else if (companyName.contains("amensys")) {
            return "860 Hebron Parkway, #603-604, Lewisville, TX 75057";
        } else if (companyName.contains("ingenious")) {
            return "21135 Whitfield Place, Suite 207, Sterling, Virginia 20165";
        } else if (companyName.contains("itiyam")) {
            return "44790 Maynard Square, Suite #230, Ashburn, VA 20147";
        }
        
        return "21135 Whitfield Place, Suite 207, Sterling, Virginia 20165";
    }

    private String getEmployeeAddress(Employee employee) {
        if (employee != null && employee.getEmployeeDetails() != null) {
            EmployeeDetails details = employee.getEmployeeDetails();
            if (details.getResidentialAddress() != null && !details.getResidentialAddress().trim().isEmpty()) {
                return details.getResidentialAddress();
            }
        }
        return "Address not available";
    }

    private Map<String, Object> parseCustomDeductions(String customDeductionsJson) {
        if (customDeductionsJson == null || customDeductionsJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(customDeductionsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing custom deductions JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private String getDeductionName(String key, Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey("name")) {
                String name = String.valueOf(map.get("name"));
                // Fix common misspellings
                if (name.equalsIgnoreCase("miscellanous")) {
                    return "Miscellaneous";
                }
                return name;
            }
        }
        // Fix common misspellings in keys
        String normalizedKey = key.toLowerCase();
        if (normalizedKey.equals("miscellanous") || normalizedKey.equals("miscellanous")) {
            return "Miscellaneous";
        }
        // Convert key to readable format: replace underscores and capitalize first letter of each word
        String[] words = key.replace("_", " ").split(" ");
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                String lowerWord = word.toLowerCase();
                if (lowerWord.equals("miscellanous")) {
                    formattedName.append("Miscellaneous ");
                } else {
                    formattedName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
                }
            }
        }
        return formattedName.toString().trim();
    }

    private BigDecimal getDeductionAmount(Object value, boolean isYtd) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            if (isYtd) {
                Object ytdVal = map.containsKey("ytd") ? map.get("ytd") : map.get("yearToDate");
                if (ytdVal instanceof Number) {
                    return BigDecimal.valueOf(((Number) ytdVal).doubleValue()).abs();
                } else if (ytdVal instanceof String) {
                    try {
                        return new BigDecimal((String) ytdVal).abs();
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                }
                return BigDecimal.ZERO;
            } else {
                Object val = map.get("value");
                if (val instanceof Number) {
                    return BigDecimal.valueOf(((Number) val).doubleValue()).abs();
                } else if (val instanceof String) {
                    try {
                        return new BigDecimal((String) val).abs();
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                }
                return BigDecimal.ZERO;
            }
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue()).abs();
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value).abs();
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String numberToWords(BigDecimal amount) {
        if (amount == null) return "ZERO AND 00/100";
        
        long dollars = amount.longValue();
        int cents = amount.subtract(BigDecimal.valueOf(dollars)).multiply(BigDecimal.valueOf(100)).intValue();
        
        String dollarsWords = convertNumberToWords(dollars);
        return dollarsWords + " AND " + String.format("%02d", cents) + "/100 DOLLARS";
    }

    private String convertNumberToWords(long number) {
        if (number == 0) return "ZERO";
        
        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
            "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN",
            "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
        
        if (number < 20) {
            return ones[(int) number];
        }
        
        if (number < 100) {
            return tens[(int) (number / 10)] + (number % 10 != 0 ? " " + ones[(int) (number % 10)] : "");
        }
        
        if (number < 1000) {
            return ones[(int) (number / 100)] + " HUNDRED" + 
                (number % 100 != 0 ? " " + convertNumberToWords(number % 100) : "");
        }
        
        if (number < 1000000) {
            return convertNumberToWords(number / 1000) + " THOUSAND" + 
                (number % 1000 != 0 ? " " + convertNumberToWords(number % 1000) : "");
        }
        
        return "Amount";
    }

    private String getCSS() {
        return "    <style>\n" +
               "        :root {\n" +
               "            --deduction-indent: 20px;\n" +
               "        }\n" +
               "        * {\n" +
               "            margin: 0;\n" +
               "            padding: 0;\n" +
               "            box-sizing: border-box;\n" +
               "        }\n" +
               "        body {\n" +
               "            font-family: 'Times New Roman', serif;\n" +
               "            background: #f5f5f5;\n" +
               "            padding: 20px;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "            margin: 0;\n" +
               "        }\n" +
               "        .paystub-html-container {\n" +
               "            width: 8.5in;\n" +
               "            height: 11in;\n" +
               "            margin: 0 auto;\n" +
               "            background: white;\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        .paystub-html {\n" +
               "            background: white;\n" +
               "            width: 8.5in;\n" +
               "            height: 11in;\n" +
               "            padding: 0.5in;\n" +
               "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\n" +
               "            position: relative;\n" +
               "            margin: 0 auto;\n" +
               "            overflow: hidden;\n" +
               "            box-sizing: border-box;\n" +
               "        }\n" +
               "        .paystub-header {\n" +
               "            display: flex;\n" +
               "            justify-content: space-between;\n" +
               "            border-bottom: none;\n" +
               "            padding-bottom: 6px;\n" +
               "            margin-bottom: 8px;\n" +
               "        }\n" +
               "        .header-left {\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.05;\n" +
               "        }\n" +
               "        .company-code {\n" +
               "            font-weight: bold;\n" +
               "            margin-bottom: 0;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .loc-dept {\n" +
               "            margin-top: 0;\n" +
               "            margin-bottom: 0;\n" +
               "        }\n" +
               "        .page-number {\n" +
               "            margin-top: 0;\n" +
               "            margin-bottom: 0;\n" +
               "        }\n" +
               "        .company-name {\n" +
               "            font-weight: 700;\n" +
               "            margin-top: 2px;\n" +
               "            margin-bottom: 0;\n" +
               "            font-size: 10pt;\n" +
               "        }\n" +
               "        .company-address {\n" +
               "            font-size: 9pt;\n" +
               "            margin-top: 0;\n" +
               "            margin-bottom: 0;\n" +
               "        }\n" +
               "        .header-right {\n" +
               "            text-align: right;\n" +
               "        }\n" +
               "        .earnings-statement-title {\n" +
               "            font-size: 11pt;\n" +
               "            font-weight: bold;\n" +
               "            margin-bottom: 2px;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .period-info {\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .period-info div {\n" +
               "            margin-bottom: 1px;\n" +
               "        }\n" +
               "        .period-info div:last-child {\n" +
               "            margin-bottom: 8px;\n" +
               "        }\n" +
               "        .employee-info {\n" +
               "            margin-bottom: 8px;\n" +
               "            padding-bottom: 4px;\n" +
               "            border-bottom: none;\n" +
               "        }\n" +
               "        .employee-name {\n" +
               "            font-weight: bold;\n" +
               "            font-size: 10pt;\n" +
               "            margin-bottom: 0;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .employee-address {\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "            margin-top: 1px;\n" +
               "        }\n" +
               "        .paystub-main {\n" +
               "            display: flex;\n" +
               "            gap: 0;\n" +
               "            margin-bottom: 10px;\n" +
               "            align-items: flex-start;\n" +
               "            flex-shrink: 0;\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        .main-left {\n" +
               "            width: 65%;\n" +
               "            flex-shrink: 0;\n" +
               "        }\n" +
               "        .main-right {\n" +
               "            width: 35%;\n" +
               "            flex-shrink: 0;\n" +
               "            margin-left: 0;\n" +
               "            padding-left: 0;\n" +
               "        }\n" +
               "        .tax-info-wrapper {\n" +
               "            display: flex;\n" +
               "            align-items: flex-start;\n" +
               "            margin-bottom: 10px;\n" +
               "            gap: 0;\n" +
               "        }\n" +
               "        .tax-info-section {\n" +
               "            margin-bottom: 0;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "            flex-shrink: 0;\n" +
               "        }\n" +
               "        .tax-info-row {\n" +
               "            margin-bottom: 1px;\n" +
               "            display: flex;\n" +
               "            gap: 10px;\n" +
               "        }\n" +
               "        .tax-info-row span {\n" +
               "            white-space: nowrap;\n" +
               "        }\n" +
               "        .paystub-table {\n" +
               "            width: 100%;\n" +
               "            border-collapse: collapse;\n" +
               "            margin-bottom: 10px;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.05;\n" +
               "            table-layout: fixed;\n" +
               "        }\n" +
               "        .paystub-table th:nth-child(1),\n" +
               "        .paystub-table td:nth-child(1) {\n" +
               "            width: 25%;\n" +
               "        }\n" +
               "        .paystub-table th:nth-child(2),\n" +
               "        .paystub-table td:nth-child(2) {\n" +
               "            width: 15%;\n" +
               "        }\n" +
               "        .paystub-table th:nth-child(3),\n" +
               "        .paystub-table td:nth-child(3) {\n" +
               "            width: 15%;\n" +
               "        }\n" +
               "        .paystub-table th:nth-child(4),\n" +
               "        .paystub-table td:nth-child(4) {\n" +
               "            width: 22.5%;\n" +
               "        }\n" +
               "        .paystub-table th:nth-child(5),\n" +
               "        .paystub-table td:nth-child(5) {\n" +
               "            width: 22.5%;\n" +
               "        }\n" +
               "        .paystub-table.deductions-table th:nth-child(1),\n" +
               "        .paystub-table.deductions-table td:nth-child(1) {\n" +
               "            width: 55%;\n" +
               "        }\n" +
               "        .paystub-table.deductions-table th:nth-child(2),\n" +
               "        .paystub-table.deductions-table td:nth-child(2) {\n" +
               "            width: 22.5%;\n" +
               "        }\n" +
               "        .paystub-table.deductions-table th:nth-child(3),\n" +
               "        .paystub-table.deductions-table td:nth-child(3) {\n" +
               "            width: 22.5%;\n" +
               "        }\n" +
               "        .paystub-table thead {\n" +
               "            border-bottom: 1px solid #ccc;\n" +
               "        }\n" +
               "        .paystub-table th {\n" +
               "            text-align: left;\n" +
               "            padding: 1px 4px;\n" +
               "            font-weight: bold;\n" +
               "            background: transparent;\n" +
               "            border-bottom: 1px solid #ccc;\n" +
               "            border-top: none;\n" +
               "            font-size: 9pt;\n" +
               "        }\n" +
               "        .paystub-table th.text-right {\n" +
               "            text-align: right;\n" +
               "        }\n" +
               "        .paystub-table td {\n" +
               "            padding: 0px 4px;\n" +
               "            border-bottom: none;\n" +
               "            font-size: 9pt;\n" +
               "            height: 16px;\n" +
               "        }\n" +
               "        .paystub-table td.text-right {\n" +
               "            text-align: right;\n" +
               "        }\n" +
               "        .paystub-table .total-row {\n" +
               "            background: transparent;\n" +
               "            font-weight: 700;\n" +
               "        }\n" +
               "        .paystub-table .total-row td {\n" +
               "            padding-top: 1px;\n" +
               "            padding-bottom: 1px;\n" +
               "            border-top: none;\n" +
               "            border-bottom: none;\n" +
               "        }\n" +
               "        .paystub-table .total-row td:first-child {\n" +
               "            text-align: left;\n" +
               "            border-top: none;\n" +
               "            border-bottom: none;\n" +
               "            border-left: none;\n" +
               "            border-right: none;\n" +
               "        }\n" +
               "        .paystub-table .total-row td:nth-child(2),\n" +
               "        .paystub-table .total-row td:nth-child(3) {\n" +
               "            border-top: none;\n" +
               "            border-bottom: none;\n" +
               "            border-left: none;\n" +
               "            border-right: none;\n" +
               "        }\n" +
               "        .paystub-table .total-row td:nth-child(4) {\n" +
               "            border-top: 1px solid #999;\n" +
               "            border-bottom: 1px solid #999;\n" +
               "            border-left: none;\n" +
               "            border-right: none;\n" +
               "        }\n" +
               "        .paystub-table .total-row td:nth-child(5) {\n" +
               "            border-top: 1px solid #999;\n" +
               "            border-bottom: 1px solid #999;\n" +
               "            border-left: none;\n" +
               "            border-right: none;\n" +
               "        }\n" +
               "        .deductions-section {\n" +
               "            margin-bottom: 10px;\n" +
               "        }\n" +
               "        .deductions-section .paystub-table thead {\n" +
               "            border-bottom: none;\n" +
               "        }\n" +
               "        .deductions-section .paystub-table th {\n" +
               "            border-bottom: none;\n" +
               "            padding-left: 0;\n" +
               "        }\n" +
               "        .deductions-section .paystub-table th:first-child,\n" +
               "        .deductions-section .paystub-table td:first-child {\n" +
               "            padding-left: var(--deduction-indent);\n" +
               "        }\n" +
               "        .deductions-section .paystub-table th:first-child {\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        .section-label {\n" +
               "            display: block;\n" +
               "            padding-left: 0;\n" +
               "            padding-bottom: 1px;\n" +
               "            border-bottom: none;\n" +
               "            font-weight: bold;\n" +
               "            position: relative;\n" +
               "            z-index: 1;\n" +
               "        }\n" +
               "        .deductions-section .paystub-table thead {\n" +
               "            background: linear-gradient(to right, transparent var(--deduction-indent), #ccc var(--deduction-indent), #ccc 100%);\n" +
               "            background-repeat: no-repeat;\n" +
               "            background-size: 100% 1px;\n" +
               "            background-position: bottom left;\n" +
               "        }\n" +
               "        .net-pay-section {\n" +
               "            margin-top: 10px;\n" +
               "            margin-bottom: 8px;\n" +
               "            padding: 8px 10px;\n" +
               "            border-top: 2px solid #666;\n" +
               "            border-bottom: 2px solid #666;\n" +
               "            border-left: none;\n" +
               "            border-right: none;\n" +
               "            background: #f8f8f8;\n" +
               "        }\n" +
               "        .net-pay-row {\n" +
               "            display: flex;\n" +
               "            justify-content: space-between;\n" +
               "            align-items: center;\n" +
               "        }\n" +
               "        .net-pay-label {\n" +
               "            font-size: 10pt;\n" +
               "            font-weight: bold;\n" +
               "        }\n" +
               "        .net-pay-amount {\n" +
               "            font-size: 13pt;\n" +
               "            font-weight: 700;\n" +
               "        }\n" +
               "        .tax-override-section, .notes-section {\n" +
               "            margin-bottom: 10px;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "        }\n" +
               "        .section-title {\n" +
               "            font-weight: bold;\n" +
               "            margin-bottom: 2px;\n" +
               "            font-size: 9pt;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .tax-override-section div, .notes-section div {\n" +
               "            margin-bottom: 1px;\n" +
               "        }\n" +
               "        .section-spacer {\n" +
               "            height: 200px;\n" +
               "            width: 100%;\n" +
               "            display: block;\n" +
               "            clear: both;\n" +
               "            flex-grow: 1;\n" +
               "            min-height: 200px;\n" +
               "        }\n" +
               "        .check-stub-section {\n" +
               "            margin-top: 0;\n" +
               "            padding-top: 8px;\n" +
               "            border-top: none;\n" +
               "            position: relative;\n" +
               "            flex-shrink: 0;\n" +
               "            margin-bottom: 0;\n" +
               "        }\n" +
               "        .federal-taxable {\n" +
               "            font-size: 9pt;\n" +
               "            margin-bottom: 8px;\n" +
               "            font-style: italic;\n" +
               "            line-height: 1.2;\n" +
               "            margin-top: 0;\n" +
               "        }\n" +
               "        .check-stub-main {\n" +
               "            display: flex;\n" +
               "            justify-content: space-between;\n" +
               "            margin-bottom: 8px;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.15;\n" +
               "        }\n" +
               "        .check-stub-left {\n" +
               "            line-height: 1.2;\n" +
               "        }\n" +
               "        .check-stub-left div {\n" +
               "            margin-bottom: 1px;\n" +
               "        }\n" +
               "        .check-stub-right {\n" +
               "            text-align: right;\n" +
               "            line-height: 1.2;\n" +
               "        }\n" +
               "        .check-stub-right div {\n" +
               "            margin-bottom: 1px;\n" +
               "        }\n" +
               "        .payee-section {\n" +
               "            margin: 8px 0;\n" +
               "            padding: 6px 10px;\n" +
               "            border: none;\n" +
               "            background: white;\n" +
               "            min-height: 50px;\n" +
               "        }\n" +
               "        .payee-line, .amount-line {\n" +
               "            margin-bottom: 4px;\n" +
               "            font-size: 9pt;\n" +
               "            min-height: 20px;\n" +
               "            border-bottom: 1px solid #ddd;\n" +
               "            padding-bottom: 1px;\n" +
               "            line-height: 1.2;\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        .amount-line {\n" +
               "            display: flex;\n" +
               "            justify-content: space-between;\n" +
               "            align-items: flex-end;\n" +
               "        }\n" +
               "        .check-amount-section {\n" +
               "            text-align: right;\n" +
               "            margin: 0;\n" +
               "            position: relative;\n" +
               "            right: 0;\n" +
               "            bottom: 0;\n" +
               "            margin-left: auto;\n" +
               "        }\n" +
               "        .check-amount {\n" +
               "            font-size: 14pt;\n" +
               "            font-weight: 700;\n" +
               "            border: none;\n" +
               "            display: inline-block;\n" +
               "            padding: 0;\n" +
               "            min-width: auto;\n" +
               "            text-align: right;\n" +
               "            background: transparent;\n" +
               "        }\n" +
               "        .bank-section {\n" +
               "            margin-top: 10px;\n" +
               "            font-size: 9pt;\n" +
               "            line-height: 1.2;\n" +
               "        }\n" +
               "        .bank-name {\n" +
               "            font-weight: bold;\n" +
               "            margin-bottom: 5px;\n" +
               "        }\n" +
               "        .watermark {\n" +
               "            display: none;\n" +
               "        }\n" +
               "        .check-watermark {\n" +
               "            position: absolute;\n" +
               "            top: 50%;\n" +
               "            left: 50%;\n" +
               "            transform: translate(-50%, -50%) rotate(-32deg);\n" +
               "            font-size: 28pt;\n" +
               "            font-weight: bold;\n" +
               "            color: rgba(0, 0, 0, 0.12);\n" +
               "            z-index: 10;\n" +
               "            white-space: nowrap;\n" +
               "            pointer-events: none;\n" +
               "            font-family: 'Times New Roman', serif;\n" +
               "            text-align: center;\n" +
               "            line-height: 1.2;\n" +
               "            width: 100%;\n" +
               "            overflow: visible;\n" +
               "        }\n" +
               "        .void-text {\n" +
               "            font-size: 9pt;\n" +
               "            font-weight: bold;\n" +
               "            color: #000;\n" +
               "            margin-top: 4px;\n" +
               "            text-align: left;\n" +
               "        }\n" +
               "        @media print {\n" +
               "            body {\n" +
               "                padding: 0;\n" +
               "                background: white;\n" +
               "                margin: 0;\n" +
               "            }\n" +
               "            .paystub-html-container {\n" +
               "                padding: 0;\n" +
               "                background: white;\n" +
               "                width: 8.5in;\n" +
               "                height: 11in;\n" +
               "            }\n" +
               "            .paystub-html {\n" +
               "                box-shadow: none;\n" +
               "                margin: 0;\n" +
               "                padding: 0.5in;\n" +
               "                width: 8.5in;\n" +
               "                height: 11in;\n" +
               "            }\n" +
               "            .watermark {\n" +
               "                opacity: 0.08;\n" +
               "            }\n" +
               "        }\n" +
               "        @media screen {\n" +
               "            .paystub-html-container {\n" +
               "                width: 8.5in;\n" +
               "                height: 11in;\n" +
               "            }\n" +
               "        }\n" +
               "    </style>\n";
    }
}
