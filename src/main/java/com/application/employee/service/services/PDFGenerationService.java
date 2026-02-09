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
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PDFGenerationService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font SMALL_BOLD_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);

    public byte[] generateADPPaystub(PayrollRecord payrollRecord, Employee employee, YTDData ytdData) throws IOException, DocumentException {
        Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // Header Section
        addHeader(document, employee, payrollRecord);

        // Add horizontal line separator
        addHorizontalLine(document);

        // Tax Filing Status and Earnings Section
        addTaxAndEarningsSection(document, employee, payrollRecord, ytdData);

        // Add horizontal line separator
        addHorizontalLine(document);

        // Statutory Deductions Section (includes Net Pay)
        addStatutoryDeductionsSection(document, payrollRecord, ytdData);

        // Voluntary Deductions Section (custom deductions from PDF)
        addVoluntaryDeductionsSection(document, payrollRecord, ytdData);

        // Federal Taxable Wages (above the line)
        addFederalTaxableWages(document, payrollRecord);

        // Add horizontal line separator
        addHorizontalLine(document);

        // Footer/Check Section
        addCheckSection(document, employee, payrollRecord, writer);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, Employee employee, PayrollRecord payrollRecord) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{35f, 30f, 35f});
        headerTable.setSpacingAfter(10);

        // Left Column - Company Info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);
        leftCell.setPaddingBottom(5);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);

        String companyNameStr = "Ingenious Heads LLC";
        String companyAddressStr = "21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279";
        
        if (employee.getCompany() != null) {
            companyNameStr = employee.getCompany().getCompanyName() != null ? 
                employee.getCompany().getCompanyName() : companyNameStr;
            companyAddressStr = getCompanyAddress(employee.getCompany());
        }

        Paragraph companyCode = new Paragraph("Company Code: K5/MEL 25841619", SMALL_FONT);
        Paragraph locDept = new Paragraph("Loc/Dept: 01/", SMALL_FONT);
        Paragraph number = new Paragraph("Number: " + (payrollRecord.getId() != null ? String.format("%05d", payrollRecord.getId()) : "00001"), SMALL_FONT);
        Paragraph page = new Paragraph("Page: 1 of 1", SMALL_FONT);
        Paragraph companyName = new Paragraph(companyNameStr, BOLD_FONT);
        Paragraph companyAddress = new Paragraph(companyAddressStr, SMALL_FONT);

        leftCell.addElement(companyCode);
        leftCell.addElement(locDept);
        leftCell.addElement(number);
        leftCell.addElement(page);
        leftCell.addElement(new Paragraph(" ", SMALL_FONT));
        leftCell.addElement(companyName);
        leftCell.addElement(companyAddress);

        // Center Column - Empty (for spacing)
        PdfPCell centerCell = new PdfPCell();
        centerCell.setBorder(Rectangle.NO_BORDER);

        // Right Column - Employee Info and Dates
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setPaddingBottom(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph earningsTitle = new Paragraph("Earnings Statement", TITLE_FONT);
        earningsTitle.setAlignment(Element.ALIGN_RIGHT);
        earningsTitle.setSpacingAfter(5);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph periodStart = new Paragraph("Period Starting: " + payrollRecord.getPayPeriodStart().format(dateFormatter), SMALL_FONT);
        periodStart.setAlignment(Element.ALIGN_RIGHT);
        Paragraph periodEnd = new Paragraph("Period Ending: " + payrollRecord.getPayPeriodEnd().format(dateFormatter), SMALL_FONT);
        periodEnd.setAlignment(Element.ALIGN_RIGHT);
        Paragraph payDate = new Paragraph("Pay Date: " + payrollRecord.getPayDate().format(dateFormatter), SMALL_FONT);
        payDate.setAlignment(Element.ALIGN_RIGHT);
        
        String fullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : "");
        Paragraph employeeName = new Paragraph(fullName.trim(), BOLD_FONT);
        employeeName.setAlignment(Element.ALIGN_RIGHT);
        employeeName.setSpacingBefore(5);
        
        String empAddress = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getResidentialAddress() != null ?
            employee.getEmployeeDetails().getResidentialAddress() : "152 Pampano Ln, Saint Charles, MO 63301";
        Paragraph employeeAddress = new Paragraph(empAddress, SMALL_FONT);
        employeeAddress.setAlignment(Element.ALIGN_RIGHT);

        rightCell.addElement(earningsTitle);
        rightCell.addElement(periodStart);
        rightCell.addElement(periodEnd);
        rightCell.addElement(payDate);
        rightCell.addElement(employeeName);
        rightCell.addElement(employeeAddress);

        headerTable.addCell(leftCell);
        headerTable.addCell(centerCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private void addTaxAndEarningsSection(Document document, Employee employee, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{30f, 40f, 30f});
        mainTable.setSpacingAfter(10);

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
        ssnPara.setSpacingBefore(5);

        leftCell.addElement(taxFilingTitle);
        leftCell.addElement(exemptionsTitle);
        leftCell.addElement(federalExempt);
        leftCell.addElement(stateExempt);
        leftCell.addElement(localExempt);
        leftCell.addElement(ssnPara);

        // Center Column - Tax Override
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

        // Right Column - Important Notes
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph importantNotesTitle = new Paragraph("Important Notes", SMALL_BOLD_FONT);
        Paragraph basisOfPay = new Paragraph("Basis of pay: Salaried", SMALL_FONT);
        basisOfPay.setSpacingBefore(5);

        rightCell.addElement(importantNotesTitle);
        rightCell.addElement(basisOfPay);

        mainTable.addCell(leftCell);
        mainTable.addCell(centerCell);
        mainTable.addCell(rightCell);

        document.add(mainTable);

        // Earnings Table - Full width, below the three columns
        PdfPTable earningsTable = new PdfPTable(5);
        earningsTable.setWidthPercentage(100);
        earningsTable.setWidths(new float[]{25f, 15f, 18f, 21f, 21f});
        earningsTable.setSpacingBefore(5);
        earningsTable.setSpacingAfter(10);

        // Headers with bottom border
        PdfPCell earningsHeader = new PdfPCell(new Phrase("Earnings", SMALL_BOLD_FONT));
        earningsHeader.setBorder(Rectangle.BOTTOM);
        earningsHeader.setBorderWidth(0.5f);
        earningsHeader.setPadding(5);
        earningsHeader.setHorizontalAlignment(Element.ALIGN_LEFT);
        earningsTable.addCell(earningsHeader);
        
        PdfPCell rateHeader = new PdfPCell(new Phrase("rate", SMALL_BOLD_FONT));
        rateHeader.setBorder(Rectangle.BOTTOM);
        rateHeader.setBorderWidth(0.5f);
        rateHeader.setPadding(5);
        rateHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        earningsTable.addCell(rateHeader);
        
        PdfPCell hoursHeader = new PdfPCell(new Phrase("hours/units", SMALL_BOLD_FONT));
        hoursHeader.setBorder(Rectangle.BOTTOM);
        hoursHeader.setBorderWidth(0.5f);
        hoursHeader.setPadding(5);
        hoursHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        earningsTable.addCell(hoursHeader);
        
        PdfPCell thisPeriodHeader = new PdfPCell(new Phrase("this period", SMALL_BOLD_FONT));
        thisPeriodHeader.setBorder(Rectangle.BOTTOM);
        thisPeriodHeader.setBorderWidth(0.5f);
        thisPeriodHeader.setPadding(5);
        thisPeriodHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        earningsTable.addCell(thisPeriodHeader);
        
        PdfPCell ytdHeader = new PdfPCell(new Phrase("year to date", SMALL_BOLD_FONT));
        ytdHeader.setBorder(Rectangle.BOTTOM);
        ytdHeader.setBorderWidth(0.5f);
        ytdHeader.setPadding(5);
        ytdHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        earningsTable.addCell(ytdHeader);

        // Regular Earnings Row - rate should be blank for salaried
        addTableCell(earningsTable, "Regular", NORMAL_FONT);
        addTableCell(earningsTable, "", NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, "0.00", NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrencyNoDollar(payrollRecord.getGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrencyNoDollar(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Gross Pay Summary Row
        PdfPCell summaryCell = new PdfPCell(new Phrase("Gross Pay", BOLD_FONT));
        summaryCell.setColspan(3);
        summaryCell.setBorder(Rectangle.NO_BORDER);
        summaryCell.setPadding(5);
        summaryCell.setPaddingTop(8);
        earningsTable.addCell(summaryCell);
        addTableCell(earningsTable, formatCurrency(payrollRecord.getGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);
        addTableCell(earningsTable, formatCurrency(ytdData != null ? ytdData.getYtdGrossPay() : payrollRecord.getYtdGrossPay()), BOLD_FONT, Element.ALIGN_RIGHT);

        document.add(earningsTable);
    }

    private void addStatutoryDeductionsSection(Document document, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        PdfPTable deductionsTable = new PdfPTable(3);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setWidths(new float[]{50f, 25f, 25f});
        deductionsTable.setSpacingAfter(5);

        // Header with bottom border
        PdfPCell statDedHeader = new PdfPCell(new Phrase("Statutory Deductions", SMALL_BOLD_FONT));
        statDedHeader.setBorder(Rectangle.BOTTOM);
        statDedHeader.setBorderWidth(0.5f);
        statDedHeader.setPadding(5);
        statDedHeader.setHorizontalAlignment(Element.ALIGN_LEFT);
        deductionsTable.addCell(statDedHeader);
        
        PdfPCell statThisPeriodHeader = new PdfPCell(new Phrase("this period", SMALL_BOLD_FONT));
        statThisPeriodHeader.setBorder(Rectangle.BOTTOM);
        statThisPeriodHeader.setBorderWidth(0.5f);
        statThisPeriodHeader.setPadding(5);
        statThisPeriodHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        deductionsTable.addCell(statThisPeriodHeader);
        
        PdfPCell statYtdHeader = new PdfPCell(new Phrase("year to date", SMALL_BOLD_FONT));
        statYtdHeader.setBorder(Rectangle.BOTTOM);
        statYtdHeader.setBorderWidth(0.5f);
        statYtdHeader.setPadding(5);
        statYtdHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        deductionsTable.addCell(statYtdHeader);

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

        // State Tax
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

        // Net Pay Row - part of the deductions table
        PdfPCell netPayLabel = new PdfPCell(new Phrase("Net Pay", BOLD_FONT));
        netPayLabel.setBorder(Rectangle.NO_BORDER);
        netPayLabel.setPadding(5);
        netPayLabel.setPaddingTop(8);
        deductionsTable.addCell(netPayLabel);
        
        PdfPCell netPayValue = new PdfPCell(new Phrase(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT));
        netPayValue.setBorder(Rectangle.NO_BORDER);
        netPayValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netPayValue.setPadding(5);
        netPayValue.setPaddingTop(8);
        deductionsTable.addCell(netPayValue);
        
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(Rectangle.NO_BORDER);
        deductionsTable.addCell(emptyCell);

        document.add(deductionsTable);
    }

    private void addVoluntaryDeductionsSection(Document document, PayrollRecord payrollRecord, YTDData ytdData) throws DocumentException {
        // Only create this section if there are custom deductions
        if (payrollRecord.getCustomDeductionsJson() == null || payrollRecord.getCustomDeductionsJson().trim().isEmpty()) {
            return;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> customDeductions = mapper.readValue(payrollRecord.getCustomDeductionsJson(), 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            if (customDeductions == null || customDeductions.isEmpty()) {
                return;
            }

            PdfPTable voluntaryTable = new PdfPTable(3);
            voluntaryTable.setWidthPercentage(100);
            voluntaryTable.setWidths(new float[]{50f, 25f, 25f});
            voluntaryTable.setSpacingBefore(5);
            voluntaryTable.setSpacingAfter(5);

            // Header with bottom border
            PdfPCell volDedHeader = new PdfPCell(new Phrase("Voluntary Deductions", SMALL_BOLD_FONT));
            volDedHeader.setBorder(Rectangle.BOTTOM);
            volDedHeader.setBorderWidth(0.5f);
            volDedHeader.setPadding(5);
            volDedHeader.setHorizontalAlignment(Element.ALIGN_LEFT);
            voluntaryTable.addCell(volDedHeader);
            
            PdfPCell volThisPeriodHeader = new PdfPCell(new Phrase("this period", SMALL_BOLD_FONT));
            volThisPeriodHeader.setBorder(Rectangle.BOTTOM);
            volThisPeriodHeader.setBorderWidth(0.5f);
            volThisPeriodHeader.setPadding(5);
            volThisPeriodHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            voluntaryTable.addCell(volThisPeriodHeader);
            
            PdfPCell volYtdHeader = new PdfPCell(new Phrase("year to date", SMALL_BOLD_FONT));
            volYtdHeader.setBorder(Rectangle.BOTTOM);
            volYtdHeader.setBorderWidth(0.5f);
            volYtdHeader.setPadding(5);
            volYtdHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            voluntaryTable.addCell(volYtdHeader);

            // Process each custom deduction
            for (Map.Entry<String, Object> entry : customDeductions.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String name = key;
                BigDecimal thisPeriodAmount = BigDecimal.ZERO;
                BigDecimal ytdAmount = null;
                
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldData = (Map<String, Object>) value;
                    name = fieldData.containsKey("name") ? (String) fieldData.get("name") : key;
                    
                    // Get this period value
                    if (fieldData.containsKey("value")) {
                        Object val = fieldData.get("value");
                        if (val instanceof Number) {
                            thisPeriodAmount = BigDecimal.valueOf(((Number) val).doubleValue()).abs();
                        } else if (val instanceof String) {
                            try {
                                thisPeriodAmount = new BigDecimal((String) val).abs();
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    // Get YTD value if available
                    if (fieldData.containsKey("ytd") || fieldData.containsKey("yearToDate")) {
                        Object ytdVal = fieldData.containsKey("ytd") ? fieldData.get("ytd") : fieldData.get("yearToDate");
                        if (ytdVal instanceof Number) {
                            ytdAmount = BigDecimal.valueOf(((Number) ytdVal).doubleValue()).abs();
                        } else if (ytdVal instanceof String) {
                            try {
                                ytdAmount = new BigDecimal((String) ytdVal).abs();
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                } else if (value instanceof Number) {
                    thisPeriodAmount = BigDecimal.valueOf(((Number) value).doubleValue()).abs();
                } else if (value instanceof String) {
                    try {
                        thisPeriodAmount = new BigDecimal((String) value).abs();
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                
                // Add row if there's a value (even if 0, to match ADP format)
                addDeductionRow(voluntaryTable, name, thisPeriodAmount, ytdAmount);
            }

            document.add(voluntaryTable);
        } catch (Exception e) {
            // Log error but don't fail the PDF generation
            System.err.println("Error processing voluntary deductions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addFederalTaxableWages(Document document, PayrollRecord payrollRecord) throws DocumentException {
        Paragraph federalTaxable = new Paragraph(
            "Your federal taxable wages this period are " + formatCurrency(payrollRecord.getGrossPay()),
            SMALL_FONT
        );
        federalTaxable.setSpacingAfter(8);
        document.add(federalTaxable);
    }

    private void addCheckSection(Document document, Employee employee, PayrollRecord payrollRecord, PdfWriter writer) throws DocumentException {
        // Add some space before check section
        document.add(new Paragraph(" ", SMALL_FONT));
        
        // Get current Y position before adding check content for watermark
        float checkSectionStartY = writer.getVerticalPosition(false);
        
        // Add watermark first (behind the content) - "THIS IS NOT A CHECK" and "VOID - NON NEGOTIABLE"
        PdfContentByte canvas = writer.getDirectContentUnder();
        Font watermarkFont1 = new Font(Font.HELVETICA, 40, Font.BOLD, new Color(200, 200, 200));
        Font watermarkFont2 = new Font(Font.HELVETICA, 30, Font.BOLD, new Color(200, 200, 200));
        
        float pageWidth = document.right() - document.left();
        float watermarkX = pageWidth / 2f + document.leftMargin();
        float watermarkY = checkSectionStartY - 20f;
        
        Phrase watermark1 = new Phrase("THIS IS NOT A CHECK", watermarkFont1);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark1,
            watermarkX, watermarkY, 0f);
        
        Phrase watermark2 = new Phrase("VOID - NON NEGOTIABLE", watermarkFont2);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark2,
            watermarkX, watermarkY - 30f, 0f);

        // Top section: Company info on left, Federal taxable wages on right
        PdfPTable topTable = new PdfPTable(2);
        topTable.setWidthPercentage(100);
        topTable.setWidths(new float[]{50f, 50f});
        topTable.setSpacingAfter(10);

        String companyNameStr = "Ingenious Heads LLC";
        String companyAddressStr = "21135 Whitfield Pl Ste 207, Sterling, VA 20165-7279";
        
        if (employee.getCompany() != null) {
            companyNameStr = employee.getCompany().getCompanyName() != null ? 
                employee.getCompany().getCompanyName() : companyNameStr;
            companyAddressStr = getCompanyAddress(employee.getCompany());
        }

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setPadding(5);
        companyCell.setPaddingTop(15);
        companyCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph companyName = new Paragraph(companyNameStr, SMALL_FONT);
        Paragraph companyAddr = new Paragraph(companyAddressStr, SMALL_FONT);
        companyCell.addElement(companyName);
        companyCell.addElement(companyAddr);

        PdfPCell taxableCell = new PdfPCell();
        taxableCell.setBorder(Rectangle.NO_BORDER);
        taxableCell.setPadding(5);
        taxableCell.setPaddingTop(15);
        taxableCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        taxableCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph taxableWages = new Paragraph(
            "Your federal taxable wages this period are " + formatCurrency(payrollRecord.getGrossPay()),
            SMALL_FONT
        );
        taxableCell.addElement(taxableWages);

        topTable.addCell(companyCell);
        topTable.addCell(taxableCell);
        document.add(topTable);

        // Middle section: Pay to order, amount in words, amount in numbers
        PdfPTable middleTable = new PdfPTable(2);
        middleTable.setWidthPercentage(100);
        middleTable.setWidths(new float[]{60f, 40f});
        middleTable.setSpacingAfter(10);

        String fullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
            (employee.getLastName() != null ? employee.getLastName() : "");

        PdfPCell payToCell = new PdfPCell();
        payToCell.setBorder(Rectangle.NO_BORDER);
        payToCell.setPadding(5);
        payToCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph payToOrder = new Paragraph("Pay to the order of: " + fullName.trim(), SMALL_FONT);
        payToOrder.setSpacingAfter(8);
        Paragraph amountWords = new Paragraph(numberToWords(payrollRecord.getNetPay()), SMALL_FONT);
        amountWords.setSpacingAfter(5);
        payToCell.addElement(payToOrder);
        payToCell.addElement(amountWords);

        PdfPCell amountCell = new PdfPCell();
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setPadding(5);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph netPayAmount = new Paragraph(formatCurrency(payrollRecord.getNetPay()), BOLD_FONT);
        netPayAmount.setAlignment(Element.ALIGN_RIGHT);
        amountCell.addElement(netPayAmount);

        middleTable.addCell(payToCell);
        middleTable.addCell(amountCell);
        document.add(middleTable);

        // Bottom section: Routing number top right, check details, bank name bottom left, recipient address bottom middle
        PdfPTable bottomTable = new PdfPTable(3);
        bottomTable.setWidthPercentage(100);
        bottomTable.setWidths(new float[]{33f, 34f, 33f});
        bottomTable.setSpacingAfter(10);

        // Left column - Bank name
        PdfPCell bankCell = new PdfPCell();
        bankCell.setBorder(Rectangle.NO_BORDER);
        bankCell.setPadding(5);
        bankCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        String bankName = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getBankName() != null ?
            employee.getEmployeeDetails().getBankName() : "Chase";
        Paragraph bankNamePara = new Paragraph(bankName, SMALL_FONT);
        bankCell.addElement(bankNamePara);

        // Middle column - Recipient address
        PdfPCell addressCell = new PdfPCell();
        addressCell.setBorder(Rectangle.NO_BORDER);
        addressCell.setPadding(5);
        addressCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        String empAddress = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getResidentialAddress() != null ?
            employee.getEmployeeDetails().getResidentialAddress() : "152 Pampano Ln, Saint Charles, MO 63301";
        Paragraph recipientName = new Paragraph(fullName.trim(), SMALL_FONT);
        Paragraph recipientAddr = new Paragraph(empAddress, SMALL_FONT);
        addressCell.addElement(recipientName);
        addressCell.addElement(recipientAddr);

        // Right column - Routing number (top), check number and pay date
        PdfPCell checkDetailsCell = new PdfPCell();
        checkDetailsCell.setBorder(Rectangle.NO_BORDER);
        checkDetailsCell.setPadding(5);
        checkDetailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        checkDetailsCell.setVerticalAlignment(Element.ALIGN_TOP);
        
        String routingNumber = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getRoutingNumber() != null ?
            employee.getEmployeeDetails().getRoutingNumber() : "68-426";
        String accountNumber = employee.getEmployeeDetails() != null && employee.getEmployeeDetails().getAccNumber() != null ?
            maskAccountNumber(employee.getEmployeeDetails().getAccNumber()) : "514";
        String bankInfo = routingNumber + "/" + accountNumber;
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph bankRouting = new Paragraph(bankInfo, SMALL_FONT);
        bankRouting.setAlignment(Element.ALIGN_RIGHT);
        bankRouting.setSpacingAfter(5);
        Paragraph payrollCheck = new Paragraph(
            "Payroll Check Number: " + (payrollRecord.getCheckNumber() != null ? String.format("%05d", payrollRecord.getCheckNumber()) : "00001"),
            SMALL_FONT
        );
        payrollCheck.setAlignment(Element.ALIGN_RIGHT);
        Paragraph payDate = new Paragraph(
            "Pay Date: " + payrollRecord.getPayDate().format(dateFormatter),
            SMALL_FONT
        );
        payDate.setAlignment(Element.ALIGN_RIGHT);
        
        checkDetailsCell.addElement(bankRouting);
        checkDetailsCell.addElement(payrollCheck);
        checkDetailsCell.addElement(payDate);

        bottomTable.addCell(bankCell);
        bottomTable.addCell(addressCell);
        bottomTable.addCell(checkDetailsCell);
        document.add(bottomTable);
    }

    private void addHorizontalLine(Document document) throws DocumentException {
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidth(0.5f);
        lineCell.setBorderColor(Color.BLACK);
        lineCell.setFixedHeight(1f);
        lineCell.setPadding(0);
        lineTable.addCell(lineCell);
        lineTable.setSpacingAfter(8);
        document.add(lineTable);
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
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        addTableCell(table, text, font, Element.ALIGN_LEFT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        // For right-aligned cells (amounts), ensure consistent padding for proper alignment
        if (alignment == Element.ALIGN_RIGHT) {
            cell.setPaddingRight(8);
            cell.setPaddingLeft(5);
        }
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addDeductionRow(PdfPTable table, String name, BigDecimal thisPeriod, BigDecimal ytd) {
        addTableCell(table, name, NORMAL_FONT);
        String thisPeriodStr = thisPeriod != null && thisPeriod.compareTo(BigDecimal.ZERO) >= 0 ?
            formatCurrencyNoDollar(thisPeriod) : "0.00";
        // Show negative for deductions
        if (!thisPeriodStr.startsWith("-") && thisPeriod != null && thisPeriod.compareTo(BigDecimal.ZERO) > 0) {
            thisPeriodStr = "-" + thisPeriodStr;
        }
        addTableCell(table, thisPeriodStr, NORMAL_FONT, Element.ALIGN_RIGHT);
        String ytdStr = ytd != null ? formatCurrencyNoDollar(ytd) : "0.00";
        addTableCell(table, ytdStr, NORMAL_FONT, Element.ALIGN_RIGHT);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount);
    }

    private String formatCurrencyNoDollar(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
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
