package com.application.employee.service.services;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.entities.YTDData;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
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

    public byte[] generateADPPaystub(PayrollRecord payrollRecord, Employee employee, YTDData ytdData) throws IOException, DocumentException {
        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // Header Section
        addHeader(document, employee, payrollRecord);

        // Earnings Section
        addEarningsSection(document, payrollRecord, ytdData);

        // Statutory Deductions Section
        addStatutoryDeductionsSection(document, payrollRecord, ytdData);

        // Net Pay Section
        addNetPaySection(document, payrollRecord);

        // Footer Section
        addFooter(document, employee, payrollRecord);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, Employee employee, PayrollRecord payrollRecord) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{40, 20, 40});

        // Left Column - Company Info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);

        Paragraph companyCode = new Paragraph("Company Code: K5/MEL 25841619", SMALL_FONT);
        Paragraph locDept = new Paragraph("Loc/Dept: 01/", SMALL_FONT);
        Paragraph number = new Paragraph("Number: " + (payrollRecord.getId() != null ? String.format("%05d", payrollRecord.getId()) : "50803"), SMALL_FONT);
        Paragraph page = new Paragraph("Page: 1 of 1", SMALL_FONT);
        Paragraph companyName = new Paragraph("Ingenious Heads LLC", BOLD_FONT);
        Paragraph companyAddress = new Paragraph("21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279", SMALL_FONT);

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

        Paragraph earningsTitle = new Paragraph("Earnings Statement", TITLE_FONT);
        earningsTitle.setAlignment(Element.ALIGN_CENTER);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph periodStart = new Paragraph("Period Starting: " + payrollRecord.getPayPeriodStart().format(dateFormatter), SMALL_FONT);
        Paragraph periodEnd = new Paragraph("Period Ending: " + payrollRecord.getPayPeriodEnd().format(dateFormatter), SMALL_FONT);
        Paragraph payDate = new Paragraph("Pay Date: " + payrollRecord.getPayDate().format(dateFormatter), SMALL_FONT);
        Paragraph employeeName = new Paragraph(
            (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : ""),
            BOLD_FONT
        );
        Paragraph employeeAddress = new Paragraph(
            employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getAddress() != null ?
                employee.getEmployeeDetails().getAddress() : "152 Pampano Ln, Saint Charles, MO 63301",
            SMALL_FONT
        );

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

    private void addEarningsSection(Document document, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable earningsTable = new PdfPTable(5);
        earningsTable.setWidthPercentage(100);
        earningsTable.setWidths(new float[]{30, 15, 20, 17.5, 17.5});

        // Header
        addTableHeader(earningsTable, "Earnings");
        addTableHeader(earningsTable, "rate");
        addTableHeader(earningsTable, "hours/units");
        addTableHeader(earningsTable, "this period");
        addTableHeader(earningsTable, "year to date");

        // Regular Earnings Row
        addTableCell(earningsTable, "Regular", NORMAL_FONT);
        addTableCell(earningsTable, "", NORMAL_FONT);
        addTableCell(earningsTable, "0.00", NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(payrollRecord.getGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Gross Pay Summary Row
        PdfPCell summaryCell = new PdfPCell(new Phrase("Gross Pay", BOLD_FONT));
        summaryCell.setColspan(3);
        summaryCell.setBorder(Rectangle.NO_BORDER);
        earningsTable.addCell(summaryCell);
        addTableCell(earningsTable, formatCurrency(payrollRecord.getGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);

        document.add(earningsTable);
        document.add(new Paragraph(" "));
    }

    private void addStatutoryDeductionsSection(Document document, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable deductionsTable = new PdfPTable(3);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setWidths(new float[]{50, 25, 25});

        // Header
        addTableHeader(deductionsTable, "Statutory Deductions");
        addTableHeader(deductionsTable, "this period");
        addTableHeader(deductionsTable, "year to date");

        // Federal Income
        addDeductionRow(deductionsTable, "Federal Income", payrollRecord.getFederalTax(), 
            ytdData != null ? ytdData.getYtdFederalTax() : null);

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

        // State Tax (show even if 0.00)
        if (payrollRecord.getStateTax() != null) {
            String stateTaxName = payrollRecord.getStateTaxName() != null ? 
                payrollRecord.getStateTaxName() : "State Income";
            addDeductionRow(deductionsTable, stateTaxName, payrollRecord.getStateTax(),
                ytdData != null ? ytdData.getYtdStateTax() : null);
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
                    com.fasterxml.jackson.core.type.TypeReference.forType(Map.class));
                
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

        // Net Pay Row
        PdfPCell netPayLabel = new PdfPCell(new Phrase("Net Pay", BOLD_FONT));
        netPayLabel.setBorder(Rectangle.NO_BORDER);
        deductionsTable.addCell(netPayLabel);
        
        PdfPCell netPayValue = new PdfPCell(new Phrase(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT));
        netPayValue.setBorder(Rectangle.NO_BORDER);
        netPayValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        deductionsTable.addCell(netPayValue);
        
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(Rectangle.NO_BORDER);
        deductionsTable.addCell(emptyCell);

        document.add(deductionsTable);
        document.add(new Paragraph(" "));
    }

    private void addNetPaySection(Document document, PayrollRecord payrollRecord) throws DocumentException {
        // Empty space
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
    }

    private void addFooter(Document document, Employee employee, PayrollRecord payrollRecord) throws DocumentException {
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{50, 50});

        // Left Column
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph federalTaxable = new Paragraph(
            "Your federal taxable wages this period are " + formatCurrency(payrollRecord.getGrossPay()),
            SMALL_FONT
        );
        Paragraph companyInfo = new Paragraph(
            "Ingenious Heads LLC, 21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279",
            SMALL_FONT
        );

        leftCell.addElement(federalTaxable);
        leftCell.addElement(new Paragraph(" "));
        leftCell.addElement(companyInfo);

        // Right Column
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph checkNumber = new Paragraph("68-426/514", SMALL_FONT);
        Paragraph payrollCheck = new Paragraph(
            "Payroll Check Number: " + (payrollRecord.getId() != null ? String.format("%05d", payrollRecord.getId()) : "50803"),
            SMALL_FONT
        );
        Paragraph payDate = new Paragraph(
            "Pay Date: " + payrollRecord.getPayDate().format(dateFormatter),
            SMALL_FONT
        );
        Paragraph payToOrder = new Paragraph(
            "Pay to the order of: " + (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : ""),
            SMALL_FONT
        );
        Paragraph amountWords = new Paragraph(
            "This amount: " + numberToWords(payrollRecord.getNetPay()),
            SMALL_FONT
        );
        
        Paragraph netPayBox = new Paragraph(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT);
        netPayBox.setAlignment(Element.ALIGN_RIGHT);

        rightCell.addElement(checkNumber);
        rightCell.addElement(payrollCheck);
        rightCell.addElement(payDate);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(payToOrder);
        rightCell.addElement(amountWords);
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(netPayBox);

        footerTable.addCell(leftCell);
        footerTable.addCell(rightCell);

        document.add(footerTable);

        // Watermark
        Paragraph watermark = new Paragraph("NOT A CHECK", SMALL_FONT);
        watermark.setAlignment(Element.ALIGN_CENTER);
        document.add(watermark);
        
        Paragraph voidText = new Paragraph("VOID - NON NEGOTIABLE", SMALL_FONT);
        voidText.setAlignment(Element.ALIGN_CENTER);
        document.add(voidText);
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
            "-" + formatCurrency(thisPeriod) : "0.00";
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

