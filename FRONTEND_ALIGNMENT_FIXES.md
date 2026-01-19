# Frontend Alignment Fixes

This document lists all the issues found and fixes needed to align the frontend with the backend changes.

## 🔴 Issues Found

### 1. HR_MANAGER Role Not Included
**Issue:** HR_MANAGER role is not included in role checks throughout the frontend.

**Files Affected:**
- `src/SharedComponents/authUtils/authUtils.js`
- `src/SharedComponents/layout/Sidebar.js`
- `src/TimeSheets/TimeSheets.js`
- `src/TimeSheets/AllTimeSheets.js`
- All components that check for admin roles

### 2. Visa Status Field Missing
**Issue:** Employee form doesn't have visa status dropdown with the new enum values.

**Files Affected:**
- `src/Employee/EmployeeForm.js`

### 3. Address Verification Not Implemented
**Issue:** Address verification feature is completely missing.

**Files Affected:**
- Need to create new service
- Need to create new component
- Need to add to employee details page

### 4. Timesheet Reminders Not Implemented
**Issue:** Reminder functionality is missing.

**Files Affected:**
- Need to create new service
- Need to create new component
- Need to add to sidebar for admins

### 5. Template Download Not Implemented
**Issue:** Template download functionality is missing.

**Files Affected:**
- `src/SharedComponents/services/TimeSheetService.js`
- `src/TimeSheets/TimeSheets.js`

### 6. Endpoint Inconsistencies
**Issue:** Some endpoints might have case sensitivity issues (`/timeSheets` vs `/timesheets`).

**Files Affected:**
- All timesheet-related files

---

## ✅ Fixes to Apply

