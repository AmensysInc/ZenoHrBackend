package com.application.employee.service.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {

    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    SADMIN_READ("admin:read"),
    SADMIN_UPDATE("admin:update"),
    SADMIN_CREATE("admin:create"),
    SADMIN_DELETE("admin:delete"),
    EMPLOYEE_READ("management:read"),
    EMPLOYEE_UPDATE("management:update"),
    EMPLOYEE_CREATE("management:create"),
    EMPLOYEE_DELETE("management:delete"),
    PROSPECT_READ("management:read"),
    PROSPECT_UPDATE("management:update"),
    RECRUITER_READ("management:read"),
    RECRUITER_UPDATE("management:update"),
    SALES_READ("management:read"),
    SALES_UPDATE("management:update");


    @Getter
    private final String permission;
}
