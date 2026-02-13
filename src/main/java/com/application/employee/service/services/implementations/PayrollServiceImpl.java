package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.entities.Paystub;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.entities.YTDData;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.PayrollRecordRepository;
import com.application.employee.service.repositories.PaystubRepository;
import com.application.employee.service.repositories.PreviousMonthTaxRepository;
import com.application.employee.service.repositories.YTDDataRepository;
import com.application.employee.service.services.CheckSettingsService;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.services.PayrollService;
import com.application.employee.service.services.TaxCalculatorService;
import com.application.employee.service.services.TaxCalculatorService.TaxCalculations;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Autowired
    private EmployeeRespository employeeRepository;

    @Autowired
    private PayrollRecordRepository payrollRecordRepository;

    @Autowired
    private PaystubRepository paystubRepository;

    @Autowired
    private PreviousMonthTaxRepository previousMonthTaxRepository;

    @Autowired
    private YTDDataRepository ytdDataRepository;

    @Autowired
    private TaxCalculatorService taxCalculatorService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CheckSettingsService checkSettingsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public TaxCalculations calculatePayroll(String employeeId, BigDecimal grossPay,
                                           LocalDate payPeriodStart, LocalDate payPeriodEnd,
                                           LocalDate payDate, Map<String, BigDecimal> otherDeductions,
                                           Map<String, Object> customDeductions) {
        Employee employee = employeeService.getEmployee(employeeId);
        if (employee == null) {
            throw new RuntimeException("Employee not found: " + employeeId);
        }

        // Get previous month tax data
        Optional<PreviousMonthTax> previousMonthTaxOpt = previousMonthTaxRepository.findByEmployeeEmployeeID(employeeId);
        PreviousMonthTax previousMonthTax = previousMonthTaxOpt.orElse(null);

        // Get current YTD data
        Integer currentYear = LocalDate.now().getYear();
        Optional<YTDData> ytdDataOpt = ytdDataRepository.findByEmployeeEmployeeIDAndCurrentYear(employeeId, currentYear);
        YTDData ytdData = ytdDataOpt.orElse(null);

        // Calculate taxes
        TaxCalculations calculations = taxCalculatorService.calculateTaxes(employee, previousMonthTax, grossPay, ytdData);

        return calculations;
    }

    @Override
    @Transactional
    public PayrollRecord generatePayroll(String employeeId, BigDecimal grossPay,
                                        LocalDate payPeriodStart, LocalDate payPeriodEnd,
                                        LocalDate payDate, TaxCalculations taxCalculations,
                                        Map<String, BigDecimal> otherDeductions,
                                        Map<String, Object> customDeductions,
                                        BigDecimal previousYtdGrossPay,
                                        BigDecimal previousYtdNetPay,
                                        BigDecimal previousYtdFederalTax,
                                        BigDecimal previousYtdStateTax,
                                        BigDecimal previousYtdLocalTax,
                                        BigDecimal previousYtdSocialSecurity,
                                        BigDecimal previousYtdMedicare,
                                        String paystubHtml) {
        Employee employee = employeeService.getEmployee(employeeId);
        if (employee == null) {
            throw new RuntimeException("Employee not found: " + employeeId);
        }

        // Get current YTD data
        Integer currentYear = LocalDate.now().getYear();
        Optional<YTDData> ytdDataOpt = ytdDataRepository.findByEmployeeEmployeeIDAndCurrentYear(employeeId, currentYear);
        YTDData ytdData = ytdDataOpt.orElse(null);

        if (ytdData == null) {
            ytdData = new YTDData();
            ytdData.setEmployee(employee);
            ytdData.setCurrentYear(currentYear);
            ytdData.setYtdGrossPay(BigDecimal.ZERO);
            ytdData.setYtdFederalTax(BigDecimal.ZERO);
            ytdData.setYtdStateTax(BigDecimal.ZERO);
            ytdData.setYtdLocalTax(BigDecimal.ZERO);
            ytdData.setYtdSocialSecurity(BigDecimal.ZERO);
            ytdData.setYtdMedicare(BigDecimal.ZERO);
            ytdData.setYtdNetPay(BigDecimal.ZERO);
            ytdData.setPayPeriodsCount(0);
        }
        
        // Automatically aggregate YTD from previous records before current pay period
        // PRIORITY: Generated payroll records are ALWAYS used over uploaded paystubs
        // This ensures that when generating March, it uses February's generated payroll (not January's uploaded paystub)
        BigDecimal aggregatedYtdGrossPay = null;
        BigDecimal aggregatedYtdNetPay = null;
        BigDecimal aggregatedYtdFederalTax = null;
        BigDecimal aggregatedYtdStateTax = null;
        BigDecimal aggregatedYtdLocalTax = null;
        BigDecimal aggregatedYtdSocialSecurity = null;
        BigDecimal aggregatedYtdMedicare = null;
        
        // FIRST: Find the latest generated payroll record before current pay period (PRIORITY)
        // This ensures generated payrolls are always used over uploaded paystubs
        Optional<PayrollRecord> latestPayrollOpt = payrollRecordRepository.findLatestByEmployeeIdAndPayPeriodEndBefore(employeeId, payPeriodStart);
        if (latestPayrollOpt.isPresent()) {
            PayrollRecord payroll = latestPayrollOpt.get();
            if (payroll.getYtdGrossPay() != null) {
                aggregatedYtdGrossPay = payroll.getYtdGrossPay();
                aggregatedYtdNetPay = payroll.getYtdNetPay();
                // For generated payrolls, get tax YTD from YTDData if available
                // (PayrollRecord only stores ytdGrossPay and ytdNetPay)
                Optional<YTDData> latestYtdData = ytdDataRepository.findByEmployeeEmployeeIDAndCurrentYear(employeeId, currentYear);
                if (latestYtdData.isPresent()) {
                    YTDData latestYtd = latestYtdData.get();
                    aggregatedYtdFederalTax = latestYtd.getYtdFederalTax();
                    aggregatedYtdStateTax = latestYtd.getYtdStateTax();
                    aggregatedYtdLocalTax = latestYtd.getYtdLocalTax();
                    aggregatedYtdSocialSecurity = latestYtd.getYtdSocialSecurity();
                    aggregatedYtdMedicare = latestYtd.getYtdMedicare();
                }
                System.out.println("Using YTD from generated PayrollRecord: " + payroll.getId() + " (Pay Period End: " + payroll.getPayPeriodEnd() + ")");
            }
        }
        
        // SECOND: Only if no generated payroll found, check uploaded paystubs (FALLBACK)
        // This is used only for the first time when generating payroll from uploaded paystub
        if (aggregatedYtdGrossPay == null) {
            Optional<Paystub> latestPaystubOpt = paystubRepository.findLatestByEmployeeIdAndPayPeriodEndBefore(employeeId, payPeriodStart);
            if (latestPaystubOpt.isPresent()) {
                Paystub paystub = latestPaystubOpt.get();
                if (paystub.getYtdGrossPay() != null) {
                    aggregatedYtdGrossPay = paystub.getYtdGrossPay();
                    aggregatedYtdNetPay = paystub.getYtdNetPay();
                    aggregatedYtdFederalTax = paystub.getYtdFederalTax();
                    aggregatedYtdStateTax = paystub.getYtdStateTax();
                    aggregatedYtdLocalTax = paystub.getYtdLocalTax();
                    aggregatedYtdSocialSecurity = paystub.getYtdSocialSecurity();
                    aggregatedYtdMedicare = paystub.getYtdMedicare();
                    System.out.println("Using YTD from uploaded Paystub: " + paystub.getId() + " (Pay Period End: " + paystub.getPayPeriodEnd() + ")");
                }
            } else {
                System.out.println("No previous payroll record or paystub found for YTD initialization.");
            }
        } else {
            System.out.println("Generated payroll found, skipping uploaded paystubs check.");
        }
        
        // Use aggregated YTD if available and no explicit previous YTD was provided
        if (aggregatedYtdGrossPay != null) {
            if (previousYtdGrossPay == null) {
                ytdData.setYtdGrossPay(aggregatedYtdGrossPay);
            }
            if (previousYtdNetPay == null && aggregatedYtdNetPay != null) {
                ytdData.setYtdNetPay(aggregatedYtdNetPay);
            }
            if (previousYtdFederalTax == null && aggregatedYtdFederalTax != null) {
                ytdData.setYtdFederalTax(aggregatedYtdFederalTax);
            }
            if (previousYtdStateTax == null && aggregatedYtdStateTax != null) {
                ytdData.setYtdStateTax(aggregatedYtdStateTax);
            }
            if (previousYtdLocalTax == null && aggregatedYtdLocalTax != null) {
                ytdData.setYtdLocalTax(aggregatedYtdLocalTax);
            }
            if (previousYtdSocialSecurity == null && aggregatedYtdSocialSecurity != null) {
                ytdData.setYtdSocialSecurity(aggregatedYtdSocialSecurity);
            }
            if (previousYtdMedicare == null && aggregatedYtdMedicare != null) {
                ytdData.setYtdMedicare(aggregatedYtdMedicare);
            }
        }
        
        // If previous YTD data is explicitly provided, use it as the starting point (overrides aggregated)
        if (previousYtdGrossPay != null) {
            ytdData.setYtdGrossPay(previousYtdGrossPay);
        }
        if (previousYtdNetPay != null) {
            ytdData.setYtdNetPay(previousYtdNetPay);
        }
        if (previousYtdFederalTax != null) {
            ytdData.setYtdFederalTax(previousYtdFederalTax);
        }
        if (previousYtdStateTax != null) {
            ytdData.setYtdStateTax(previousYtdStateTax);
        }
        if (previousYtdLocalTax != null) {
            ytdData.setYtdLocalTax(previousYtdLocalTax);
        }
        if (previousYtdSocialSecurity != null) {
            ytdData.setYtdSocialSecurity(previousYtdSocialSecurity);
        }
        if (previousYtdMedicare != null) {
            ytdData.setYtdMedicare(previousYtdMedicare);
        }

        // Extract standard deductions
        BigDecimal healthInsurance = otherDeductions != null ? 
                otherDeductions.getOrDefault("healthInsurance", BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal otherDeductionsAmount = otherDeductions != null ? 
                otherDeductions.getOrDefault("otherDeductions", BigDecimal.ZERO) : BigDecimal.ZERO;

        // Calculate total custom deductions
        // Handle both simple Map<String, BigDecimal> and Map<String, Object> with name/value structure
        BigDecimal totalCustomDeductions = BigDecimal.ZERO;
        if (customDeductions != null) {
            for (Object value : customDeductions.values()) {
                BigDecimal deductionAmount = BigDecimal.ZERO;
                if (value instanceof BigDecimal) {
                    deductionAmount = (BigDecimal) value;
                } else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldData = (Map<String, Object>) value;
                    if (fieldData.containsKey("value")) {
                        Object val = fieldData.get("value");
                        if (val instanceof Number) {
                            deductionAmount = BigDecimal.valueOf(((Number) val).doubleValue());
                        } else if (val instanceof String) {
                            try {
                                deductionAmount = new BigDecimal((String) val);
                            } catch (NumberFormatException e) {
                                // Ignore invalid values
                            }
                        }
                    }
                } else if (value instanceof Number) {
                    deductionAmount = BigDecimal.valueOf(((Number) value).doubleValue());
                }
                if (deductionAmount != null && deductionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    totalCustomDeductions = totalCustomDeductions.add(deductionAmount);
                }
            }
        }
        totalCustomDeductions = totalCustomDeductions.setScale(2, RoundingMode.HALF_UP);

        // Calculate net pay
        BigDecimal netPay = taxCalculatorService.calculateNetPay(grossPay, taxCalculations,
                healthInsurance, otherDeductionsAmount, totalCustomDeductions);

        // Calculate total deductions
        BigDecimal totalDeductions = taxCalculations.getTotalTaxes()
                .add(healthInsurance)
                .add(otherDeductionsAmount)
                .add(totalCustomDeductions)
                .setScale(2, RoundingMode.HALF_UP);

        // Create payroll record
        PayrollRecord payrollRecord = new PayrollRecord();
        payrollRecord.setEmployee(employee);
        payrollRecord.setPayPeriodStart(payPeriodStart);
        payrollRecord.setPayPeriodEnd(payPeriodEnd);
        payrollRecord.setPayDate(payDate);
        payrollRecord.setGrossPay(grossPay);
        payrollRecord.setFederalTax(taxCalculations.getFederalTax());
        payrollRecord.setStateTax(taxCalculations.getStateTax());
        payrollRecord.setLocalTax(taxCalculations.getLocalTax());
        payrollRecord.setSocialSecurity(taxCalculations.getSocialSecurity());
        payrollRecord.setMedicare(taxCalculations.getMedicare());
        payrollRecord.setAdditionalMedicare(taxCalculations.getAdditionalMedicare());
        payrollRecord.setHealthInsurance(healthInsurance);
        payrollRecord.setRetirement401k(BigDecimal.ZERO); // Set to zero as field is deprecated but kept for backward compatibility
        payrollRecord.setOtherDeductions(otherDeductionsAmount);
        payrollRecord.setTotalDeductions(totalDeductions);
        payrollRecord.setNetPay(netPay);
        payrollRecord.setStatus("processed");
        payrollRecord.setPaystubGenerated(false);

        // Store custom deductions as JSON
        if (customDeductions != null && !customDeductions.isEmpty()) {
            try {
                payrollRecord.setCustomDeductionsJson(objectMapper.writeValueAsString(customDeductions));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing custom deductions", e);
            }
        }

        // Get state tax name from previous month tax if available
        Optional<PreviousMonthTax> previousMonthTaxOpt = previousMonthTaxRepository.findByEmployeeEmployeeID(employeeId);
        if (previousMonthTaxOpt.isPresent() && previousMonthTaxOpt.get().getStateTaxName() != null) {
            payrollRecord.setStateTaxName(previousMonthTaxOpt.get().getStateTaxName());
        }

        // Get and assign check number based on company
        if (employee.getCompany() != null && employee.getCompany().getCompanyId() != null) {
            Long checkNumber = checkSettingsService.getNextCheckNumber(employee.getCompany().getCompanyId());
            payrollRecord.setCheckNumber(checkNumber);
        }

        // Store paystub HTML template if provided
        if (paystubHtml != null && !paystubHtml.isEmpty()) {
            payrollRecord.setPaystubHtml(paystubHtml);
            payrollRecord.setPaystubGenerated(true);
        }

        // Save payroll record
        payrollRecord = payrollRecordRepository.save(payrollRecord);

        // Update YTD values
        ytdData = taxCalculatorService.updateYTDValues(ytdData, taxCalculations, netPay,
                healthInsurance, otherDeductionsAmount);
        ytdData.setLastPayPeriod(payDate);
        ytdDataRepository.save(ytdData);

        // Set YTD values in payroll record
        payrollRecord.setYtdGrossPay(ytdData.getYtdGrossPay());
        payrollRecord.setYtdNetPay(ytdData.getYtdNetPay());
        payrollRecord = payrollRecordRepository.save(payrollRecord);

        return payrollRecord;
    }

    @Override
    public List<PayrollRecord> getAllPayrollRecords() {
        return payrollRecordRepository.findAllWithEmployee();
    }

    @Override
    public List<PayrollRecord> getPayrollRecordsByEmployee(String employeeId) {
        return payrollRecordRepository.findByEmployeeIdWithEmployee(employeeId);
    }

    @Override
    public PayrollRecord getPayrollRecordById(Long id) {
        return payrollRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found: " + id));
    }

    @Override
    public PayrollRecord getLatestPayrollRecordByEmployee(String employeeId) {
        return payrollRecordRepository.findLatestByEmployeeId(employeeId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deletePayrollRecord(Long id) {
        if (!payrollRecordRepository.existsById(id)) {
            throw new RuntimeException("Payroll record not found: " + id);
        }
        payrollRecordRepository.deleteById(id);
    }
}

