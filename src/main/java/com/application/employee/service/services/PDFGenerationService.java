package com.application.employee.service.services;

import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.entities.YTDData;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PDFGenerationService {

    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font VERY_SMALL_FONT = new Font(Font.HELVETICA, 7, Font.NORMAL);

    public byte[] generateADPPaystub(PayrollRecord payrollRecord, Employee employee, YTDData ytdData) throws IOException, DocumentException {
        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // Header Section
        addHeader(document, employee, payrollRecord);

        // Tax Filing Status and Earnings Section
        addTaxAndEarningsSection(document, employee, payrollRecord, ytdData);

        // Statutory Deductions Section
        addStatutoryDeductionsSection(document, payrollRecord, ytdData);

        // Net Pay and Important Notes
        addNetPayAndNotes(document, payrollRecord);

        // Federal Taxable Wages
        addFederalTaxableWages(document, payrollRecord);

        // Footer/Check Section
        addCheckSection(document, employee, payrollRecord, writer);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, Employee employee, PayrollRecord payrollRecord) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{40f, 20f, 40f});

        // Left Column - Company Info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);

        // Get company information from employee
        String companyNameStr = "Ingenious Heads LLC";
        String companyAddressStr = "21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279";
        
        if (employee.getCompany() != null) {
            companyNameStr = employee.getCompany().getCompanyName() != null ? 
                employee.getCompany().getCompanyName() : companyNameStr;
            companyAddressStr = getCompanyAddress(employee.getCompany());
        }

        Paragraph companyCode = new Paragraph("Company Code: K5/MEL 25841619", SMALL_FONT);
        Paragraph locDept = new Paragraph("Loc/Dept: 01/", SMALL_FONT);
        Paragraph number = new Paragraph("Number: " + (payrollRecord.getId() != null ? String.format("%05d", payrollRecord.getId()) : "50803"), SMALL_FONT);
        Paragraph page = new Paragraph("Page: 1 of 1", SMALL_FONT);
        Paragraph companyName = new Paragraph(companyNameStr, BOLD_FONT);
        Paragraph companyAddress = new Paragraph(companyAddressStr, SMALL_FONT);

        leftCell.addElement(companyCode);
        leftCell.addElement(locDept);
        leftCell.addElement(number);
        leftCell.addElement(page);
        leftCell.addElement(new Paragraph(" "));
        leftCell.addElement(companyName);
        leftCell.addElement(companyAddress);

        // Center Column - Empty
        PdfPCell centerCell = new PdfPCell();
        centerCell.setBorder(Rectangle.NO_BORDER);

        // Right Column - Employee Info and Dates
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph earningsTitle = new Paragraph("Earnings Statement", TITLE_FONT);
        earningsTitle.setAlignment(Element.ALIGN_CENTER);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph periodStart = new Paragraph("Period Starting: " + payrollRecord.getPayPeriodStart().format(dateFormatter), SMALL_FONT);
        Paragraph periodEnd = new Paragraph("Period Ending: " + payrollRecord.getPayPeriodEnd().format(dateFormatter), SMALL_FONT);
        Paragraph payDate = new Paragraph("Pay Date: " + payrollRecord.getPayDate().format(dateFormatter), SMALL_FONT);
        
        String fullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : "");
        Paragraph employeeName = new Paragraph(fullName.trim(), BOLD_FONT);
        
        String empAddress = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getResidentialAddress() != null ?
            employee.getEmployeeDetails().getResidentialAddress() : "152 Pampano Ln, Saint Charles, MO 63301";
        Paragraph employeeAddress = new Paragraph(empAddress, SMALL_FONT);

        rightCell.addElement(earningsTitle);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(periodStart);
        rightCell.addElement(periodEnd);
        rightCell.addElement(payDate);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(employeeName);
        rightCell.addElement(employeeAddress);

        headerTable.addCell(leftCell);
        headerTable.addCell(centerCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addTaxAndEarningsSection(Document document, Employee employee, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{33f, 34f, 33f});

        // Left Column - Tax Filing Status
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph taxFilingTitle = new Paragraph("Taxable Filing Status: Single", SMALL_FONT);
        Paragraph exemptionsTitle = new Paragraph("Exemptions/Allowances:", SMALL_FONT);
        Paragraph federalExempt = new Paragraph("Federal: Std W/H Table", SMALL_FONT);
        Paragraph stateExempt = new Paragraph("State: 0", SMALL_FONT);
        Paragraph localExempt = new Paragraph("Local: 0", SMALL_FONT);
        
        String ssn = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getSsn() != null ?
            maskSSN(employee.getEmployeeDetails().getSsn()) : "XXX-XX-XXXX";
        Paragraph ssnPara = new Paragraph("Social Security Number: " + ssn, SMALL_FONT);

        leftCell.addElement(taxFilingTitle);
        leftCell.addElement(exemptionsTitle);
        leftCell.addElement(federalExempt);
        leftCell.addElement(stateExempt);
        leftCell.addElement(localExempt);
        leftCell.addElement(new Paragraph(" "));
        leftCell.addElement(ssnPara);

        // Center Column - Tax Override and Earnings Table
        PdfPCell centerCell = new PdfPCell();
        centerCell.setBorder(Rectangle.NO_BORDER);
        centerCell.setPadding(5);
        centerCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph taxOverrideTitle = new Paragraph("Tax Override", SMALL_FONT);
        Paragraph federalOverride = new Paragraph("Federal: 0.00 Addnl", SMALL_FONT);
        Paragraph stateOverride = new Paragraph("State:", SMALL_FONT);
        Paragraph localOverride = new Paragraph("Local:", SMALL_FONT);

        centerCell.addElement(taxOverrideTitle);
        centerCell.addElement(federalOverride);
        centerCell.addElement(stateOverride);
        centerCell.addElement(localOverride);
        centerCell.addElement(new Paragraph(" "));

        // Earnings Table
        PdfPTable earningsTable = new PdfPTable(5);
        earningsTable.setWidthPercentage(100);
        earningsTable.setWidths(new float[]{30f, 15f, 20f, 17.5f, 17.5f});

        addTableHeader(earningsTable, "Earnings");
        addTableHeader(earningsTable, "rate");
        addTableHeader(earningsTable, "hours/units");
        addTableHeader(earningsTable, "this period");
        addTableHeader(earningsTable, "year to date");

        // Regular Earnings Row
        addTableCell(earningsTable, "Regular", NORMAL_FONT);
        addTableCell(earningsTable, "", NORMAL_FONT, Element.ALIGN_RIGHT); // Rate can be blank for salaried
        addTableCell(earningsTable, "0.00", NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(payrollRecord.getGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Gross Pay Summary Row
        PdfPCell summaryCell = new PdfPCell(new Phrase("Gross Pay", BOLD_FONT));
        summaryCell.setColspan(3);
        summaryCell.setBorder(Rectangle.NO_BORDER);
        summaryCell.setPadding(5);
        earningsTable.addCell(summaryCell);
        addTableCell(earningsTable, formatCurrency(payrollRecord.getGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);

        centerCell.addElement(earningsTable);

        // Right Column - Other Benefits
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph totalHoursTitle = new Paragraph("Total Hours Worked", SMALL_FONT);
        Paragraph totalHoursThisPeriod = new Paragraph("This Period: 0.00", SMALL_FONT);
        Paragraph totalHoursYTD = new Paragraph("Year to Date: 0.00", SMALL_FONT);
        Paragraph basisOfPay = new Paragraph("Basis of pay: Salaried", SMALL_FONT);

        rightCell.addElement(totalHoursTitle);
        rightCell.addElement(totalHoursThisPeriod);
        rightCell.addElement(totalHoursYTD);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(basisOfPay);

        mainTable.addCell(leftCell);
        mainTable.addCell(centerCell);
        mainTable.addCell(rightCell);

        document.add(mainTable);
        document.add(new Paragraph(" "));
    }

    private void addStatutoryDeductionsSection(Document document, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable deductionsTable = new PdfPTable(3);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setWidths(new float[]{50f, 25f, 25f});

        // Header
        addTableHeader(deductionsTable, "Statutory Deductions");
        addTableHeader(deductionsTable, "this period");
        addTableHeader(deductionsTable, "year to date");

        // Federal Income
        addDeductionRow(deductionsTable, "Federal Income", payrollRecord.getFederalTax(), 
            ytdData != null ? ytdData.getYtdFederalTax() : null);

        // State Tax (should come before Social Security and Medicare)
        if (payrollRecord.getStateTax() != null) {
            String stateTaxName = payrollRecord.getStateTaxName() != null ? 
                payrollRecord.getStateTaxName() : "State Income";
            addDeductionRow(deductionsTable, stateTaxName, payrollRecord.getStateTax(),
                ytdData != null ? ytdData.getYtdStateTax() : null);
        }

        // Social Security
        if (payrollRecord.getSocialSecurity() != null && payrollRecord.getSocialSecurity().compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Social Security", payrollRecord.getSocialSecurity(),
                ytdData != null ? ytdData.getYtdSocialSecurity() : null);
        }

        // Medicare
        if (payrollRecord.getMedicare() != null && payrollRecord.getMedicare().compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Medicare", payrollRecord.getMedicare(),
                ytdData != null ? ytdData.getYtdMedicare() : null);
        }

        // Local Tax
        if (payrollRecord.getLocalTax() != null && payrollRecord.getLocalTax().compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Local Tax", payrollRecord.getLocalTax(),
                ytdData != null ? ytdData.getYtdLocalTax() : null);
        }

        // Additional Medicare
        if (payrollRecord.getAdditionalMedicare() != null && payrollRecord.getAdditionalMedicare().compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Additional Medicare", payrollRecord.getAdditionalMedicare(), null);
        }

        // Custom Deductions
        if (payrollRecord.getCustomDeductionsJson() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> customDeductions = mapper.readValue(payrollRecord.getCustomDeductionsJson(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                
                for (Map.Entry<String, Object> entry : customDeductions.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    String name = key;
                    BigDecimal amount = BigDecimal.ZERO;
                    
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fieldData = (Map<String, Object>) value;
                        name = fieldData.containsKey("name") ? (String) fieldData.get("name") : key;
                        if (fieldData.containsKey("value")) {
                            Object val = fieldData.get("value");
                            if (val instanceof Number) {
                                amount = BigDecimal.valueOf(((Number) val).doubleValue());
                            }
                        }
                    } else if (value instanceof Number) {
                        amount = BigDecimal.valueOf(((Number) value).doubleValue());
                    }
                    
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        addDeductionRow(deductionsTable, name, amount, null);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Net Pay Row - part of the deductions table
        PdfPCell netPayLabel = new PdfPCell(new Phrase("Net Pay", BOLD_FONT));
        netPayLabel.setBorder(Rectangle.NO_BORDER);
        netPayLabel.setPadding(5);
        deductionsTable.addCell(netPayLabel);
        
        PdfPCell netPayValue = new PdfPCell(new Phrase(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT));
        netPayValue.setBorder(Rectangle.NO_BORDER);
        netPayValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netPayValue.setPadding(5);
        deductionsTable.addCell(netPayValue);
        
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(Rectangle.NO_BORDER);
        deductionsTable.addCell(emptyCell);

        document.add(deductionsTable);
        document.add(new Paragraph(" "));
    }

    private void addNetPayAndNotes(Document document, PayrollRecord payrollRecord) throws DocumentException {
        // Net Pay is now part of the deductions table, so this method is empty
        // Keeping it for structure but not adding anything
    }

    private void addFederalTaxableWages(Document document, PayrollRecord payrollRecord) throws DocumentException {
        Paragraph federalTaxable = new Paragraph(
            "Your federal taxable wages this period are " + formatCurrency(payrollRecord.getGrossPay()),
            SMALL_FONT
        );
        document.add(federalTaxable);
        document.add(new Paragraph(" "));
    }

    private void addCheckSection(Document document, Employee employee, PayrollRecord payrollRecord, PdfWriter writer) throws DocumentException {
        // Add watermark first (behind the content)
        PdfContentByte canvas = writer.getDirectContentUnder();
        Font watermarkFont = new Font(Font.HELVETICA, 60, Font.BOLD, new Color(180, 180, 180));
        Phrase watermark = new Phrase("VOID NON-NEGOTIABLE", watermarkFont);
        
        // Position watermark in the lower portion of the page (where check is)
        float pageWidth = document.right() - document.left();
        float pageHeight = document.top() - document.bottom();
        float watermarkX = pageWidth / 2f + document.leftMargin();
        float watermarkY = pageHeight * 0.25f + document.bottomMargin(); // Lower quarter of page
        
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark,
            watermarkX, watermarkY, 0f);

        PdfPTable checkTable = new PdfPTable(2);
        checkTable.setWidthPercentage(100);
        checkTable.setWidths(new float[]{50f, 50f});

        // Left Column - Company Info (bottom aligned)
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);
        leftCell.setVerticalAlignment(Element.ALIGN_BOTTOM);

        String companyNameStr = "Ingenious Heads LLC";
        String companyAddressStr = "21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279";
        
        if (employee.getCompany() != null) {
            companyNameStr = employee.getCompany().getCompanyName() != null ? 
                employee.getCompany().getCompanyName() : companyNameStr;
            companyAddressStr = getCompanyAddress(employee.getCompany());
        }

        Paragraph companyInfo = new Paragraph(
            companyNameStr + ", " + companyAddressStr,
            SMALL_FONT
        );

        leftCell.addElement(companyInfo);

        // Right Column - Check Details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        // Get bank routing and account from employee details
        String routingNumber = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getRoutingNumber() != null ?
            employee.getEmployeeDetails().getRoutingNumber() : "68-426";
        String accountNumber = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getAccNumber() != null ?
            maskAccountNumber(employee.getEmployeeDetails().getAccNumber()) : "514";
        String bankInfo = routingNumber + "/" + accountNumber;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String fullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : "");
        String empAddress = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getResidentialAddress() != null ?
            employee.getEmployeeDetails().getResidentialAddress() : "152 Pampano Ln, Saint Charles, MO 63301";

        Paragraph bankRouting = new Paragraph(bankInfo, SMALL_FONT);
        Paragraph payrollCheck = new Paragraph(
            "Payroll Check Number: " + (payrollRecord.getId() != null ? String.format("%05d", payrollRecord.getId()) : "00001"),
            SMALL_FONT
        );
        Paragraph payDate = new Paragraph(
            "Pay Date: " + payrollRecord.getPayDate().format(dateFormatter),
            SMALL_FONT
        );
        Paragraph payToOrder = new Paragraph("Pay to the order of: " + fullName.trim(), SMALL_FONT);
        Paragraph payeeAddress = new Paragraph(empAddress, SMALL_FONT);
        Paragraph amountWords = new Paragraph(
            numberToWords(payrollRecord.getNetPay()),
            SMALL_FONT
        );
        
        Paragraph netPayBox = new Paragraph(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT);
        netPayBox.setAlignment(Element.ALIGN_RIGHT);

        rightCell.addElement(bankRouting);
        rightCell.addElement(payrollCheck);
        rightCell.addElement(payDate);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(payToOrder);
        rightCell.addElement(payeeAddress);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(amountWords);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(netPayBox);

        checkTable.addCell(leftCell);
        checkTable.addCell(rightCell);

        document.add(checkTable);
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

    private String maskSSN(String ssn) {
        if (ssn == null || ssn.length() < 4) return "XXX-XX-XXXX";
        String cleaned = ssn.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) return "XXX-XX-XXXX";
        return "XXX-XX-" + cleaned.substring(cleaned.length() - 4);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 3) return "514";
        String cleaned = accountNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 3) return cleaned;
        return cleaned.substring(cleaned.length() - 3);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(new Color(240, 240, 240));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        addTableCell(table, text, font, Element.ALIGN_LEFT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addDeductionRow(PdfPTable table, String name, BigDecimal thisPeriod, BigDecimal ytd) {
        addTableCell(table, name, NORMAL_FONT);
        String thisPeriodStr = thisPeriod != null && thisPeriod.compareTo(BigDecimal.ZERO) >= 0 ?
            formatCurrency(thisPeriod) : "0.00";
        // Show negative for deductions
        if (!thisPeriodStr.startsWith("-") && thisPeriod != null && thisPeriod.compareTo(BigDecimal.ZERO) > 0) {
            thisPeriodStr = "-" + thisPeriodStr;
        }
        addTableCell(table, thisPeriodStr, NORMAL_FONT, Element.ALIGN_RIGHT);
        String ytdStr = ytd != null ? formatCurrency(ytd) : "0.00";
        addTableCell(table, ytdStr, NORMAL_FONT, Element.ALIGN_RIGHT);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount);
    }

    private String numberToWords(BigDecimal amount) {
        if (amount == null) return "ZERO AND 00/100";
        
        long dollars = amount.longValue();
        int cents = amount.subtract(BigDecimal.valueOf(dollars)).multiply(BigDecimal.valueOf(100)).intValue();
        
        String dollarsWords = convertNumberToWords(dollars);
        return dollarsWords + " AND " + String.format("%02d", cents) + "/100";
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
        
        return "";
    }
}
