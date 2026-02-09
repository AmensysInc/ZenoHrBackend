package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.entities.YTDData;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.PayrollRecordRepository;
import com.application.employee.service.repositories.PreviousMonthTaxRepository;
import com.application.employee.service.repositories.YTDDataRepository;
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
    private PreviousMonthTaxRepository previousMonthTaxRepository;

    @Autowired
    private YTDDataRepository ytdDataRepository;

    @Autowired
    private TaxCalculatorService taxCalculatorService;

    @Autowired
    private EmployeeService employeeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public TaxCalculations calculatePayroll(String employeeId, BigDecimal grossPay,
                                           LocalDate payPeriodStart, LocalDate payPeriodEnd,
                                           LocalDate payDate, Map<String, BigDecimal> otherDeductions) {
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
                                        Map<String, BigDecimal> customDeductions) {
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

        // Extract standard deductions
        BigDecimal healthInsurance = otherDeductions != null ? 
                otherDeductions.getOrDefault("healthInsurance", BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal retirement401k = otherDeductions != null ? 
                otherDeductions.getOrDefault("retirement401k", BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal otherDeductionsAmount = otherDeductions != null ? 
                otherDeductions.getOrDefault("otherDeductions", BigDecimal.ZERO) : BigDecimal.ZERO;

        // Calculate total custom deductions
        BigDecimal totalCustomDeductions = BigDecimal.ZERO;
        if (customDeductions != null) {
            for (BigDecimal value : customDeductions.values()) {
                if (value != null) {
                    totalCustomDeductions = totalCustomDeductions.add(value);
                }
            }
        }
        totalCustomDeductions = totalCustomDeductions.setScale(2, RoundingMode.HALF_UP);

        // Calculate net pay
        BigDecimal netPay = taxCalculatorService.calculateNetPay(grossPay, taxCalculations,
                healthInsurance, retirement401k, otherDeductionsAmount, totalCustomDeductions);

        // Calculate total deductions
        BigDecimal totalDeductions = taxCalculations.getTotalTaxes()
                .add(healthInsurance)
                .add(retirement401k)
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
        payrollRecord.setRetirement401k(retirement401k);
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

        // Save payroll record
        payrollRecord = payrollRecordRepository.save(payrollRecord);

        // Update YTD values
        ytdData = taxCalculatorService.updateYTDValues(ytdData, taxCalculations, netPay,
                healthInsurance, retirement401k, otherDeductionsAmount);
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

