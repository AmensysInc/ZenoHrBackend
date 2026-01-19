package com.application.employee.service.services.implementations;

import com.application.employee.service.config.SendGridEmail;
import com.application.employee.service.dto.TimesheetReminderRequest;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.TimeSheet;
import com.application.employee.service.entities.TimeSheetMaster;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.TimeSheetMasterRepo;
import com.application.employee.service.repositories.TimeSheetRepo;
import com.application.employee.service.services.TimesheetReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimesheetReminderServiceImpl implements TimesheetReminderService {

    @Autowired
    private TimeSheetMasterRepo timeSheetMasterRepo;

    @Autowired
    private TimeSheetRepo timeSheetRepo;

    @Autowired
    private EmployeeRespository employeeRespository;

    @Autowired
    private SendGridEmail sendGridEmail;

    @Value("${spring.mail.from:docs@saibersys.com}")
    private String defaultFromEmail;

    @Override
    public Map<String, Object> sendReminders(TimesheetReminderRequest request) {
        Map<String, Object> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failureList = new ArrayList<>();

        // Determine which employees to send reminders to
        List<String> employeeIds = request.getEmployeeIds();
        List<Employee> employeesToNotify = new ArrayList<>();

        if (employeeIds != null && !employeeIds.isEmpty()) {
            // Send to specific employees
            employeesToNotify = employeeIds.stream()
                    .map(id -> employeeRespository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            // Send to all employees with pending timesheets
            Integer month = request.getMonth() != null ? request.getMonth() : LocalDate.now().getMonthValue();
            Integer year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();

            List<TimeSheetMaster> pendingMasters = timeSheetMasterRepo.findAllByMonthAndYear(month, year);
            Set<String> employeeIdSet = pendingMasters.stream()
                    .map(master -> master.getEmployee().getEmployeeID())
                    .collect(Collectors.toSet());

            employeesToNotify = employeeIdSet.stream()
                    .map(id -> employeeRespository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // Prepare email content
        String subject = request.getSubject() != null && !request.getSubject().isEmpty()
                ? request.getSubject()
                : "Timesheet Submission Reminder";

        String message = request.getMessage() != null && !request.getMessage().isEmpty()
                ? request.getMessage()
                : "This is a reminder to submit your timesheet for the current period. Please log in to the system and submit your timesheet.";

        // Send emails
        for (Employee employee : employeesToNotify) {
            try {
                String employeeEmail = employee.getEmailID();
                String companyEmail = employee.getCompany() != null && employee.getCompany().getEmail() != null
                        ? employee.getCompany().getEmail()
                        : defaultFromEmail;

                String personalizedMessage = "Dear " + employee.getFirstName() + " " + employee.getLastName() + ",\n\n"
                        + message + "\n\n"
                        + "Regards,\nHR Team";

                sendGridEmail.sendEmails(
                        companyEmail,
                        List.of(employeeEmail),
                        null,
                        null,
                        subject,
                        personalizedMessage,
                        List.of()
                );

                if (sendGridEmail.getLastStatusCode() == 202) {
                    successList.add(employeeEmail);
                } else {
                    failureList.add(employeeEmail + " - " + sendGridEmail.getLastErrorMessage());
                }
            } catch (Exception e) {
                failureList.add(employee.getEmailID() + " - " + e.getMessage());
            }
        }

        result.put("successCount", successList.size());
        result.put("failureCount", failureList.size());
        result.put("successList", successList);
        result.put("failureList", failureList);
        result.put("totalSent", successList.size());

        return result;
    }

    @Override
    public Map<String, Object> getPendingTimesheets(Integer month, Integer year) {
        Map<String, Object> result = new HashMap<>();

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        List<TimeSheetMaster> masters = timeSheetMasterRepo.findAllByMonthAndYear(month, year);
        List<Map<String, Object>> pendingList = new ArrayList<>();

        for (TimeSheetMaster master : masters) {
            List<TimeSheet> sheets = timeSheetRepo.getAllByTimeSheetMasterMasterId(master.getMasterId());
            boolean hasPending = sheets.stream()
                    .anyMatch(sheet -> sheet.getStatus() == null || 
                            !sheet.getStatus().equalsIgnoreCase("SUBMITTED") &&
                            !sheet.getStatus().equalsIgnoreCase("APPROVED"));

            if (hasPending || sheets.isEmpty()) {
                Map<String, Object> pendingInfo = new HashMap<>();
                pendingInfo.put("employeeId", master.getEmployee().getEmployeeID());
                pendingInfo.put("employeeName", master.getEmployee().getFirstName() + " " + master.getEmployee().getLastName());
                pendingInfo.put("employeeEmail", master.getEmployee().getEmailID());
                pendingInfo.put("projectId", master.getProjectHistory().getProjectId());
                pendingInfo.put("projectName", master.getProjectHistory().getProjectAddress());
                pendingInfo.put("month", master.getMonth());
                pendingInfo.put("year", master.getYear());
                pendingInfo.put("masterId", master.getMasterId());
                pendingList.add(pendingInfo);
            }
        }

        result.put("month", month);
        result.put("year", year);
        result.put("pendingCount", pendingList.size());
        result.put("pendingTimesheets", pendingList);

        return result;
    }
}

