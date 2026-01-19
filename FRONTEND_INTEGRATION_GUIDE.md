# Frontend Integration Guide

This guide will help you integrate the frontend with the newly implemented backend features.

## 📁 Project Structure

```
quick-hrms-backend-master/
├── frontend/                    # React frontend (cloned from quick-hr repo)
│   ├── src/
│   │   ├── SharedComponents/
│   │   │   ├── services/       # API service files
│   │   │   └── httpClient .js  # Axios configuration
│   │   └── ...
├── src/                         # Backend Java source
├── API_DOCUMENTATION.md         # Complete API documentation
└── IMPLEMENTATION_SUMMARY.md    # Implementation details
```

## 🔧 Configuration

### 1. Environment Variables

Create `.env` file in the `frontend` directory (or update existing):

```env
REACT_APP_API_URL=http://localhost:8080
```

For production, update `.env.prod`:
```env
REACT_APP_API_URL=https://your-backend-url.com
```

For development, update `.env.dev`:
```env
REACT_APP_API_URL=http://localhost:8080
```

### 2. Backend CORS Configuration

The backend already has CORS configured to allow requests from the frontend. Make sure your backend is running on the port specified in `REACT_APP_API_URL`.

---

## 🆕 New Features to Integrate

### 1. HR Manager Role

**Location:** Update role handling in authentication components

**Files to update:**
- `src/SharedComponents/authUtils/authUtils.js` - Add HR_MANAGER to role checks
- `src/SharedComponents/layout/Sidebar.js` - Add HR_MANAGER to menu visibility logic
- `src/SharedComponents/layout/Navbar.js` - Update role-based UI elements

**Example:**
```javascript
const isAdmin = () => {
  const role = sessionStorage.getItem('role');
  return role === 'ADMIN' || role === 'SADMIN' || role === 'HR_MANAGER';
};
```

---

### 2. Visa Status Dropdown

**New Endpoint:** `GET /visa-status/options`

**Create new service file:** `src/SharedComponents/services/VisaStatusService.js`

```javascript
import { get } from "../httpClient ";

export async function getVisaStatusOptions() {
  try {
    const response = await get('/visa-status/options');
    if (response.status === 200) {
      return response.data;
    }
  } catch (error) {
    console.error("Error fetching visa status options:", error);
  }
  return [];
}
```

**Update Employee Form:** `src/Employee/EmployeeForm.js`

```javascript
import { getVisaStatusOptions } from '../SharedComponents/services/VisaStatusService';

// In component:
const [visaStatusOptions, setVisaStatusOptions] = useState([]);

useEffect(() => {
  getVisaStatusOptions().then(options => {
    setVisaStatusOptions(options);
  });
}, []);

// In form:
<select name="visaStatus" value={formData.visaStatus} onChange={handleChange}>
  <option value="">Select Visa Status</option>
  {visaStatusOptions.map(option => (
    <option key={option.value} value={option.value}>
      {option.displayName}
    </option>
  ))}
</select>
```

---

### 3. Address Verification

**New Service File:** `src/SharedComponents/services/AddressVerificationService.js`

```javascript
import { get, post, put } from "../httpClient ";

export async function getAddressVerification(employeeId) {
  try {
    const response = await get(`/address-verification/${employeeId}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching address verification:", error);
    return null;
  }
}

export async function createOrUpdateAddressVerification(employeeId, data) {
  try {
    const response = await post(`/address-verification/${employeeId}`, data);
    return response.data;
  } catch (error) {
    console.error("Error saving address verification:", error);
    throw error;
  }
}

export async function verifyHomeAddress(employeeId, verifiedBy) {
  try {
    const response = await post(
      `/address-verification/${employeeId}/verify-home?verifiedBy=${verifiedBy}`
    );
    return response.data;
  } catch (error) {
    console.error("Error verifying home address:", error);
    throw error;
  }
}

export async function verifyWorkAddress(employeeId, verifiedBy) {
  try {
    const response = await post(
      `/address-verification/${employeeId}/verify-work?verifiedBy=${verifiedBy}`
    );
    return response.data;
  } catch (error) {
    console.error("Error verifying work address:", error);
    throw error;
  }
}

export async function getAllAddressVerifications() {
  try {
    const response = await get('/address-verification/all');
    return response.data;
  } catch (error) {
    console.error("Error fetching all address verifications:", error);
    return [];
  }
}
```

**New Component:** `src/EmployeeAccess/AddressVerification.js`

```javascript
import React, { useState, useEffect } from 'react';
import { 
  getAddressVerification, 
  createOrUpdateAddressVerification,
  verifyHomeAddress,
  verifyWorkAddress 
} from '../SharedComponents/services/AddressVerificationService';
import { get } from '../SharedComponents/httpClient ';

function AddressVerification({ employeeId }) {
  const [addressData, setAddressData] = useState({
    homeAddress: '',
    workAddress: '',
    isWorking: false,
    homeAddressVerified: false,
    workAddressVerified: false
  });
  const [loading, setLoading] = useState(false);
  const userEmail = sessionStorage.getItem('email');
  const userRole = sessionStorage.getItem('role');
  const isAdmin = userRole === 'ADMIN' || userRole === 'SADMIN' || userRole === 'HR_MANAGER';

  useEffect(() => {
    loadAddressData();
  }, [employeeId]);

  const loadAddressData = async () => {
    const data = await getAddressVerification(employeeId);
    if (data) {
      setAddressData(data);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      await createOrUpdateAddressVerification(employeeId, {
        homeAddress: addressData.homeAddress,
        workAddress: addressData.workAddress,
        isWorking: addressData.isWorking
      });
      alert('Address saved successfully');
      loadAddressData();
    } catch (error) {
      alert('Error saving address');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyHome = async () => {
    try {
      await verifyHomeAddress(employeeId, userEmail);
      alert('Home address verified');
      loadAddressData();
    } catch (error) {
      alert('Error verifying address');
    }
  };

  const handleVerifyWork = async () => {
    try {
      await verifyWorkAddress(employeeId, userEmail);
      alert('Work address verified');
      loadAddressData();
    } catch (error) {
      alert('Error verifying address');
    }
  };

  return (
    <div className="address-verification">
      <h3>Address Verification</h3>
      
      <div className="form-group">
        <label>Home Address</label>
        <textarea
          value={addressData.homeAddress || ''}
          onChange={(e) => setAddressData({...addressData, homeAddress: e.target.value})}
          disabled={!isAdmin && addressData.homeAddressVerified}
        />
        {addressData.homeAddressVerified && (
          <span className="verified-badge">✓ Verified</span>
        )}
        {isAdmin && !addressData.homeAddressVerified && (
          <button onClick={handleVerifyHome}>Verify Home Address</button>
        )}
      </div>

      <div className="form-group">
        <label>Work Address</label>
        <textarea
          value={addressData.workAddress || ''}
          onChange={(e) => setAddressData({...addressData, workAddress: e.target.value})}
          disabled={!isAdmin && addressData.workAddressVerified}
        />
        {addressData.workAddressVerified && (
          <span className="verified-badge">✓ Verified</span>
        )}
        {isAdmin && !addressData.workAddressVerified && (
          <button onClick={handleVerifyWork}>Verify Work Address</button>
        )}
      </div>

      <div className="form-group">
        <label>
          <input
            type="checkbox"
            checked={addressData.isWorking || false}
            onChange={(e) => setAddressData({...addressData, isWorking: e.target.checked})}
          />
          Currently Working
        </label>
      </div>

      <button onClick={handleSave} disabled={loading}>
        {loading ? 'Saving...' : 'Save Address'}
      </button>
    </div>
  );
}

export default AddressVerification;
```

---

### 4. Timesheet Reminders

**New Service File:** `src/SharedComponents/services/TimesheetReminderService.js`

```javascript
import { get, post } from "../httpClient ";

export async function sendReminders(request) {
  try {
    const response = await post('/timesheets/reminders/send', request);
    return response.data;
  } catch (error) {
    console.error("Error sending reminders:", error);
    throw error;
  }
}

export async function getPendingTimesheets(month, year) {
  try {
    const params = new URLSearchParams();
    if (month) params.append('month', month);
    if (year) params.append('year', year);
    
    const response = await get(`/timesheets/reminders/pending?${params.toString()}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching pending timesheets:", error);
    return { pendingTimesheets: [], pendingCount: 0 };
  }
}
```

**New Component:** `src/TimeSheets/TimesheetReminders.js`

```javascript
import React, { useState, useEffect } from 'react';
import { sendReminders, getPendingTimesheets } from '../SharedComponents/services/TimesheetReminderService';

function TimesheetReminders() {
  const [pendingTimesheets, setPendingTimesheets] = useState([]);
  const [selectedEmployees, setSelectedEmployees] = useState([]);
  const [reminderData, setReminderData] = useState({
    subject: 'Timesheet Submission Reminder',
    message: 'This is a reminder to submit your timesheet for the current period.'
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    loadPendingTimesheets();
  }, []);

  const loadPendingTimesheets = async () => {
    const data = await getPendingTimesheets();
    setPendingTimesheets(data.pendingTimesheets || []);
  };

  const handleSendReminders = async () => {
    setLoading(true);
    try {
      const request = {
        employeeIds: selectedEmployees.length > 0 ? selectedEmployees : null,
        subject: reminderData.subject,
        message: reminderData.message
      };
      
      const response = await sendReminders(request);
      setResult(response);
      alert(`Reminders sent! Success: ${response.successCount}, Failed: ${response.failureCount}`);
    } catch (error) {
      alert('Error sending reminders');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="timesheet-reminders">
      <h2>Timesheet Reminders</h2>
      
      <div className="pending-list">
        <h3>Pending Timesheets ({pendingTimesheets.length})</h3>
        <table>
          <thead>
            <tr>
              <th>Select</th>
              <th>Employee</th>
              <th>Email</th>
              <th>Project</th>
              <th>Month/Year</th>
            </tr>
          </thead>
          <tbody>
            {pendingTimesheets.map((ts) => (
              <tr key={ts.masterId}>
                <td>
                  <input
                    type="checkbox"
                    checked={selectedEmployees.includes(ts.employeeId)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedEmployees([...selectedEmployees, ts.employeeId]);
                      } else {
                        setSelectedEmployees(selectedEmployees.filter(id => id !== ts.employeeId));
                      }
                    }}
                  />
                </td>
                <td>{ts.employeeName}</td>
                <td>{ts.employeeEmail}</td>
                <td>{ts.projectName}</td>
                <td>{ts.month}/{ts.year}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="reminder-form">
        <h3>Send Reminder</h3>
        <div>
          <label>Subject:</label>
          <input
            type="text"
            value={reminderData.subject}
            onChange={(e) => setReminderData({...reminderData, subject: e.target.value})}
          />
        </div>
        <div>
          <label>Message:</label>
          <textarea
            value={reminderData.message}
            onChange={(e) => setReminderData({...reminderData, message: e.target.value})}
            rows={5}
          />
        </div>
        <button onClick={handleSendReminders} disabled={loading}>
          {loading ? 'Sending...' : 'Send Reminders'}
        </button>
        {selectedEmployees.length === 0 && (
          <p className="info">No employees selected. Will send to all employees with pending timesheets.</p>
        )}
      </div>

      {result && (
        <div className="result">
          <h4>Results:</h4>
          <p>Success: {result.successCount}</p>
          <p>Failed: {result.failureCount}</p>
        </div>
      )}
    </div>
  );
}

export default TimesheetReminders;
```

**Add to Sidebar:** Add a menu item for admins/HR managers to access reminders.

---

### 5. Template Download

**Update:** `src/SharedComponents/services/TimeSheetService.js`

Add these functions:

```javascript
export async function downloadTimesheetTemplate(templateType = 'weekly') {
  try {
    const response = await get(`/timeSheets/templates/download?templateType=${templateType}`, {
      responseType: 'blob'
    });
    
    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `timesheet_${templateType}_template.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  } catch (error) {
    console.error("Error downloading template:", error);
    alert('Error downloading template');
  }
}

export async function getAvailableTemplates() {
  try {
    const response = await get('/timeSheets/templates/list');
    return response.data;
  } catch (error) {
    console.error("Error fetching templates:", error);
    return [];
  }
}
```

**Add Download Button:** In `src/TimeSheets/TimeSheets.js` or create a new component:

```javascript
import { downloadTimesheetTemplate, getAvailableTemplates } from '../SharedComponents/services/TimeSheetService';

function TemplateDownload() {
  const [templates, setTemplates] = useState([]);

  useEffect(() => {
    getAvailableTemplates().then(setTemplates);
  }, []);

  const handleDownload = (templateType) => {
    downloadTimesheetTemplate(templateType);
  };

  return (
    <div className="template-download">
      <h3>Download Timesheet Templates</h3>
      {templates.map(template => (
        <button
          key={template.type}
          onClick={() => handleDownload(template.type)}
        >
          Download {template.name}
        </button>
      ))}
    </div>
  );
}
```

---

## 🔄 Integration Steps

1. **Update Environment Variables**
   - Create/update `.env` file with backend URL

2. **Install Dependencies** (if needed)
   ```bash
   cd frontend
   npm install
   ```

3. **Create New Service Files**
   - `VisaStatusService.js`
   - `AddressVerificationService.js`
   - `TimesheetReminderService.js`
   - Update `TimeSheetService.js`

4. **Create New Components**
   - `AddressVerification.js`
   - `TimesheetReminders.js`
   - Update existing components to use new services

5. **Update Existing Components**
   - Add HR_MANAGER role checks
   - Update visa status dropdown
   - Add address verification to employee details
   - Add reminder functionality to timesheet management
   - Add template download buttons

6. **Update Navigation**
   - Add new menu items for admins/HR managers
   - Update role-based visibility

7. **Test Integration**
   - Test all new endpoints
   - Verify authentication
   - Test role-based access

---

## 📝 Notes

- All new endpoints require JWT authentication (already handled by httpClient)
- Role checks should include HR_MANAGER alongside ADMIN
- Error handling should be consistent with existing patterns
- Use the existing UI components and styling patterns

---

## 🐛 Troubleshooting

**CORS Issues:**
- Make sure backend CORS is configured correctly
- Check that `REACT_APP_API_URL` matches backend URL

**Authentication Issues:**
- Verify JWT token is being sent in headers
- Check token expiration
- Verify role is stored in sessionStorage

**API Errors:**
- Check browser console for error messages
- Verify backend is running
- Check API_DOCUMENTATION.md for correct endpoint URLs

---

## 📚 Additional Resources

- See `API_DOCUMENTATION.md` for complete API reference
- See `IMPLEMENTATION_SUMMARY.md` for backend implementation details
- Backend repository: Current workspace
- Frontend repository: https://github.com/sainathaluri/quick-hr

