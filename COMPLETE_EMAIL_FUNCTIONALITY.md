# Complete Email/Mailing Functionality Overview

This document lists ALL email sending functionality in your HRMS application.

---

## 📧 Email Features Summary

### 1. **Send Login Details** (Employee Management)
**Location**: Employee Edit/Create Form  
**Endpoint**: `POST /auth/resetPassword`  
**Category**: `LOGIN_DETAILS`  
**Used For**: Sending login credentials to employees  
**Who Can Use**: Admin, Super Admin (via Employee form "Send Login Details" button)  
**Implementation**: 
- Calls `AuthenticationService.reset()` with category "LOGIN_DETAILS"
- Uses email template system (falls back to generic template if not found)
- Uses **SendGrid** via `sendEmailUsingTemplate()`

**Frontend**: `EmployeeForm.js` → `sendLoginDetails()`  
**Backend**: `AuthenticationService.reset()` → `sendEmailUsingTemplate()`

---

### 2. **Forgot Password** (Authentication)
**Location**: Login Page  
**Endpoint**: `POST /auth/resetPassword`  
**Category**: `FORGOT_PASSWORD`  
**Used For**: Password recovery when user forgets password  
**Who Can Use**: Anyone (public endpoint)  
**Implementation**: 
- Generates temporary password
- Uses email template with category "FORGOT_PASSWORD"
- Uses **SendGrid** via `sendEmailUsingTemplate()`

**Frontend**: `Login.js` or `ForgotPassword.js`  
**Backend**: `AuthenticationService.reset()` → `sendEmailUsingTemplate()`

---

### 3. **Change Password Confirmation** (Authentication)
**Location**: After password change  
**Endpoint**: `POST /auth/resetPassword`  
**Category**: `CHANGE_PASSWORD`  
**Used For**: Confirmation email after password change  
**Who Can Use**: Authenticated users  
**Implementation**: 
- Sends confirmation email (no temp password generated)
- Uses email template with category "CHANGE_PASSWORD"
- Uses **SendGrid** via `sendEmailUsingTemplate()`

---

### 4. **User Registration Email** (User Management)
**Location**: User Registration  
**Endpoint**: `POST /auth/register`  
**Category**: `LOGIN_DETAILS` (internally)  
**Used For**: Sending temporary password to newly registered users  
**Who Can Use**: System (automatic on registration)  
**Implementation**: 
- **⚠️ ISSUE**: Uses `sendTemporaryPasswordEmail()` which uses **Gmail SMTP** (JavaMailSender)
- Should be updated to use SendGrid for consistency
- Uses **Gmail SMTP** (inconsistent!)

**Backend**: `AuthenticationService.register()` → `sendTemporaryPasswordEmail()` ❌

---

### 5. **Manual Email Sending** (Communication)
**Location**: "Send Email" page  
**Endpoint**: `POST /email/send`  
**Category**: Manual/General  
**Used For**: Admin/Recruiter/Super Admin can send custom emails  
**Who Can Use**: Admin, Recruiter, Super Admin  
**Features**:
- To, CC, BCC recipients
- Custom subject and body (rich text editor)
- **Attachments**: Currently disabled (hardcoded to empty list)
- Uses **SendGrid** via `SendGridEmail.sendEmails()`

**Frontend**: `EmailForm.js`  
**Backend**: `EmailController.sendEmails()` → `SendGridEmail.sendEmails()`

---

### 6. **Timesheet Reminders** (Automated Notifications)
**Location**: Timesheet Management  
**Endpoint**: `POST /timesheets/reminders/send`  
**Category**: `TIMESHEET_REMINDER` (implied)  
**Used For**: Reminding employees to submit pending timesheets  
**Who Can Use**: Admin, Super Admin, HR Manager  
**Features**:
- Can send to specific employees or all with pending timesheets
- Customizable subject and message
- Personalized with employee name
- Uses company email as sender
- Returns success/failure list
- Uses **SendGrid** via `SendGridEmail.sendEmails()`

**Frontend**: Timesheet reminder interface  
**Backend**: `TimesheetReminderController` → `TimesheetReminderService.sendReminders()`

**Additional Endpoint**: `GET /timesheets/reminders/pending` - Get list of pending timesheets

---

### 7. **Bulk Mail** (Email Management)
**Location**: Recruiter/Admin tools  
**Endpoints**: 
- `POST /bulkmails` - Save bulk email
- `GET /bulkmails/all` - Get all email addresses
- `GET /bulkmails/{recruiterId}` - Get emails by recruiter

**Used For**: Managing email lists for recruiters  
**Who Can Use**: Admin, Recruiter, Super Admin  
**Note**: This is for storing email lists, not sending emails directly. Likely used in conjunction with manual email sending.

---

## 📊 Email Service Usage Summary

| Feature | Email Service Used | Status |
|---------|-------------------|--------|
| Send Login Details | **SendGrid** ✅ | Working |
| Forgot Password | **SendGrid** ✅ | Working |
| Change Password | **SendGrid** ✅ | Working |
| User Registration | **Gmail SMTP** ❌ | **Inconsistent!** |
| Manual Email Send | **SendGrid** ✅ | Working (no attachments) |
| Timesheet Reminders | **SendGrid** ✅ | Working |

---

## 🎯 Email Template System

All email features (except User Registration) use the template system:
- Templates stored in database (`message` table)
- Template categories:
  - `LOGIN_DETAILS`
  - `FORGOT_PASSWORD`
  - `CHANGE_PASSWORD`
  - (Custom templates can be added)

**Template Features**:
- Placeholder replacement (e.g., `{{emailID}}`, `{{temp_password}}`)
- Fallback to generic templates if not found
- Active/Inactive status

**Template Management**:
- Admin can manage templates via "Email Templates" UI
- Backend: `MessageController` (likely)

---

## 🔧 Technical Implementation

### Email Services:
1. **SendGridEmail Service** (Primary)
   - Main email sending service
   - Uses SendGrid API
   - Supports: To, CC, BCC, Attachments, HTML content

2. **JavaMailSender** (Legacy - Gmail SMTP)
   - Only used in `sendTemporaryPasswordEmail()`
   - **Should be replaced with SendGrid**

### Email Configuration:
- **SendGrid API Key**: `spring.sendgrid.api-key` (environment variable)
- **Gmail SMTP**: Hardcoded credentials in `MailConfig.java` ⚠️ **SECURITY ISSUE**

### Dynamic Sender Email:
- Determines sender email from employee's company
- Falls back to: `docs@saibersys.com`
- Configured via: `spring.mail.from` or company email in database

---

## 🚨 Known Issues & Improvements Needed

### Critical:
1. ❌ **User Registration** uses Gmail SMTP instead of SendGrid
2. ❌ **Hardcoded Gmail credentials** in `MailConfig.java` (security risk)

### Medium Priority:
3. ⚠️ **Attachments not enabled** in manual email endpoint
4. ⚠️ **No email validation** before sending
5. ⚠️ **Poor error handling** - doesn't throw exceptions

### Nice to Have:
6. 📝 **No email delivery status tracking**
7. 📝 **No email history/audit log**
8. 📝 **No rate limiting** on email endpoints
9. 📝 **No retry mechanism** for failed sends

---

## 📝 API Endpoints Summary

### Authentication/User Emails:
```
POST /auth/resetPassword
  Body: { "email": "...", "category": "LOGIN_DETAILS" | "FORGOT_PASSWORD" | "CHANGE_PASSWORD" }
  → Sends email based on category

POST /auth/register
  → Automatically sends temporary password email (uses Gmail SMTP)
```

### Manual Email Sending:
```
POST /email/send
  Body: {
    "fromEmail": "...",
    "toList": ["..."],
    "ccList": ["..."],
    "bccList": ["..."],
    "subject": "...",
    "body": "..."
  }
  → Sends custom email (attachments currently disabled)
```

### Timesheet Reminders:
```
POST /timesheets/reminders/send
  Body: {
    "employeeIds": ["..."],  // Optional - if empty, sends to all with pending
    "month": 1,              // Optional - defaults to current month
    "year": 2024,            // Optional - defaults to current year
    "subject": "...",        // Optional
    "message": "..."         // Optional
  }
  → Sends reminder emails

GET /timesheets/reminders/pending?month=1&year=2024
  → Returns list of employees with pending timesheets
```

### Bulk Mail Management:
```
GET /bulkmails/all
  → Returns all email addresses in system

GET /bulkmails/{recruiterId}
  → Returns emails for specific recruiter

POST /bulkmails?recruiterId=...
  → Saves bulk email entry
```

---

## 💡 Recommendations

1. **Standardize on SendGrid**: Update `sendTemporaryPasswordEmail()` to use SendGrid
2. **Remove Gmail Config**: If not needed, remove `MailConfig.java` entirely
3. **Enable Attachments**: Uncomment and fix attachment support in EmailController
4. **Add Email Validation**: Validate email addresses before sending
5. **Improve Error Handling**: Throw proper exceptions on failures
6. **Add Logging**: Replace System.out.println with proper logging
7. **Add Email Audit**: Log all sent emails for tracking
8. **Add Rate Limiting**: Prevent email spam/abuse

---

## 🎨 Frontend Integration

### Email-Related UI Components:
1. **EmployeeForm.js** - "Send Login Details" button
2. **EmailForm.js** - Manual email composition page
3. **ForgotPassword.js** - Password recovery form
4. **Login.js** - Login with forgot password link
5. **EmailTemplateList.js** - Manage email templates
6. **EmailTemplateEdit.js** - Edit email templates
7. Timesheet reminder interface

---

## 📈 Usage Statistics (Potential)

Current implementation doesn't track:
- Number of emails sent
- Success/failure rates
- Email delivery status
- Email history per employee

**Recommendation**: Add email audit/logging table to track all sent emails.

---

## ✅ What's Working Well

1. ✅ Flexible template system with database storage
2. ✅ Dynamic sender email based on company
3. ✅ Support for CC/BCC in manual emails
4. ✅ Personalized messages in timesheet reminders
5. ✅ Proper authorization (role-based access)
6. ✅ Placeholder replacement in templates

---

## 🔐 Security Considerations

1. ⚠️ **Gmail credentials hardcoded** - Must be moved to environment variables
2. ✅ **Role-based access** - Properly implemented
3. ⚠️ **No rate limiting** - Could be abused
4. ✅ **API key in environment** - SendGrid key properly configured (assumed)

---

This completes all email/mailing functionality in your HRMS system!

