package com.application.employee.service.services;

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
        
        // YTD (Year-To-Date) values
        extracted.put("ytdGrossPay", null);
        extracted.put("ytdNetPay", null);
        extracted.put("ytdFederalTax", null);
        extracted.put("ytdStateTax", null);
        extracted.put("ytdLocalTax", null);
        extracted.put("ytdSocialSecurity", null);
        extracted.put("ytdMedicare", null);

        try {
            byte[] pdfBytes = file.getInputStream().readAllBytes();
            try (PDDocument document = PDDocument.load(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                
                // Store raw text for debugging (first 2000 chars)
                if (text != null && text.length() > 0) {
                    extracted.put("_rawTextPreview", text.substring(0, Math.min(2000, text.length())));
                }

                // Extract standard fields
                extractStandardFields(text, extracted);

                // Extract YTD fields
                extractYtdFields(text, extracted);

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
        
        // First pass: Find table structure and column positions
        int thisPeriodColIndex = -1;
        int ytdColIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("this period") && line.contains("year to date")) {
                // Found header row - identify column positions
                String[] parts = lines[i].split("\\s+");
                for (int j = 0; j < parts.length; j++) {
                    if (parts[j].toLowerCase().contains("period") && thisPeriodColIndex == -1) {
                        thisPeriodColIndex = j;
                    }
                    if (parts[j].toLowerCase().contains("year") || parts[j].toLowerCase().contains("ytd")) {
                        ytdColIndex = j;
                        break;
                    }
                }
                break;
            }
        }
        
        // Extract from table format with "this period" and "year to date" columns
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();
            
            // Extract Gross Pay from earnings section - must match exactly "gross pay"
            if (lowerLine.matches(".*\\bgross\\s+pay\\b.*") && extracted.get("totalGrossPay") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("totalGrossPay", value);
                }
            }
            
            // Extract Net Pay - must match exactly "net pay"
            if (lowerLine.matches(".*\\bnet\\s+pay\\b.*") && extracted.get("totalNetPay") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("totalNetPay", value);
                }
            }
            
            // Extract Federal Tax - must be "federal income" (not just "federal")
            if (lowerLine.matches(".*\\bfederal\\s+income\\b.*") && extracted.get("federalTaxWithheld") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null) {
                    extracted.put("federalTaxWithheld", value.abs()); // Take absolute value for deductions
                }
            }
            
            // Extract State Tax - must be "state income" (not "state ui", "state di", etc.)
            if (lowerLine.matches(".*\\bstate\\s+income\\b.*") && 
                !lowerLine.contains("ui") && !lowerLine.contains("di") && !lowerLine.contains("fli") &&
                extracted.get("stateTaxWithheld") == null) {
                // Extract state name
                Pattern stateNamePattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+State\\s+Income", Pattern.CASE_INSENSITIVE);
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
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null) {
                    extracted.put("stateTaxWithheld", value.abs()); // Take absolute value
                }
            }
            
            // Extract Local Tax
            if (lowerLine.matches(".*\\blocal\\s+tax\\b.*") && extracted.get("localTaxWithheld") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null) {
                    extracted.put("localTaxWithheld", value.abs());
                }
            }
            
            // Extract Social Security - must match exactly
            if ((lowerLine.matches(".*\\bsocial\\s+security\\b.*") || lowerLine.matches(".*\\boasdi\\b.*")) 
                && extracted.get("socialSecurityWithheld") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null) {
                    extracted.put("socialSecurityWithheld", value.abs());
                }
            }
            
            // Extract Medicare - exclude additional medicare
            if (lowerLine.matches(".*\\bmedicare\\b.*") && 
                !lowerLine.contains("additional") && extracted.get("medicareWithheld") == null) {
                BigDecimal value = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                if (value != null) {
                    extracted.put("medicareWithheld", value.abs());
                }
            }
        }
    }
    
    private void extractYtdFields(String text, Map<String, Object> extracted) {
        String[] lines = text.split("\n");
        
        // Find YTD column index
        int ytdColIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("this period") && line.contains("year to date")) {
                // Found header row - identify YTD column position
                String[] parts = lines[i].split("\\s+");
                for (int j = 0; j < parts.length; j++) {
                    if (parts[j].toLowerCase().contains("year") || parts[j].toLowerCase().contains("ytd")) {
                        ytdColIndex = j;
                        break;
                    }
                }
                break;
            }
        }
        
        // Extract YTD values from table format
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();
            
            // Extract YTD Gross Pay
            if (lowerLine.matches(".*\\bgross\\s+pay\\b.*") && extracted.get("ytdGrossPay") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdGrossPay", value);
                }
            }
            
            // Extract YTD Net Pay
            if (lowerLine.matches(".*\\bnet\\s+pay\\b.*") && extracted.get("ytdNetPay") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdNetPay", value);
                }
            }
            
            // Extract YTD Federal Tax
            if (lowerLine.matches(".*\\bfederal\\s+income\\b.*") && extracted.get("ytdFederalTax") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdFederalTax", value.abs());
                }
            }
            
            // Extract YTD State Tax
            if (lowerLine.matches(".*\\bstate\\s+income\\b.*") && 
                !lowerLine.contains("ui") && !lowerLine.contains("di") && !lowerLine.contains("fli") &&
                extracted.get("ytdStateTax") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdStateTax", value.abs());
                }
            }
            
            // Extract YTD Local Tax
            if (lowerLine.matches(".*\\blocal\\s+tax\\b.*") && extracted.get("ytdLocalTax") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdLocalTax", value.abs());
                }
            }
            
            // Extract YTD Social Security
            if ((lowerLine.matches(".*\\bsocial\\s+security\\b.*") || lowerLine.matches(".*\\boasdi\\b.*")) 
                && extracted.get("ytdSocialSecurity") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdSocialSecurity", value.abs());
                }
            }
            
            // Extract YTD Medicare
            if (lowerLine.matches(".*\\bmedicare\\b.*") && 
                !lowerLine.contains("additional") && extracted.get("ytdMedicare") == null) {
                BigDecimal value = extractYtdValue(line, i, lines, ytdColIndex);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    extracted.put("ytdMedicare", value.abs());
                }
            }
        }
    }
    
    private BigDecimal extractYtdValue(String line, int lineIndex, String[] allLines, int ytdColIndex) {
        // Extract all numeric values from the line
        Pattern pattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
        Matcher matcher = pattern.matcher(line);
        List<BigDecimal> values = new ArrayList<>();
        
        while (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1));
            if (value != null) {
                values.add(value);
            }
        }
        
        if (values.isEmpty()) {
            return null;
        }
        
        // If we know the YTD column index, use it
        if (ytdColIndex >= 0 && ytdColIndex < values.size()) {
            return values.get(ytdColIndex);
        }
        
        // Otherwise, for table format with "this period" and "year to date":
        // - First value is usually "this period"
        // - Second value is usually "year to date" (YTD)
        if (values.size() >= 2) {
            // YTD is typically the second value (larger value for cumulative amounts)
            return values.get(1);
        }
        
        // If only one value, it might be YTD if the line explicitly mentions it
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("year to date") || lowerLine.contains("ytd")) {
            return values.get(0);
        }
        
        return null;
    }
    
    private BigDecimal extractThisPeriodValue(String line, int lineIndex, String[] allLines, int thisPeriodColIndex) {
        // Extract all numeric values from the line
        Pattern pattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
        Matcher matcher = pattern.matcher(line);
        List<BigDecimal> values = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        
        while (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1));
            if (value != null) {
                values.add(value);
                positions.add(matcher.start());
            }
        }
        
        if (values.isEmpty()) {
            return null;
        }
        
        // If we know the column index, use it
        if (thisPeriodColIndex >= 0 && thisPeriodColIndex < values.size()) {
            return values.get(thisPeriodColIndex);
        }
        
        // Otherwise, for table format with "this period" and "year to date":
        // - First value is usually "this period" (for earnings/deductions in table)
        // - But we need to check if line has "this period" or "year to date" text
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("this period") || lowerLine.contains("year to date")) {
            // This might be a header row, check next line
            if (lineIndex + 1 < allLines.length) {
                return extractThisPeriodValue(allLines[lineIndex + 1], lineIndex + 1, allLines, -1);
            }
        }
        
        // For deduction lines, first value is typically "this period"
        // For earnings, it depends on format - but usually first value
        // Since we're looking for "this period", take the first value
        // But if there are multiple values and one is clearly larger (YTD), take the smaller one
        if (values.size() >= 2) {
            // Compare values - "this period" is usually smaller than YTD for cumulative values
            // But for first pay, they might be equal
            BigDecimal first = values.get(0);
            BigDecimal second = values.get(1);
            
            // If second is much larger, first is likely "this period"
            if (second.compareTo(first.multiply(new BigDecimal("5"))) > 0) {
                return first;
            }
            // Otherwise, take first (this period column)
            return first;
        }
        
        return values.get(0);
    }
    
    private BigDecimal extractValueFromLine(String line, int lineIndex, String[] allLines) {
        // Strategy 1: Look for table format with "this period" and "year to date" columns
        // Pattern: field name, then value (this period), then value (ytd)
        // We want the FIRST value after the field name (this period column)
        Pattern tablePattern = Pattern.compile("(?:this\\s*period|year\\s*to\\s*date|ytd)", Pattern.CASE_INSENSITIVE);
        if (tablePattern.matcher(line).find()) {
            // This line has column headers, check next line for values
            if (lineIndex + 1 < allLines.length) {
                String nextLine = allLines[lineIndex + 1].trim();
                // Extract first value (this period column)
                Pattern valuePattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
                Matcher valueMatcher = valuePattern.matcher(nextLine);
                if (valueMatcher.find()) {
                    BigDecimal value = parseDecimal(valueMatcher.group(1).replace("-", ""));
                    if (value != null) return value;
                }
            }
        }
        
        // Strategy 2: Look for "this period" followed by value
        Pattern pattern = Pattern.compile("this\\s*period[\\s:]*\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
            if (value != null) return value;
        }
        
        // Strategy 3: Extract values from line and identify "this period" vs "ytd"
        // Look for pattern: field name, then this period value, then ytd value
        // In table format, "this period" is usually the first value column
        pattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
        matcher = pattern.matcher(line);
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
            if (value != null) {
                values.add(value);
            }
        }
        
        // If we have multiple values, we need to determine which is "this period"
        // Check if line contains "year to date" or "ytd" to identify columns
        String lowerLine = line.toLowerCase();
        boolean hasYtdMarker = lowerLine.contains("year to date") || lowerLine.contains("ytd");
        
        if (values.size() >= 2 && hasYtdMarker) {
            // First value is typically "this period", second is "ytd"
            // But we need to check the context - look at surrounding lines
            // For gross pay/net pay, "this period" is usually smaller than YTD (unless it's the first pay)
            // For deductions, both are usually similar or YTD is larger
            return values.get(0); // First value is "this period"
        } else if (values.size() >= 2) {
            // No YTD marker, but multiple values - check if we can infer from context
            // Look at previous/next lines for context
            if (lineIndex > 0) {
                String prevLine = allLines[lineIndex - 1].toLowerCase();
                if (prevLine.contains("this period") || prevLine.contains("year to date")) {
                    // Previous line has headers, first value is "this period"
                    return values.get(0);
                }
            }
            // If no clear context, prefer smaller value for gross/net (this period is usually smaller)
            // But for deductions, either could work - take first
            return values.get(0);
        } else if (values.size() == 1) {
            return values.get(0);
        }
        
        // Strategy 4: Look for value in next line (common in table formats)
        if (lineIndex + 1 < allLines.length) {
            String nextLine = allLines[lineIndex + 1].trim();
            pattern = Pattern.compile("\\$?\\s*(-?[\\d,]+(?:\\.\\d{2})?)");
            matcher = pattern.matcher(nextLine);
            if (matcher.find()) {
                BigDecimal value = parseDecimal(matcher.group(1).replace("-", ""));
                if (value != null) return value;
            }
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
        boolean inVoluntaryDeductionsSection = false;
        boolean inStatutoryDeductionsSection = false;
        Set<String> standardFields = new HashSet<>(Arrays.asList(
            "federal income", "federal tax", "social security", "medicare",
            "gross pay", "net pay", "state income", "california state income",
            "illinois state income", "local tax", "additional medicare",
            "state ui", "state di", "state fli", "state sdi", "state sui"
        ));

        // First, find column positions for "this period" and "year to date"
        int thisPeriodColIndex = -1;
        int ytdColIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("this period") && line.contains("year to date")) {
                String[] parts = lines[i].split("\\s+");
                for (int j = 0; j < parts.length; j++) {
                    if (parts[j].toLowerCase().contains("period") && thisPeriodColIndex == -1) {
                        thisPeriodColIndex = j;
                    }
                    if (parts[j].toLowerCase().contains("year") || parts[j].toLowerCase().contains("ytd")) {
                        ytdColIndex = j;
                        break;
                    }
                }
                break;
            }
        }

        // Track which sections we're in
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lowerLine = line.toLowerCase();

            // Detect any deductions section
            if (lowerLine.contains("deductions") || lowerLine.contains("voluntary") || lowerLine.contains("statutory")) {
                inDeductionsSection = true;
                if (lowerLine.contains("voluntary")) {
                    inVoluntaryDeductionsSection = true;
                    inStatutoryDeductionsSection = false;
                } else if (lowerLine.contains("statutory")) {
                    inStatutoryDeductionsSection = true;
                    inVoluntaryDeductionsSection = false;
                }
                continue;
            }

            // Stop if we hit net pay or other sections
            if (lowerLine.contains("net pay") || lowerLine.contains("important notes") || 
                lowerLine.contains("basis of pay") || lowerLine.contains("federal taxable") ||
                lowerLine.contains("check") || lowerLine.contains("pay to the order") ||
                lowerLine.contains("earnings statement") || lowerLine.contains("employee information")) {
                inDeductionsSection = false;
                inVoluntaryDeductionsSection = false;
                inStatutoryDeductionsSection = false;
                continue;
            }

            // Process any line in deductions sections OR lines that look like deductions
            if ((inDeductionsSection || inVoluntaryDeductionsSection || inStatutoryDeductionsSection) && !line.isEmpty()) {
                // Skip header rows
                if (lowerLine.contains("this period") || lowerLine.contains("year to date") || 
                    lowerLine.contains("voluntary deductions") || lowerLine.contains("statutory deductions") ||
                    lowerLine.contains("deductions") && (lowerLine.contains("this period") || lowerLine.contains("year to date"))) {
                    continue;
                }

                // Check if it's a standard field
                boolean isStandard = false;
                for (String standard : standardFields) {
                    if (lowerLine.contains(standard)) {
                        isStandard = true;
                        break;
                    }
                }
                
                // Extract any non-standard deduction
                if (!isStandard) {
                    // Extract deduction name - everything before the first number or dash
                    Pattern namePattern = Pattern.compile("^([A-Za-z][A-Za-z\\s]+?)(?=\\s*-?\\s*[\\d,])", Pattern.CASE_INSENSITIVE);
                    Matcher nameMatcher = namePattern.matcher(line);
                    
                    String deductionName = null;
                    if (nameMatcher.find()) {
                        deductionName = nameMatcher.group(1).trim();
                    } else {
                        // Try alternative: extract first word(s) before any number
                        Pattern altPattern = Pattern.compile("^([A-Za-z]+(?:\\s+[A-Za-z]+)*)", Pattern.CASE_INSENSITIVE);
                        Matcher altMatcher = altPattern.matcher(line);
                        if (altMatcher.find()) {
                            deductionName = altMatcher.group(1).trim();
                        }
                    }
                    
                    // Also try to extract from lines that have numbers but might not match the pattern above
                    if (deductionName == null || deductionName.length() < 2) {
                        // Look for common deduction patterns: word(s) followed by numbers
                        Pattern commonPattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+(-?[\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
                        Matcher commonMatcher = commonPattern.matcher(line);
                        if (commonMatcher.find()) {
                            String potentialName = commonMatcher.group(1).trim();
                            // Make sure it's not a standard field
                            boolean isStandardCheck = false;
                            for (String standard : standardFields) {
                                if (potentialName.toLowerCase().contains(standard)) {
                                    isStandardCheck = true;
                                    break;
                                }
                            }
                            if (!isStandardCheck && potentialName.length() > 1) {
                                deductionName = potentialName;
                            }
                        }
                    }
                    
                    if (deductionName != null && deductionName.length() > 1) {
                        // Extract all numeric values from the line
                        Pattern valuePattern = Pattern.compile("(-?[\\d,]+(?:\\.\\d{2})?)");
                        Matcher valueMatcher = valuePattern.matcher(line);
                        List<BigDecimal> allValues = new ArrayList<>();
                        while (valueMatcher.find()) {
                            BigDecimal val = parseDecimal(valueMatcher.group(1));
                            if (val != null) {
                                allValues.add(val.abs()); // Store absolute values
                            }
                        }
                        
                        // If no values on current line, check next line (common in table formats)
                        if (allValues.isEmpty() && i + 1 < lines.length) {
                            String nextLine = lines[i + 1].trim();
                            valueMatcher = valuePattern.matcher(nextLine);
                            while (valueMatcher.find()) {
                                BigDecimal val = parseDecimal(valueMatcher.group(1));
                                if (val != null) {
                                    allValues.add(val.abs());
                                }
                            }
                        }
                        
                        BigDecimal thisPeriodAmount = null;
                        BigDecimal ytdAmount = null;
                        
                        // If we have multiple values, first is "this period", second is "year to date"
                        if (allValues.size() >= 2) {
                            thisPeriodAmount = allValues.get(0);
                            ytdAmount = allValues.get(1);
                        } else if (allValues.size() == 1) {
                            thisPeriodAmount = allValues.get(0);
                        }
                        
                        // If still no value, try extractThisPeriodValue method
                        if (thisPeriodAmount == null) {
                            thisPeriodAmount = extractThisPeriodValue(line, i, lines, thisPeriodColIndex);
                            if (thisPeriodAmount != null) {
                                thisPeriodAmount = thisPeriodAmount.abs();
                            }
                        }
                        
                        // Create a clean key
                        String key = deductionName.toLowerCase()
                            .replaceAll("[^a-z0-9\\s]", "")
                            .replaceAll("\\s+", "_");
                        
                        if (key.length() > 50) {
                            key = key.substring(0, 50);
                        }
                        
                        // Always add the field - set to 0 if no amount found
                        // This ensures "Advance" with $0.00 and "Miscellaneous" are captured
                        if (thisPeriodAmount == null) {
                            thisPeriodAmount = BigDecimal.ZERO;
                        }
                        
                        // Only add if not already exists or if this is a better match (non-zero amount)
                        if (!additionalFields.containsKey(key) || 
                            (additionalFields.containsKey(key) && thisPeriodAmount.compareTo(BigDecimal.ZERO) > 0)) {
                            Map<String, Object> fieldData = new HashMap<>();
                            fieldData.put("name", deductionName);
                            fieldData.put("value", thisPeriodAmount);
                            if (ytdAmount != null) {
                                fieldData.put("ytd", ytdAmount);
                            }
                            additionalFields.put(key, fieldData);
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

