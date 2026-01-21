# Group Admin Feature Implementation Plan

## Overview
Group Admins can manage multiple companies. When they log in, they can select which company to view, and all data will be filtered by that selected company.

## Requirements
1. ✅ Group Admin role created
2. ✅ Security configuration updated
3. ⏳ Company selector in sidebar for GROUP_ADMIN
4. ⏳ Store selected company in sessionStorage
5. ⏳ Filter all API calls by selected company for GROUP_ADMIN
6. ⏳ Super Admin can create Group Admin users and assign companies
7. ⏳ Fix working status dropdown issue

## Implementation Steps

### Backend Changes

#### 1. Role Enum ✅
- Added `GROUP_ADMIN` role with same permissions as ADMIN

#### 2. Security Configuration ✅
- Added GROUP_ADMIN to all relevant endpoints

#### 3. Company Filtering Logic (Need to implement)
- Update `EmployeeController` to accept optional `company_id` parameter
- When GROUP_ADMIN, filter by their assigned companies
- When SADMIN, no filtering (sees all)

#### 4. Get Companies for Group Admin
- Endpoint exists: `GET /user-company/user/{userId}`
- Returns list of companies assigned to user

### Frontend Changes

#### 1. Sidebar Company Selector
- For GROUP_ADMIN role, show company dropdown in sidebar
- Fetch companies from `/user-company/user/{userId}`
- Store selected company in `sessionStorage.setItem('selectedCompanyId', companyId)`
- Default to first company or last selected

#### 2. Company Filtering
- Add `company_id` parameter to all API calls when GROUP_ADMIN
- Use `sessionStorage.getItem('selectedCompanyId')`

#### 3. Super Admin UI
- Update user creation form to support GROUP_ADMIN role
- Allow selecting multiple companies for Group Admin users

## Current Status

✅ Completed:
- GROUP_ADMIN role added to backend
- Security configuration updated
- Working status dropdown fixed (companyId type conversion)

⏳ In Progress:
- Sidebar company selector
- Company filtering logic
- API parameter updates

## Next Steps

1. Update sidebar to show company selector for GROUP_ADMIN
2. Add company filtering to all relevant API calls
3. Update Super Admin user creation to support Group Admin with company assignment

