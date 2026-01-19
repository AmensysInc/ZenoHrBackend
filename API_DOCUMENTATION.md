# API Documentation for Frontend Integration

This document describes all the new API endpoints added to support the missing features. The frontend team can use this to integrate with the backend.

## Base URL
All endpoints are relative to your backend base URL (e.g., `http://localhost:8080`)

## Authentication
All endpoints (except login/register) require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

---

## 1. HR Manager Role

The `HR_MANAGER` role has been added with the same permissions as `ADMIN`. Users with this role can:
- Access all employee management endpoints
- Access timesheet management
- Send reminders
- Verify addresses
- Download templates

**Note:** When creating users, you can now assign the role `HR_MANAGER` in addition to existing roles.

---

## 2. Visa Status Enum

The visa status field now supports the following enum values:
- `H1B`
- `OPT`
- `GREEN_CARD`
- `H4_EAD`
- `GREEN_CARD_EAD`
- `US_CITIZEN`
- `OPT_EXTENSION`

### 2.1 Get Visa Status Options
**GET** `/visa-status/options`

**Authorization:** Public (no auth required)

**Response:**
```json
[
  {
    "value": "H1B",
    "displayName": "H1B"
  },
  {
    "value": "OPT",
    "displayName": "OPT"
  },
  {
    "value": "GREEN_CARD",
    "displayName": "Green Card"
  },
  {
    "value": "H4_EAD",
    "displayName": "H4 EAD"
  },
  {
    "value": "GREEN_CARD_EAD",
    "displayName": "Green Card EAD"
  },
  {
    "value": "US_CITIZEN",
    "displayName": "US Citizen"
  },
  {
    "value": "OPT_EXTENSION",
    "displayName": "OPT Extension"
  }
]
```

**When creating/updating employees:**
- The `visaStatus` field in `EmployeeDTO` accepts these enum values as strings
- The backend will automatically convert common variations (e.g., "H4 EAD" → "H4_EAD")
- Use the `value` field from the options endpoint for the API, and `displayName` for UI display

**Example:**
```json
{
  "visaStatus": "H1B"
}
```

---

## 3. Address Verification Endpoints

### 3.1 Create or Update Address Verification
**POST** `/address-verification/{employeeId}`

**Authorization:** ADMIN, SADMIN, HR_MANAGER, EMPLOYEE

**Request Body:**
```json
{
  "homeAddress": "123 Main St, City, State, ZIP",
  "workAddress": "456 Work Ave, City, State, ZIP",
  "isWorking": true,
  "notes": "Optional notes"
}
```

**Response:**
```json
{
  "id": 1,
  "employeeId": "uuid",
  "employeeName": "John Doe",
  "employeeEmail": "john@example.com",
  "homeAddress": "123 Main St, City, State, ZIP",
  "homeAddressVerified": false,
  "homeAddressVerifiedDate": null,
  "workAddress": "456 Work Ave, City, State, ZIP",
  "workAddressVerified": false,
  "workAddressVerifiedDate": null,
  "isWorking": true,
  "verifiedBy": null,
  "notes": "Optional notes"
}
```

### 3.2 Get Address Verification
**GET** `/address-verification/{employeeId}`

**Authorization:** ADMIN, SADMIN, HR_MANAGER, EMPLOYEE

**Response:** Same as above

### 3.3 Update Address Verification
**PUT** `/address-verification/{employeeId}`

**Authorization:** ADMIN, SADMIN, HR_MANAGER, EMPLOYEE

**Request Body:**
```json
{
  "homeAddress": "Updated home address",
  "homeAddressVerified": true,
  "workAddress": "Updated work address",
  "workAddressVerified": true,
  "isWorking": true,
  "verifiedBy": "admin@example.com",
  "notes": "Verified on site"
}
```

### 3.4 Verify Home Address
**POST** `/address-verification/{employeeId}/verify-home?verifiedBy=admin@example.com`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Response:** Updated AddressVerificationDTO

### 3.5 Verify Work Address
**POST** `/address-verification/{employeeId}/verify-work?verifiedBy=admin@example.com`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Response:** Updated AddressVerificationDTO

### 3.6 Get All Address Verifications
**GET** `/address-verification/all`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Response:**
```json
[
  {
    "id": 1,
    "employeeId": "uuid",
    "employeeName": "John Doe",
    ...
  }
]
```

---

## 4. Timesheet Reminder Endpoints

### 4.1 Send Reminders
**POST** `/timesheets/reminders/send`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Request Body:**
```json
{
  "employeeIds": ["uuid1", "uuid2"],  // Optional: if null, sends to all with pending timesheets
  "subject": "Timesheet Submission Reminder",  // Optional
  "message": "Please submit your timesheet",  // Optional
  "month": 12,  // Optional: filter by month
  "year": 2024  // Optional: filter by year
}
```

**Response:**
```json
{
  "successCount": 5,
  "failureCount": 0,
  "successList": ["employee1@example.com", "employee2@example.com"],
  "failureList": [],
  "totalSent": 5
}
```

### 4.2 Get Pending Timesheets
**GET** `/timesheets/reminders/pending?month=12&year=2024`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Query Parameters:**
- `month` (optional): Month number (1-12)
- `year` (optional): Year (defaults to current)

**Response:**
```json
{
  "month": 12,
  "year": 2024,
  "pendingCount": 3,
  "pendingTimesheets": [
    {
      "employeeId": "uuid",
      "employeeName": "John Doe",
      "employeeEmail": "john@example.com",
      "projectId": "project-uuid",
      "projectName": "Project Address",
      "month": 12,
      "year": 2024,
      "masterId": 123
    }
  ]
}
```

---

## 5. Timesheet Template Download

### 5.1 Download Template
**GET** `/timeSheets/templates/download?templateType=weekly`

**Authorization:** ADMIN, SADMIN, HR_MANAGER

**Query Parameters:**
- `templateType` (optional): `weekly` or `monthly` (default: `weekly`)

**Response:** File download (Excel file)

**Note:** The template files should be placed in: `{file.storage-location}/templates/`
- `timesheet_weekly_template.xlsx`
- `timesheet_monthly_template.xlsx`

### 5.2 List Available Templates
**GET** `/timeSheets/templates/list`

**Authorization:** ADMIN, SADMIN, HR_MANAGER, EMPLOYEE

**Response:**
```json
[
  {
    "type": "weekly",
    "name": "Weekly Timesheet Template",
    "description": "Template for weekly timesheet submission"
  },
  {
    "type": "monthly",
    "name": "Monthly Timesheet Template",
    "description": "Template for monthly timesheet submission"
  }
]
```

---

## Error Responses

All endpoints may return the following error responses:

**400 Bad Request:**
```json
{
  "message": "Validation error message"
}
```

**401 Unauthorized:**
```json
{
  "message": "Unauthorized - Invalid or missing token"
}
```

**403 Forbidden:**
```json
{
  "message": "Forbidden - Insufficient permissions"
}
```

**404 Not Found:**
```json
{
  "message": "Resource not found"
}
```

**500 Internal Server Error:**
```json
{
  "message": "Internal server error"
}
```

---

## Database Changes Required

The following database table needs to be created:

### address_verification table
```sql
CREATE TABLE address_verification (
    ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id VARCHAR(255) NOT NULL,
    HOME_ADDRESS VARCHAR(500),
    HOME_ADDRESS_VERIFIED BOOLEAN DEFAULT FALSE,
    HOME_ADDRESS_VERIFIED_DATE DATE,
    WORK_ADDRESS VARCHAR(500),
    WORK_ADDRESS_VERIFIED BOOLEAN DEFAULT FALSE,
    WORK_ADDRESS_VERIFIED_DATE DATE,
    IS_WORKING BOOLEAN DEFAULT FALSE,
    VERIFIED_BY VARCHAR(255),
    NOTES TEXT,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(ID)
);
```

---

## Frontend Integration Checklist

- [ ] Update role enum to include `HR_MANAGER`
- [ ] Update visa status dropdown to use new enum values
- [ ] Implement address verification UI (create/update/view)
- [ ] Add address verification buttons for admins
- [ ] Implement timesheet reminder UI for admins
- [ ] Add template download button
- [ ] Update API service methods to call new endpoints
- [ ] Handle new error responses
- [ ] Test all new endpoints with proper authentication

---

## Notes for Frontend Team

1. **Visa Status:** The backend accepts both enum names (H1B, OPT, etc.) and display names (H1B, OPT, Green Card, etc.). Use the enum names for consistency.

2. **Address Verification:** Employees can update their own addresses, but only admins/HR managers can verify them.

3. **Reminders:** The reminder system sends emails via SendGrid. Make sure the backend has proper email configuration.

4. **Templates:** Template files need to be manually placed in the templates folder. The backend will serve them for download.

5. **Security:** All new endpoints respect role-based access control. Make sure to check user roles before showing UI elements.

---

## Support

For questions or issues, contact the backend team.

