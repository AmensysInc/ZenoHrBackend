package com.application.employee.service.services;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.entities.YTDData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxCalculatorService {

    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.062"); // 6.2%
    private static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145"); // 1.45%
    private static final BigDecimal ADDITIONAL_MEDICARE_RATE = new BigDecimal("0.009"); // 0.9%
    private static final BigDecimal SOCIAL_SECURITY_WAGE_BASE = new BigDecimal("160200"); // Update annually
    private static final BigDecimal ADDITIONAL_MEDICARE_THRESHOLD = new BigDecimal("200000"); // Single filer

    public TaxCalculations calculateTaxes(Employee employee, PreviousMonthTax previousMonthTax, 
                                         BigDecimal grossPay, YTDData ytdData) {
        TaxCalculations calculations = new TaxCalculations();
        calculations.setGrossPay(grossPay);
        calculations.setEmployeeType(employee.getSecurityGroup() != null ? employee.getSecurityGroup().name() : "US_CITIZEN");

        // If no previous tax data, create minimal structure
        if (previousMonthTax == null) {
            previousMonthTax = new PreviousMonthTax();
            previousMonthTax.setTotalGrossPay(BigDecimal.ZERO);
            previousMonthTax.setFederalTaxWithheld(BigDecimal.ZERO);
            previousMonthTax.setStateTaxWithheld(BigDecimal.ZERO);
            previousMonthTax.setLocalTaxWithheld(BigDecimal.ZERO);
            previousMonthTax.setSocialSecurityWithheld(BigDecimal.ZERO);
            previousMonthTax.setMedicareWithheld(BigDecimal.ZERO);
        }

        // Calculate each tax
        calculations.setFederalTax(calculateFederalTax(grossPay, previousMonthTax, ytdData));
        calculations.setStateTax(calculateStateTax(grossPay, previousMonthTax, ytdData));
        calculations.setLocalTax(calculateLocalTax(grossPay, previousMonthTax, ytdData));
        calculations.setSocialSecurity(calculateSocialSecurity(grossPay, ytdData, previousMonthTax));
        calculations.setMedicare(calculateMedicare(grossPay, ytdData, previousMonthTax));
        calculations.setAdditionalMedicare(calculateAdditionalMedicare(grossPay, ytdData, previousMonthTax));

        // Calculate total taxes
        BigDecimal totalTaxes = calculations.getFederalTax()
                .add(calculations.getStateTax())
                .add(calculations.getLocalTax())
                .add(calculations.getSocialSecurity())
                .add(calculations.getMedicare())
                .add(calculations.getAdditionalMedicare());
        calculations.setTotalTaxes(totalTaxes.setScale(2, RoundingMode.HALF_UP));

        return calculations;
    }

    private BigDecimal calculateFederalTax(BigDecimal grossPay, PreviousMonthTax previousMonthTax, YTDData ytdData) {
        // Use effective rate from previous month if available
        if (previousMonthTax.getTotalGrossPay() != null && 
            previousMonthTax.getTotalGrossPay().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = previousMonthTax.getFederalTaxWithheld()
                    .divide(previousMonthTax.getTotalGrossPay(), 10, RoundingMode.HALF_UP);
            return grossPay.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback to standard withholding (simplified)
        return standardFederalWithholding(grossPay, ytdData);
    }

    private BigDecimal calculateStateTax(BigDecimal grossPay, PreviousMonthTax previousMonthTax, YTDData ytdData) {
        if (previousMonthTax.getTotalGrossPay() != null && 
            previousMonthTax.getTotalGrossPay().compareTo(BigDecimal.ZERO) > 0 &&
            previousMonthTax.getStateTaxWithheld() != null) {
            BigDecimal effectiveRate = previousMonthTax.getStateTaxWithheld()
                    .divide(previousMonthTax.getTotalGrossPay(), 10, RoundingMode.HALF_UP);
            return grossPay.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateLocalTax(BigDecimal grossPay, PreviousMonthTax previousMonthTax, YTDData ytdData) {
        if (previousMonthTax.getTotalGrossPay() != null && 
            previousMonthTax.getTotalGrossPay().compareTo(BigDecimal.ZERO) > 0 &&
            previousMonthTax.getLocalTaxWithheld() != null &&
            previousMonthTax.getLocalTaxWithheld().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = previousMonthTax.getLocalTaxWithheld()
                    .divide(previousMonthTax.getTotalGrossPay(), 10, RoundingMode.HALF_UP);
            return grossPay.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSocialSecurity(BigDecimal grossPay, YTDData ytdData, PreviousMonthTax previousMonthTax) {
        // If previous month had zero or empty Social Security, don't calculate it
        if (previousMonthTax != null && 
            (previousMonthTax.getSocialSecurityWithheld() == null || 
             previousMonthTax.getSocialSecurityWithheld().compareTo(BigDecimal.ZERO) == 0)) {
            return BigDecimal.ZERO;
        }

        // Use effective rate from previous month if available
        if (previousMonthTax != null && 
            previousMonthTax.getTotalGrossPay() != null && 
            previousMonthTax.getTotalGrossPay().compareTo(BigDecimal.ZERO) > 0 &&
            previousMonthTax.getSocialSecurityWithheld() != null &&
            previousMonthTax.getSocialSecurityWithheld().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = previousMonthTax.getSocialSecurityWithheld()
                    .divide(previousMonthTax.getTotalGrossPay(), 10, RoundingMode.HALF_UP);
            return grossPay.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback to standard calculation
        BigDecimal ytdGross = (ytdData != null && ytdData.getYtdGrossPay() != null) ? 
                ytdData.getYtdGrossPay() : BigDecimal.ZERO;
        BigDecimal remainingWageBase = SOCIAL_SECURITY_WAGE_BASE.subtract(ytdGross);
        if (remainingWageBase.compareTo(BigDecimal.ZERO) < 0) {
            remainingWageBase = BigDecimal.ZERO;
        }
        BigDecimal taxableAmount = grossPay.min(remainingWageBase);
        return taxableAmount.multiply(SOCIAL_SECURITY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMedicare(BigDecimal grossPay, YTDData ytdData, PreviousMonthTax previousMonthTax) {
        // If previous month had zero or empty Medicare, don't calculate it
        if (previousMonthTax != null && 
            (previousMonthTax.getMedicareWithheld() == null || 
             previousMonthTax.getMedicareWithheld().compareTo(BigDecimal.ZERO) == 0)) {
            return BigDecimal.ZERO;
        }

        // Use effective rate from previous month if available
        if (previousMonthTax != null && 
            previousMonthTax.getTotalGrossPay() != null && 
            previousMonthTax.getTotalGrossPay().compareTo(BigDecimal.ZERO) > 0 &&
            previousMonthTax.getMedicareWithheld() != null &&
            previousMonthTax.getMedicareWithheld().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = previousMonthTax.getMedicareWithheld()
                    .divide(previousMonthTax.getTotalGrossPay(), 10, RoundingMode.HALF_UP);
            return grossPay.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback to standard calculation
        return grossPay.multiply(MEDICARE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateAdditionalMedicare(BigDecimal grossPay, YTDData ytdData, PreviousMonthTax previousMonthTax) {
        // If previous month had zero or empty Medicare, don't calculate Additional Medicare either
        if (previousMonthTax != null && 
            (previousMonthTax.getMedicareWithheld() == null || 
             previousMonthTax.getMedicareWithheld().compareTo(BigDecimal.ZERO) == 0)) {
            return BigDecimal.ZERO;
        }

        BigDecimal ytdGross = (ytdData != null && ytdData.getYtdGrossPay() != null) ? 
                ytdData.getYtdGrossPay() : BigDecimal.ZERO;
        BigDecimal currentYtdAfterThisPay = ytdGross.add(grossPay);

        if (currentYtdAfterThisPay.compareTo(ADDITIONAL_MEDICARE_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate only on the portion above threshold
        BigDecimal taxableAmount = currentYtdAfterThisPay.subtract(ADDITIONAL_MEDICARE_THRESHOLD);
        BigDecimal previousTaxableAmount = ytdGross.subtract(ADDITIONAL_MEDICARE_THRESHOLD);
        if (previousTaxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            previousTaxableAmount = BigDecimal.ZERO;
        }
        BigDecimal additionalTaxableThisPeriod = taxableAmount.subtract(previousTaxableAmount);
        if (additionalTaxableThisPeriod.compareTo(BigDecimal.ZERO) < 0) {
            additionalTaxableThisPeriod = BigDecimal.ZERO;
        }

        return additionalTaxableThisPeriod.multiply(ADDITIONAL_MEDICARE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal standardFederalWithholding(BigDecimal grossPay, YTDData ytdData) {
        // Simplified progressive tax brackets (2023 rates - update annually)
        // This is a simplified version - you may want to use IRS Publication 15-T
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal remainingPay = grossPay;

        // Tax brackets
        BigDecimal[][] brackets = {
            {new BigDecimal("0"), new BigDecimal("11000"), new BigDecimal("0.10")},
            {new BigDecimal("11000"), new BigDecimal("44725"), new BigDecimal("0.12")},
            {new BigDecimal("44725"), new BigDecimal("95375"), new BigDecimal("0.22")},
            {new BigDecimal("95375"), new BigDecimal("201050"), new BigDecimal("0.24")},
            {new BigDecimal("201050"), new BigDecimal("511850"), new BigDecimal("0.32")},
            {new BigDecimal("511850"), new BigDecimal("999999999"), new BigDecimal("0.37")}
        };

        for (BigDecimal[] bracket : brackets) {
            if (remainingPay.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal min = bracket[0];
            BigDecimal max = bracket[1];
            BigDecimal rate = bracket[2];

            BigDecimal taxableInBracket = remainingPay.min(max.subtract(min));
            if (taxableInBracket.compareTo(BigDecimal.ZERO) > 0) {
                tax = tax.add(taxableInBracket.multiply(rate));
                remainingPay = remainingPay.subtract(taxableInBracket);
            }
        }

        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetPay(BigDecimal grossPay, TaxCalculations taxes, 
                                     BigDecimal healthInsurance, BigDecimal retirement401k, 
                                     BigDecimal otherDeductions, BigDecimal totalCustomDeductions) {
        BigDecimal totalDeductions = taxes.getTotalTaxes()
                .add(healthInsurance != null ? healthInsurance : BigDecimal.ZERO)
                .add(retirement401k != null ? retirement401k : BigDecimal.ZERO)
                .add(otherDeductions != null ? otherDeductions : BigDecimal.ZERO)
                .add(totalCustomDeductions != null ? totalCustomDeductions : BigDecimal.ZERO);
        
        return grossPay.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
    }

    public YTDData updateYTDValues(YTDData currentYTD, TaxCalculations calculations, 
                                   BigDecimal netPay, BigDecimal healthInsurance, 
                                   BigDecimal retirement401k, BigDecimal otherDeductions) {
        if (currentYTD == null) {
            currentYTD = new YTDData();
            currentYTD.setYtdGrossPay(BigDecimal.ZERO);
            currentYTD.setYtdFederalTax(BigDecimal.ZERO);
            currentYTD.setYtdStateTax(BigDecimal.ZERO);
            currentYTD.setYtdLocalTax(BigDecimal.ZERO);
            currentYTD.setYtdSocialSecurity(BigDecimal.ZERO);
            currentYTD.setYtdMedicare(BigDecimal.ZERO);
            currentYTD.setYtdNetPay(BigDecimal.ZERO);
            currentYTD.setPayPeriodsCount(0);
        }

        currentYTD.setYtdGrossPay(currentYTD.getYtdGrossPay().add(calculations.getGrossPay()));
        currentYTD.setYtdFederalTax(currentYTD.getYtdFederalTax().add(calculations.getFederalTax()));
        currentYTD.setYtdStateTax(currentYTD.getYtdStateTax().add(calculations.getStateTax()));
        currentYTD.setYtdLocalTax(currentYTD.getYtdLocalTax().add(calculations.getLocalTax()));
        currentYTD.setYtdSocialSecurity(currentYTD.getYtdSocialSecurity().add(calculations.getSocialSecurity()));
        currentYTD.setYtdMedicare(currentYTD.getYtdMedicare().add(calculations.getMedicare()).add(calculations.getAdditionalMedicare()));
        currentYTD.setYtdNetPay(currentYTD.getYtdNetPay().add(netPay));
        currentYTD.setPayPeriodsCount(currentYTD.getPayPeriodsCount() + 1);

        return currentYTD;
    }

    // Inner class for tax calculations result
    public static class TaxCalculations {
        private BigDecimal grossPay;
        private String employeeType;
        private BigDecimal federalTax = BigDecimal.ZERO;
        private BigDecimal stateTax = BigDecimal.ZERO;
        private BigDecimal localTax = BigDecimal.ZERO;
        private BigDecimal socialSecurity = BigDecimal.ZERO;
        private BigDecimal medicare = BigDecimal.ZERO;
        private BigDecimal additionalMedicare = BigDecimal.ZERO;
        private BigDecimal totalTaxes = BigDecimal.ZERO;

        // Getters and setters
        public BigDecimal getGrossPay() { return grossPay; }
        public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
        public String getEmployeeType() { return employeeType; }
        public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }
        public BigDecimal getFederalTax() { return federalTax; }
        public void setFederalTax(BigDecimal federalTax) { this.federalTax = federalTax; }
        public BigDecimal getStateTax() { return stateTax; }
        public void setStateTax(BigDecimal stateTax) { this.stateTax = stateTax; }
        public BigDecimal getLocalTax() { return localTax; }
        public void setLocalTax(BigDecimal localTax) { this.localTax = localTax; }
        public BigDecimal getSocialSecurity() { return socialSecurity; }
        public void setSocialSecurity(BigDecimal socialSecurity) { this.socialSecurity = socialSecurity; }
        public BigDecimal getMedicare() { return medicare; }
        public void setMedicare(BigDecimal medicare) { this.medicare = medicare; }
        public BigDecimal getAdditionalMedicare() { return additionalMedicare; }
        public void setAdditionalMedicare(BigDecimal additionalMedicare) { this.additionalMedicare = additionalMedicare; }
        public BigDecimal getTotalTaxes() { return totalTaxes; }
        public void setTotalTaxes(BigDecimal totalTaxes) { this.totalTaxes = totalTaxes; }
    }
}

