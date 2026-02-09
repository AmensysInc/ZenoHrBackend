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
import java.util.stream.Collectors;

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
                
                // Store raw text for debugging (first 2000 chars)
                if (text != null && text.length() > 0) {
                    extracted.put("_rawTextPreview", text.substring(0, Math.min(2000, text.length())));
                }

                // Extract standard fields
                extractStandardFields(text, extracted);

                // Extract dates
                extractDates(text, extracted);

                // Extract additional/custom deductions dynamically
                extractCustomDeductions(text, extracted);
            }
        } catch (IOException e) {
            throw new IOException("Error parsing PDF file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected error parsing PDF: " + e.getMessage(), e);
        }

        return extracted;
    }

    private void extractStandardFields(String text, Map<String, Object> extracted) {
        String[] lines = text.split("\n");
        
        // Extract from table format with "this period" and "year to date" columns
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();
            
            // Extract Gross Pay from earnings section - try multiple patterns
            if ((lowerLine.contains("gross pay") || (lowerLine.contains("gross") && lowerLine.contains("pay"))) 
                && extracted.get("totalGrossPay") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("totalGrossPay", value);
                }
            }
            
            // Extract Net Pay
            if ((lowerLine.contains("net pay") || (lowerLine.contains("net") && lowerLine.contains("pay"))) 
                && extracted.get("totalNetPay") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("totalNetPay", value);
                }
            }
            
            // Extract Federal Tax - multiple patterns
            if ((lowerLine.contains("federal income") || lowerLine.contains("federal tax") || 
                 lowerLine.contains("federal withholding") || (lowerLine.contains("federal") && lowerLine.contains("tax"))) 
                && extracted.get("federalTaxWithheld") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null) {
                    extracted.put("federalTaxWithheld", value);
                }
            }
            
            // Extract State Tax with state name
            if ((lowerLine.contains("state income") || lowerLine.contains("state tax") ||
                 lowerLine.contains("california") || lowerLine.contains("illinois") || 
                 lowerLine.contains("new jersey") || lowerLine.contains("texas") ||
                 lowerLine.contains("new york") || lowerLine.contains("florida")) &&
                extracted.get("stateTaxWithheld") == null) {
                // Extract state name
                Pattern stateNamePattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+State\\s+(?:Income|Tax)", Pattern.CASE_INSENSITIVE);
                Matcher stateNameMatcher = stateNamePattern.matcher(line);
                if (stateNameMatcher.find()) {
                    extracted.put("stateTaxName", stateNameMatcher.group(1) + " State Income");
                } else {
                    // Try simpler pattern
                    Pattern simpleStatePattern = Pattern.compile("(California|Illinois|New Jersey|Texas|New York|Florida|Arizona|Georgia|North Carolina|Washington)", Pattern.CASE_INSENSITIVE);
                    Matcher simpleMatcher = simpleStatePattern.matcher(line);
                    if (simpleMatcher.find()) {
                        extracted.put("stateTaxName", simpleMatcher.group(1) + " State Income");
                    }
                }
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null) {
                    extracted.put("stateTaxWithheld", value);
                }
            }
            
            // Extract Local Tax
            if (lowerLine.contains("local tax") && extracted.get("localTaxWithheld") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null) {
                    extracted.put("localTaxWithheld", value);
                }
            }
            
            // Extract Social Security - multiple patterns
            if ((lowerLine.contains("social security") || lowerLine.contains("ss tax") || 
                 lowerLine.contains("oasdi")) && extracted.get("socialSecurityWithheld") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null) {
                    extracted.put("socialSecurityWithheld", value);
                }
            }
            
            // Extract Medicare - exclude additional medicare
            if ((lowerLine.contains("medicare") || lowerLine.contains("med tax")) && 
                !lowerLine.contains("additional") && extracted.get("medicareWithheld") == null) {
                BigDecimal value = extractValueFromLine(line, i, lines);
                if (value != null) {
                    extracted.put("medicareWithheld", value);
                }
            }
        }
    }
    
    private BigDecimal extractValueFromLine(String line, int lineIndex, String[] allLines) {
        // Strategy 1: Look for "this period" followed by value
        Pattern pattern = Pattern.compile("this\\s*period[\\s:]*\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
            if (value != null) return value;
        }
        
        // Strategy 2: Look for value before "year to date" or "ytd"
        pattern = Pattern.compile("\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)\\s+(?:year\\s+to\\s+date|ytd)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(line);
        if (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
            if (value != null) return value;
        }
        
        // Strategy 3: Look for value in next line (common in table formats)
        if (lineIndex + 1 < allLines.length) {
            String nextLine = allLines[lineIndex + 1].trim();
            pattern = Pattern.compile("\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)");
            matcher = pattern.matcher(nextLine);
            if (matcher.find()) {
                BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
                if (value != null && value.compareTo(BigDecimal.ZERO) >= 0) return value;
            }
        }
        
        // Strategy 4: Extract all decimal values and take the first reasonable one
        pattern = Pattern.compile("\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)");
        matcher = pattern.matcher(line);
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
            if (value != null && value.compareTo(BigDecimal.ZERO) >= 0) {
                values.add(value);
            }
        }
        
        // If multiple values, prefer the larger one (usually the "this period" value is larger than YTD for first pay)
        if (!values.isEmpty()) {
            return values.stream().max(BigDecimal::compareTo).orElse(values.get(0));
        }
        
        return null;
    }
    
    // Keep old method for backward compatibility
    private BigDecimal extractValueFromThisPeriodColumn(String line) {
        return extractValueFromLine(line, 0, new String[]{line});
    }

    private void extractDates(String text, Map<String, Object> extracted) {
        // Extract Period Start Date - multiple patterns
        Pattern[] startPatterns = {
            Pattern.compile("period\\s*start(?:ing)?[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pay\\s*period\\s*start[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("from[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s*to", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : startPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                LocalDate date = parseDate(matcher.group(1));
                if (date != null) {
                    extracted.put("periodStartDate", date);
                    break;
                }
            }
        }

        // Extract Period End Date - multiple patterns
        Pattern[] endPatterns = {
            Pattern.compile("period\\s*end(?:ing)?[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pay\\s*period\\s*end[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("to[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s*pay\\s*date", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : endPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                LocalDate date = parseDate(matcher.group(1));
                if (date != null) {
                    extracted.put("periodEndDate", date);
                    break;
                }
            }
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

