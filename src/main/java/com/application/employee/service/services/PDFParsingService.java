package com.application.employee.service.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PDFParsingService {

    public Map<String, Object> parsePayrollPDF(MultipartFile file) throws IOException {
        Map<String, Object> extracted = new HashMap<>();
        extracted.put("totalGrossPay", null);
        extracted.put("totalNetPay", null);
        extracted.put("federalTaxWithheld", null);
        extracted.put("stateTaxWithheld", null);
        extracted.put("stateTaxName", null);
        extracted.put("localTaxWithheld", null);
        extracted.put("socialSecurityWithheld", null);
        extracted.put("medicareWithheld", null);
        extracted.put("periodStartDate", null);
        extracted.put("periodEndDate", null);
        extracted.put("additionalFields", new HashMap<String, Object>());

        try {
            byte[] pdfBytes = file.getInputStream().readAllBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                // Extract standard fields
                extractStandardFields(text, extracted);

                // Extract dates
                extractDates(text, extracted);

                // Extract additional/custom deductions dynamically
                extractCustomDeductions(text, extracted);
            }
        } catch (IOException e) {
            throw new IOException("Error parsing PDF file: " + e.getMessage(), e);
        }

        return extracted;
    }

    private void extractStandardFields(String text, Map<String, Object> extracted) {
        String[] lines = text.split("\n");
        
        // Extract from table format with "this period" and "year to date" columns
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();
            
            // Extract Gross Pay from earnings section
            if (lowerLine.contains("gross pay") && !extracted.containsKey("totalGrossPay")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("totalGrossPay", value);
                }
            }
            
            // Extract Net Pay
            if (lowerLine.contains("net pay") && !extracted.containsKey("totalNetPay")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("totalNetPay", value);
                }
            }
            
            // Extract Federal Tax
            if ((lowerLine.contains("federal income") || lowerLine.contains("federal tax")) && 
                !extracted.containsKey("federalTaxWithheld")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("federalTaxWithheld", value);
                }
            }
            
            // Extract State Tax with state name
            if ((lowerLine.contains("state income") || lowerLine.contains("california state income") || 
                 lowerLine.contains("illinois state income") || lowerLine.contains("new jersey state income")) &&
                !extracted.containsKey("stateTaxWithheld")) {
                // Extract state name
                Pattern stateNamePattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+State\\s+Income", Pattern.CASE_INSENSITIVE);
                Matcher stateNameMatcher = stateNamePattern.matcher(line);
                if (stateNameMatcher.find()) {
                    extracted.put("stateTaxName", stateNameMatcher.group(1) + " State Income");
                }
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("stateTaxWithheld", value);
                }
            }
            
            // Extract Local Tax
            if (lowerLine.contains("local tax") && !extracted.containsKey("localTaxWithheld")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("localTaxWithheld", value);
                }
            }
            
            // Extract Social Security
            if (lowerLine.contains("social security") && !extracted.containsKey("socialSecurityWithheld")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("socialSecurityWithheld", value);
                }
            }
            
            // Extract Medicare
            if (lowerLine.contains("medicare") && !lowerLine.contains("additional") && 
                !extracted.containsKey("medicareWithheld")) {
                BigDecimal value = extractValueFromThisPeriodColumn(line);
                if (value != null) {
                    extracted.put("medicareWithheld", value);
                }
            }
        }
    }
    
    private BigDecimal extractValueFromThisPeriodColumn(String line) {
        // Look for pattern: "this period" followed by value, or value in "this period" column
        // Handle both negative and positive values
        Pattern pattern = Pattern.compile("this\\s*period[\\s:]*(-?[\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return parseDecimal(matcher.group(1).replace("-", ""));
        }
        
        // Alternative: look for value before "year to date"
        pattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)\\s+year\\s+to\\s+date", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(line);
        if (matcher.find()) {
            return parseDecimal(matcher.group(1).replace("-", ""));
        }
        
        // Fallback: extract any decimal value (for simpler formats)
        pattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
        matcher = pattern.matcher(line);
        if (matcher.find()) {
            return parseDecimal(matcher.group(1).replace("-", ""));
        }
        
        return null;
    }

    private void extractDates(String text, Map<String, Object> extracted) {
        // Extract Period Start Date
        Pattern periodStartPattern = Pattern.compile("period\\s*start(?:ing)?[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE);
        Matcher periodStartMatcher = periodStartPattern.matcher(text);
        if (periodStartMatcher.find()) {
            extracted.put("periodStartDate", parseDate(periodStartMatcher.group(1)));
        }

        // Extract Period End Date
        Pattern periodEndPattern = Pattern.compile("period\\s*end(?:ing)?[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE);
        Matcher periodEndMatcher = periodEndPattern.matcher(text);
        if (periodEndMatcher.find()) {
            extracted.put("periodEndDate", parseDate(periodEndMatcher.group(1)));
        }
    }

    private void extractCustomDeductions(String text, Map<String, Object> extracted) {
        @SuppressWarnings("unchecked")
        Map<String, Object> additionalFields = (Map<String, Object>) extracted.get("additionalFields");
        
        String[] lines = text.split("\n");
        boolean inDeductionsSection = false;
        Set<String> standardFields = new HashSet<>(Arrays.asList(
            "federal income", "federal tax", "social security", "medicare",
            "gross pay", "net pay", "state income", "california state income",
            "illinois state income", "local tax"
        ));

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();

            // Detect deductions section
            if (lowerLine.contains("statutory deductions") || lowerLine.contains("voluntary deductions")) {
                inDeductionsSection = true;
                continue;
            }

            // Stop if we hit net pay or other sections
            if (lowerLine.contains("net pay") || lowerLine.contains("important notes") || 
                lowerLine.contains("basis of pay") || lowerLine.contains("federal taxable")) {
                inDeductionsSection = false;
                continue;
            }

            if (inDeductionsSection && !line.isEmpty()) {
                // Check if it's a standard field
                boolean isStandard = false;
                for (String standard : standardFields) {
                    if (lowerLine.contains(standard)) {
                        isStandard = true;
                        break;
                    }
                }
                
                if (!isStandard) {
                    // Extract deduction name (everything before the first number)
                    Pattern namePattern = Pattern.compile("^([A-Za-z\\s]+?)(?=\\s+-?[\\d,])", Pattern.CASE_INSENSITIVE);
                    Matcher nameMatcher = namePattern.matcher(line);
                    
                    if (nameMatcher.find()) {
                        String deductionName = nameMatcher.group(1).trim();
                        
                        // Extract amount from "this period" column
                        BigDecimal amount = extractValueFromThisPeriodColumn(line);
                        
                        if (deductionName.length() > 2 && amount != null && amount.compareTo(BigDecimal.ZERO) >= 0) {
                            // Create a clean key
                            String key = deductionName.toLowerCase()
                                .replaceAll("[^a-z0-9\\s]", "")
                                .replaceAll("\\s+", "_")
                                .substring(0, Math.min(50, deductionName.length()));
                            
                            // Only add if not already exists or if this is a better match
                            if (!additionalFields.containsKey(key) || 
                                (additionalFields.containsKey(key) && amount.compareTo(BigDecimal.ZERO) > 0)) {
                                Map<String, Object> fieldData = new HashMap<>();
                                fieldData.put("name", deductionName);
                                fieldData.put("value", amount);
                                additionalFields.put(key, fieldData);
                            }
                        }
                    }
                }
            }
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = value.replace(",", "").trim();
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            String[] formats = {"MM/dd/yyyy", "MM/dd/yy", "M/d/yyyy", "M/d/yy"};
            for (String format : formats) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    // If 2-digit year, assume 2000s
                    if (format.contains("yy") && !format.contains("yyyy")) {
                        int year = date.getYear();
                        if (year < 100) {
                            year += 2000;
                            date = date.withYear(year);
                        }
                    }
                    return date;
                } catch (Exception e) {
                    // Try next format
                }
            }
        } catch (Exception e) {
            // Return null if parsing fails
        }
        return null;
    }
}

