# 🏢 HR & Payroll Module — Frontend Integration Guide

> **Project**: REMS Backend  
> **Module**: Human Resources & Payroll Management  
> **Version**: 1.0  
> **Date**: March 24, 2026  
> **Base URL**: `/api/hr`  
> **Auth**: All endpoints require JWT Bearer token in `Authorization` header

---

## 📑 Table of Contents

1. [Module Overview](#-module-overview)
2. [Global Response Format](#-global-response-format)
3. [Pagination Standard](#-pagination-standard)
4. [Enum Reference](#-enum-reference)
5. [Department APIs](#-1-department-management)
6. [Employee APIs](#-2-employee-management)
7. [Attendance APIs](#-3-attendance-management)
8. [Leave APIs](#-4-leave-management)
9. [Salary Amendment APIs](#-5-salary-amendments)
10. [Payroll & Salary Slip APIs](#-6-payroll--salary-slips)
11. [Dashboard & Analytics APIs](#-7-dashboard--analytics)
12. [Suggested Pages & UI Components](#-suggested-pages--ui-components)
13. [Workflow Diagrams](#-workflow-diagrams)

---

## 🌐 Module Overview

```
/api/hr/department          → Department CRUD, search, active list
/api/hr/employee            → Employee CRUD, search, filter by dept, terminate
/api/hr/attendance          → Mark single/bulk, daily view, summary
/api/hr/leave               → Apply, approve/reject workflow, pending list
/api/hr/salary-amendment    → Month-specific additions/deductions
/api/hr/payroll             → Process payroll, salary slips, dashboard, analytics
```

### Architecture at a Glance

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│  Department  │◄────│   Employee   │────►│   Allowance   │
│              │     │              │────►│   Deduction    │
└─────────────┘     └──────┬───────┘     └───────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌───────────┐  ┌──────────────────┐
   │  Attendance  │  │   Leave   │  │ Salary Amendment │
   └─────────────┘  └───────────┘  └────────┬─────────┘
                                             │
                                             ▼
                                   ┌─────────────────┐
                                   │  Payroll Engine  │
                                   │  (Process Month) │
                                   └────────┬────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │   Salary Slips   │
                                   └─────────────────┘
```

---

## 📦 Global Response Format

Every API returns this consistent structure:

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": { ... }
}
```

### Response Codes

| Code   | Meaning             | When                                       |
|--------|---------------------|---------------------------------------------|
| `0000` | ✅ Success           | Request processed successfully               |
| `0001` | ⚠️ No Data Found    | ID not found / empty result                 |
| `0002` | ❌ Invalid Parameter | Validation failed / bad input               |
| `9999` | 🔥 System Failure   | Unexpected server error                     |

### Frontend Helper (Axios)

```javascript
// api/hrService.js
import axios from 'axios';

const API = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
});

API.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response helper
export const handleResponse = (response) => {
  const { responseCode, responseMessage, data } = response.data;
  if (responseCode === '0000') return { success: true, data };
  return { success: false, message: responseMessage, data };
};

export default API;
```

---

## 📄 Pagination Standard

All paginated endpoints accept these **query parameters**:

| Parameter  | Type    | Default        | Description                              |
|------------|---------|----------------|------------------------------------------|
| `page`     | int     | `0`            | Zero-based page index                    |
| `size`     | int     | `10`           | Number of records per page               |
| `sortBy`   | string  | `createdDate`  | Field name to sort by                    |
| `sortDir`  | string  | `desc`         | Sort direction — `asc` or `desc`         |

### Paginated Response Shape

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "content": [ ... ],
    "totalElements": 45,
    "totalPages": 5,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

### Frontend Pagination Usage

```javascript
const fetchEmployees = async (orgId, page = 0, size = 10) => {
  const res = await API.get(`/api/hr/employee/organization/${orgId}`, {
    params: { page, size, sortBy: 'fullName', sortDir: 'asc' }
  });
  return handleResponse(res);
};
```

---

## 🏷️ Enum Reference

Use these exact string values when sending data to the backend:

### Employee Status
| Value         | Badge Color | Description             |
|---------------|-------------|-------------------------|
| `ACTIVE`      | 🟢 Green    | Currently employed       |
| `INACTIVE`    | 🟡 Yellow   | Temporarily inactive     |
| `TERMINATED`  | 🔴 Red      | Employment ended         |
| `ON_LEAVE`    | 🔵 Blue     | Currently on leave       |

### Employment Type
| Value       | Description            |
|-------------|------------------------|
| `FULL_TIME` | Full-time employee     |
| `PART_TIME` | Part-time employee     |
| `CONTRACT`  | Contract-based         |
| `INTERN`    | Internship             |

### Gender
| Value    |
|----------|
| `MALE`   |
| `FEMALE` |
| `OTHER`  |

### Attendance Status
| Value      | Badge Color | Description              |
|------------|-------------|--------------------------|
| `PRESENT`  | 🟢 Green    | Marked present            |
| `ABSENT`   | 🔴 Red      | Marked absent             |
| `HALF_DAY` | 🟡 Yellow   | Half-day attendance       |
| `LATE`     | 🟠 Orange   | Late arrival              |
| `ON_LEAVE` | 🔵 Blue     | On approved leave         |

### Leave Type
| Value       | Description          |
|-------------|----------------------|
| `ANNUAL`    | Annual / Earned      |
| `SICK`      | Sick leave           |
| `CASUAL`    | Casual leave         |
| `MATERNITY` | Maternity leave      |
| `PATERNITY` | Paternity leave      |
| `UNPAID`    | Unpaid leave         |

### Leave Status
| Value       | Badge Color | Description               |
|-------------|-------------|---------------------------|
| `PENDING`   | 🟡 Yellow   | Awaiting approval          |
| `APPROVED`  | 🟢 Green    | Approved by admin          |
| `REJECTED`  | 🔴 Red      | Rejected by admin          |
| `CANCELLED` | ⚫ Grey     | Cancelled by employee      |

### Salary Amendment Type
| Value       | Description                       |
|-------------|-----------------------------------|
| `ADDITION`  | Bonus, overtime, extra pay        |
| `DEDUCTION` | Penalty, advance recovery, etc.   |

### Salary Slip Status
| Value       | Badge Color | Description                      |
|-------------|-------------|----------------------------------|
| `GENERATED` | 🟡 Yellow   | Slip created, payment pending    |
| `PAID`      | 🟢 Green    | Salary disbursed                 |
| `CANCELLED` | 🔴 Red      | Slip voided                      |

### Payroll Status
| Value        | Badge Color | Description                     |
|--------------|-------------|---------------------------------|
| `DRAFT`      | ⚫ Grey     | Created but not processed       |
| `PROCESSING` | 🟡 Yellow   | Currently being processed       |
| `COMPLETED`  | 🟢 Green    | Payroll fully processed         |
| `CANCELLED`  | 🔴 Red      | Payroll run cancelled           |

---

## 🏛️ 1. Department Management

**Base URL**: `/api/hr/department`

---

### 1.1 Create Department

```
POST /api/hr/department/create
```

**Request Body:**
```json
{
  "organizationId": 1,
  "name": "Engineering",
  "description": "Software Development Team",
  "departmentHead": "Ahmed Khan",
  "budgetAllocated": 500000.00,
  "isActive": true
}
```

| Field              | Type       | Required | Notes                          |
|--------------------|------------|----------|--------------------------------|
| `organizationId`   | Long       | ✅ Yes   | Current org ID                 |
| `name`             | String     | ✅ Yes   | Department name                |
| `description`      | String     | No       | Brief description              |
| `departmentHead`   | String     | No       | Name of department head        |
| `budgetAllocated`  | BigDecimal | No       | Monthly budget allocated       |
| `isActive`         | Boolean    | No       | Defaults to `true`             |

**Success Response:**
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "id": 1,
    "organizationId": 1,
    "name": "Engineering",
    "description": "Software Development Team",
    "departmentHead": "Ahmed Khan",
    "budgetAllocated": 500000.00,
    "isActive": true,
    "createdDate": "2026-03-24T10:30:00",
    "updatedDate": "2026-03-24T10:30:00",
    "employees": []
  }
}
```

---

### 1.2 Update Department

```
PUT /api/hr/department/update/{id}
```

**Request Body:** Same fields as create — only include fields you want to update (partial update supported).

---

### 1.3 Get Department by ID

```
GET /api/hr/department/{id}
```

**Response `data`:**
```json
{
  "department": {
    "id": 1,
    "organizationId": 1,
    "name": "Engineering",
    "description": "Software Development Team",
    "departmentHead": "Ahmed Khan",
    "budgetAllocated": 500000.00,
    "isActive": true,
    "createdDate": "2026-03-24T10:30:00",
    "updatedDate": "2026-03-24T10:30:00",
    "employees": [ ... ]
  },
  "employeeCount": 12
}
```

---

### 1.4 List Departments (Paginated)

```
GET /api/hr/department/organization/{organizationId}?page=0&size=10&sortBy=createdDate&sortDir=desc
```

---

### 1.5 Active Departments (Dropdown List)

```
GET /api/hr/department/active/{organizationId}
```

> 💡 **Use this for dropdowns** — returns a flat array of active departments (no pagination).

**Response `data`:** `[ { id, name, ... }, ... ]`

---

### 1.6 Search Departments

```
GET /api/hr/department/search/{organizationId}?name=eng&page=0&size=10
```

---

### 1.7 Delete Department

```
DELETE /api/hr/department/delete/{id}
```

> ⚠️ Will **fail** if department has employees. Reassign employees first.

---

## 👤 2. Employee Management

**Base URL**: `/api/hr/employee`

---

### 2.1 Create Employee (with Allowances & Deductions)

```
POST /api/hr/employee/create
```

**Request Body:**
```json
{
  "organizationId": 1,
  "employeeCode": "EMP-001",
  "fullName": "Ali Hassan",
  "email": "ali@company.com",
  "phone": "03001234567",
  "cnic": "35201-1234567-1",
  "address": "123 Main Street",
  "city": "Lahore",
  "gender": "MALE",
  "dateOfBirth": "1995-06-15",
  "departmentId": 1,
  "designation": "Software Engineer",
  "employmentType": "FULL_TIME",
  "joiningDate": "2026-01-15",
  "basicSalary": 80000.00,
  "bankName": "HBL",
  "bankAccountNumber": "1234567890",
  "bankBranchCode": "0123",
  "allowances": [
    {
      "allowanceName": "House Rent",
      "amount": 20000.00,
      "isRecurring": true,
      "isActive": true
    },
    {
      "allowanceName": "Medical",
      "amount": 5000.00,
      "isRecurring": true,
      "isActive": true
    }
  ],
  "deductions": [
    {
      "deductionName": "Provident Fund",
      "amount": 4000.00,
      "isRecurring": true,
      "isActive": true
    },
    {
      "deductionName": "Tax",
      "amount": 6000.00,
      "isRecurring": true,
      "isActive": true
    }
  ]
}
```

| Field              | Type        | Required | Notes                                       |
|--------------------|-------------|----------|---------------------------------------------|
| `organizationId`   | Long        | ✅ Yes   | Current org ID                              |
| `employeeCode`     | String      | No       | Unique code (e.g., `EMP-001`)               |
| `fullName`         | String      | ✅ Yes   | Employee full name                          |
| `email`            | String      | No       | Email address                               |
| `phone`            | String      | No       | Phone number                                |
| `cnic`             | String      | No       | National ID (CNIC)                          |
| `address`          | String      | No       | Street address                              |
| `city`             | String      | No       | City                                        |
| `gender`           | String      | No       | `MALE` / `FEMALE` / `OTHER`                 |
| `dateOfBirth`      | String      | No       | Format: `YYYY-MM-DD`                        |
| `departmentId`     | Long        | ✅ Yes   | Foreign key → Department                    |
| `designation`      | String      | No       | Job title                                   |
| `employmentType`   | String      | No       | Defaults to `FULL_TIME`                     |
| `joiningDate`      | String      | No       | Format: `YYYY-MM-DD`                        |
| `basicSalary`      | BigDecimal  | ✅ Yes   | Monthly base salary                         |
| `bankName`         | String      | No       | Bank name                                   |
| `bankAccountNumber`| String      | No       | Account number                              |
| `bankBranchCode`   | String      | No       | Branch code                                 |
| `allowances`       | Array       | No       | List of allowances (see table below)        |
| `deductions`       | Array       | No       | List of deductions (see table below)        |

**Allowance / Deduction Object:**

| Field           | Type       | Required | Notes                             |
|-----------------|------------|----------|-----------------------------------|
| `allowanceName` / `deductionName` | String | ✅ Yes | Name (e.g., "House Rent", "Tax") |
| `amount`        | BigDecimal | ✅ Yes   | Monthly amount                    |
| `isRecurring`   | Boolean    | No       | Defaults to `true`                |
| `isActive`      | Boolean    | No       | Defaults to `true`                |

**Employee Response Shape:**
```json
{
  "id": 1,
  "organizationId": 1,
  "employeeCode": "EMP-001",
  "fullName": "Ali Hassan",
  "email": "ali@company.com",
  "phone": "03001234567",
  "cnic": "35201-1234567-1",
  "address": "123 Main Street",
  "city": "Lahore",
  "gender": "MALE",
  "dateOfBirth": "1995-06-15",
  "profileImageUrl": null,
  "designation": "Software Engineer",
  "employmentType": "FULL_TIME",
  "status": "ACTIVE",
  "joiningDate": "2026-01-15",
  "terminationDate": null,
  "department": {
    "id": 1,
    "organizationId": 1,
    "name": "Engineering",
    "description": "Software Development Team",
    "departmentHead": "Ahmed Khan",
    "budgetAllocated": 500000.00,
    "isActive": true,
    "createdDate": "2026-03-24T10:30:00",
    "updatedDate": "2026-03-24T10:30:00"
  },
  "departmentId": 1,
  "basicSalary": 80000.00,
  "bankName": "HBL",
  "bankAccountNumber": "1234567890",
  "bankBranchCode": "0123",
  "allowances": [
    { "id": 1, "organizationId": 1, "employeeId": 1, "allowanceName": "House Rent", "amount": 20000.00, "isRecurring": true, "isActive": true },
    { "id": 2, "organizationId": 1, "employeeId": 1, "allowanceName": "Medical", "amount": 5000.00, "isRecurring": true, "isActive": true }
  ],
  "deductions": [
    { "id": 1, "organizationId": 1, "employeeId": 1, "deductionName": "Provident Fund", "amount": 4000.00, "isRecurring": true, "isActive": true },
    { "id": 2, "organizationId": 1, "employeeId": 1, "deductionName": "Tax", "amount": 6000.00, "isRecurring": true, "isActive": true }
  ],
  "createdDate": "2026-03-24T10:30:00",
  "updatedDate": "2026-03-24T10:30:00"
}
```

---

### 2.2 Update Employee

```
PUT /api/hr/employee/update/{id}
```

**Request Body:** Same as create — partial update supported. If `allowances` or `deductions` arrays are sent, they **replace** the existing ones entirely.

---

### 2.3 Get Employee by ID

```
GET /api/hr/employee/{id}
```

---

### 2.4 List Employees by Organization (Paginated)

```
GET /api/hr/employee/organization/{organizationId}?page=0&size=10&sortBy=fullName&sortDir=asc
```

---

### 2.5 List Employees by Department (Paginated)

```
GET /api/hr/employee/department/{organizationId}/{departmentId}?page=0&size=10&sortBy=createdDate&sortDir=desc
```

---

### 2.6 Search Employees

```
GET /api/hr/employee/search/{organizationId}?keyword=ali&page=0&size=10&sortBy=fullName&sortDir=asc
```

> 🔍 Searches across: `fullName`, `email`, `employeeCode`, `designation`

---

### 2.7 Terminate Employee

```
PUT /api/hr/employee/terminate/{id}
```

> Sets status to `TERMINATED` and records `terminationDate` as today.

---

### 2.8 Delete Employee

```
DELETE /api/hr/employee/delete/{id}
```

> ⚠️ **Hard delete** — permanently removes employee and all associated allowances/deductions.

---

## 📅 3. Attendance Management

**Base URL**: `/api/hr/attendance`

---

### 3.1 Mark Single Attendance

```
POST /api/hr/attendance/mark
```

**Request Body:**
```json
{
  "organizationId": 1,
  "employeeId": 1,
  "attendanceDate": "2026-03-24",
  "checkInTime": "09:00:00",
  "checkOutTime": "18:00:00",
  "status": "PRESENT",
  "hoursWorked": 8.0,
  "remarks": ""
}
```

| Field            | Type    | Required | Notes                                              |
|------------------|---------|----------|----------------------------------------------------|
| `organizationId` | Long    | ✅ Yes   | Current org ID                                     |
| `employeeId`     | Long    | ✅ Yes   | Employee ID                                        |
| `attendanceDate` | String  | ✅ Yes   | Format: `YYYY-MM-DD`                               |
| `checkInTime`    | String  | No       | Format: `HH:mm:ss`                                 |
| `checkOutTime`   | String  | No       | Format: `HH:mm:ss`                                 |
| `status`         | String  | No       | `PRESENT` (default), `ABSENT`, `HALF_DAY`, `LATE`, `ON_LEAVE` |
| `hoursWorked`    | Double  | No       | Total hours worked                                 |
| `remarks`        | String  | No       | Any notes                                          |

> ⚠️ Returns error if attendance already exists for this employee + date. Use update endpoint instead.

---

### 3.2 Mark Bulk Attendance

```
POST /api/hr/attendance/mark-bulk
```

**Request Body:** Array of attendance objects:
```json
[
  { "organizationId": 1, "employeeId": 1, "attendanceDate": "2026-03-24", "status": "PRESENT", "checkInTime": "09:00:00" },
  { "organizationId": 1, "employeeId": 2, "attendanceDate": "2026-03-24", "status": "ABSENT" },
  { "organizationId": 1, "employeeId": 3, "attendanceDate": "2026-03-24", "status": "LATE", "checkInTime": "10:15:00" }
]
```

**Response `data`:**
```json
{
  "successCount": 3,
  "skippedCount": 0
}
```

> 💡 **Best for daily attendance page** — submit all employees at once. Duplicates are skipped, not errored.

---

### 3.3 Update Attendance

```
PUT /api/hr/attendance/update/{id}
```

---

### 3.4 Get Attendance by Employee (Paginated)

```
GET /api/hr/attendance/employee/{employeeId}?page=0&size=30&sortBy=attendanceDate&sortDir=desc
```

---

### 3.5 Get Attendance by Date Range

```
GET /api/hr/attendance/employee/{employeeId}/range?startDate=2026-03-01&endDate=2026-03-31
```

> 📆 Dates must be in `YYYY-MM-DD` format.

---

### 3.6 Get Daily Attendance (All Employees)

```
GET /api/hr/attendance/daily/{organizationId}?date=2026-03-24
```

> 💡 **Use for the daily attendance view/grid** — returns all attendance records for a specific day.

---

### 3.7 Get Attendance Summary

```
GET /api/hr/attendance/summary/{employeeId}?startDate=2026-03-01&endDate=2026-03-31
```

**Response `data`:**
```json
{
  "present": 18,
  "absent": 3,
  "halfDay": 1,
  "late": 2,
  "onLeave": 2,
  "totalWorkingDays": 26
}
```

> 💡 Great for **employee profile attendance stats** and **monthly attendance report cards**.

---

### 3.8 Delete Attendance

```
DELETE /api/hr/attendance/delete/{id}
```

---

## 🌴 4. Leave Management

**Base URL**: `/api/hr/leave`

---

### 4.1 Apply for Leave

```
POST /api/hr/leave/apply
```

**Request Body:**
```json
{
  "organizationId": 1,
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-04-01",
  "endDate": "2026-04-05",
  "reason": "Family vacation"
}
```

| Field            | Type   | Required | Notes                                              |
|------------------|--------|----------|----------------------------------------------------|
| `organizationId` | Long   | ✅ Yes   | Current org ID                                     |
| `employeeId`     | Long   | ✅ Yes   | Employee ID                                        |
| `leaveType`      | String | ✅ Yes   | `ANNUAL`, `SICK`, `CASUAL`, `MATERNITY`, `PATERNITY`, `UNPAID` |
| `startDate`      | String | ✅ Yes   | Format: `YYYY-MM-DD`                               |
| `endDate`        | String | ✅ Yes   | Format: `YYYY-MM-DD`                               |
| `reason`         | String | No       | Reason for leave                                   |

**Response — Leave Object:**
```json
{
  "id": 1,
  "organizationId": 1,
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-04-01",
  "endDate": "2026-04-05",
  "totalDays": 5,
  "reason": "Family vacation",
  "status": "PENDING",
  "approvedBy": null,
  "approvedDate": null,
  "createdDate": "2026-03-24T10:30:00",
  "updatedDate": "2026-03-24T10:30:00"
}
```

---

### 4.2 Approve Leave

```
PUT /api/hr/leave/approve/{id}?approvedBy=Admin User
```

| Parameter    | Type   | Required | Notes                    |
|--------------|--------|----------|--------------------------|
| `approvedBy` | String | ✅ Yes   | Query param — admin name |

---

### 4.3 Reject Leave

```
PUT /api/hr/leave/reject/{id}?rejectedBy=Admin User
```

---

### 4.4 Cancel Leave

```
PUT /api/hr/leave/cancel/{id}
```

---

### 4.5 Get Leave by ID

```
GET /api/hr/leave/{id}
```

---

### 4.6 Get Leaves by Employee (Paginated)

```
GET /api/hr/leave/employee/{employeeId}?page=0&size=10&sortBy=createdDate&sortDir=desc
```

---

### 4.7 Get All Leaves by Organization (Paginated)

```
GET /api/hr/leave/organization/{organizationId}?page=0&size=10&sortBy=createdDate&sortDir=desc
```

---

### 4.8 Get Pending Leaves (Admin View)

```
GET /api/hr/leave/pending/{organizationId}?page=0&size=10&sortBy=createdDate&sortDir=desc
```

> 💡 **Use this on the HR dashboard** to show a "Pending Approvals" section.

---

### 4.9 Delete Leave

```
DELETE /api/hr/leave/delete/{id}
```

---

## 💰 5. Salary Amendments

**Base URL**: `/api/hr/salary-amendment`

> Salary amendments are **month-specific one-time additions or deductions** — bonuses, penalties, overtime, advance recovery, etc.

---

### 5.1 Create Amendment

```
POST /api/hr/salary-amendment/create
```

**Request Body:**
```json
{
  "organizationId": 1,
  "employeeId": 1,
  "amount": 5000.00,
  "amendmentType": "ADDITION",
  "description": "Overtime bonus for March",
  "status": "APPROVED",
  "salaryMonth": 3,
  "salaryYear": 2026
}
```

| Field           | Type       | Required | Notes                                  |
|-----------------|------------|----------|----------------------------------------|
| `organizationId`| Long       | ✅ Yes   | Current org ID                         |
| `employeeId`    | Long       | ✅ Yes   | Employee ID                            |
| `amount`        | BigDecimal | ✅ Yes   | Amendment amount                       |
| `amendmentType` | String     | ✅ Yes   | `ADDITION` or `DEDUCTION`              |
| `description`   | String     | No       | Reason / note                          |
| `status`        | String     | No       | Defaults to `APPROVED`                 |
| `salaryMonth`   | Integer    | ✅ Yes   | Target month (1–12)                    |
| `salaryYear`    | Integer    | ✅ Yes   | Target year (e.g., 2026)               |

---

### 5.2 Update Amendment

```
PUT /api/hr/salary-amendment/update/{id}
```

---

### 5.3 Get Amendments by Employee

```
GET /api/hr/salary-amendment/employee/{employeeId}
```

---

### 5.4 Get Amendments by Organization & Month

```
GET /api/hr/salary-amendment/organization/{organizationId}?month=3&year=2026
```

---

### 5.5 Delete Amendment

```
DELETE /api/hr/salary-amendment/delete/{id}
```

---

## 📊 6. Payroll & Salary Slips

**Base URL**: `/api/hr/payroll`

---

### 6.1 🚀 Process Payroll (Bulk — All Active Employees)

```
POST /api/hr/payroll/process
```

**Request Body:**
```json
{
  "organizationId": 1,
  "payrollMonth": 3,
  "payrollYear": 2026,
  "processedBy": "Admin User"
}
```

**Salary Calculation Formula:**
```
Gross Salary  = Basic Salary + Total Allowances
Net Salary    = Gross Salary - Total Deductions + Net Amendments

Where:
  Net Amendments = Sum(ADDITION amounts) - Sum(DEDUCTION amounts)
```

**Response `data`:**
```json
{
  "payroll": {
    "id": 1,
    "organizationId": 1,
    "payrollMonth": 3,
    "payrollYear": 2026,
    "status": "COMPLETED",
    "totalEmployees": 25,
    "totalBasicSalary": 2000000.00,
    "totalAllowances": 625000.00,
    "totalDeductions": 250000.00,
    "totalAmendments": 15000.00,
    "totalNetSalary": 2390000.00,
    "processedBy": "Admin User",
    "createdDate": "2026-03-24T12:00:00",
    "processedDate": "2026-03-24T12:00:00"
  },
  "salarySlips": [
    {
      "id": 1,
      "organizationId": 1,
      "employeeId": 1,
      "employeeName": "Ali Hassan",
      "employeeCode": "EMP-001",
      "departmentName": "Engineering",
      "designation": "Software Engineer",
      "salaryMonth": 3,
      "salaryYear": 2026,
      "basicSalary": 80000.00,
      "totalAllowances": 25000.00,
      "totalDeductions": 10000.00,
      "totalAmendments": 5000.00,
      "grossSalary": 105000.00,
      "netSalary": 100000.00,
      "status": "GENERATED",
      "generatedDate": "2026-03-24T12:00:00",
      "paidDate": null
    }
  ]
}
```

> ⚠️ Returns error if payroll already processed for this month. Cancel existing payroll first.

---

### 6.2 Generate Single Salary Slip

```
POST /api/hr/payroll/generate-slip/{employeeId}?month=3&year=2026
```

**Response `data`:**
```json
{
  "salarySlip": { ... },
  "allowanceBreakdown": [
    { "allowanceName": "House Rent", "amount": 20000.00 },
    { "allowanceName": "Medical", "amount": 5000.00 }
  ],
  "deductionBreakdown": [
    { "deductionName": "Provident Fund", "amount": 4000.00 },
    { "deductionName": "Tax", "amount": 6000.00 }
  ],
  "amendments": [
    { "amendmentType": "ADDITION", "amount": 5000.00, "description": "Overtime bonus" }
  ]
}
```

> 💡 **Use this for the salary slip detail/print page** — includes full breakdown.

---

### 6.3 Get Salary Slip by ID (with Breakdown)

```
GET /api/hr/payroll/salary-slip/{id}
```

> Returns same detailed breakdown as 6.2.

---

### 6.4 Get Salary Slips by Employee (Paginated)

```
GET /api/hr/payroll/salary-slips/employee/{employeeId}?page=0&size=12&sortBy=salaryYear&sortDir=desc
```

> 💡 **Employee salary history** — shows all past salary slips.

---

### 6.5 Get Salary Slips by Organization & Month

```
GET /api/hr/payroll/salary-slips/organization/{organizationId}?month=3&year=2026
```

> 💡 Returns **all employee slips** for that month — use for the payroll summary table.

---

### 6.6 Mark Single Slip as Paid

```
PUT /api/hr/payroll/salary-slip/mark-paid/{id}
```

---

### 6.7 Mark All Slips as Paid (Bulk)

```
PUT /api/hr/payroll/salary-slips/mark-all-paid/{organizationId}?month=3&year=2026
```

**Response `data`:**
```json
{
  "paidCount": 25,
  "totalSlips": 25
}
```

---

### 6.8 Get Payroll History (Paginated)

```
GET /api/hr/payroll/history/{organizationId}?page=0&size=12&sortBy=createdDate&sortDir=desc
```

> Shows all payroll runs — month-by-month records.

---

### 6.9 Cancel Payroll

```
PUT /api/hr/payroll/cancel/{payrollId}
```

> ⚠️ Deletes all associated salary slips. **Fails** if any slip is already marked `PAID`.

---

## 📈 7. Dashboard & Analytics

---

### 7.1 HR Dashboard (Summary Cards)

```
GET /api/hr/payroll/dashboard/{organizationId}
```

**Response `data`:**
```json
{
  "totalEmployees": 50,
  "activeEmployees": 45,
  "totalDepartments": 6,
  "totalMonthlyPayroll": 2390000.00,
  "pendingLeaveRequests": 3,
  "presentToday": 40,
  "absentToday": 5,
  "payslipsGeneratedThisMonth": 45
}
```

**Suggested Dashboard Cards:**

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  👥 Total Emp.   │  │  ✅ Active Emp.  │  │  🏛️ Departments │  │  💰 Monthly      │
│       50         │  │       45         │  │        6         │  │   Payroll        │
│                  │  │                  │  │                  │  │  ₨ 2,390,000     │
└──────────────────┘  └──────────────────┘  └──────────────────┘  └──────────────────┘

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  📋 Pending      │  │  🟢 Present      │  │  🔴 Absent       │  │  📄 Payslips     │
│  Leave Requests  │  │  Today           │  │  Today           │  │  This Month      │
│       3          │  │       40         │  │        5         │  │       45         │
└──────────────────┘  └──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

### 7.2 Department-wise Salary Summary

```
GET /api/hr/payroll/department-salary-summary/{organizationId}
```

**Response `data`:**
```json
[
  {
    "departmentId": 1,
    "departmentName": "Engineering",
    "employeeCount": 12,
    "totalBasicSalary": 960000.00,
    "totalAllowances": 300000.00,
    "totalDeductions": 120000.00,
    "totalNetSalary": 1140000.00,
    "budgetAllocated": 1200000.00
  },
  {
    "departmentId": 2,
    "departmentName": "Marketing",
    "employeeCount": 8,
    "totalBasicSalary": 480000.00,
    "totalAllowances": 160000.00,
    "totalDeductions": 64000.00,
    "totalNetSalary": 576000.00,
    "budgetAllocated": 600000.00
  }
]
```

> 💡 **Perfect for bar charts** comparing department spending vs budget.

---

## 🖥️ Suggested Pages & UI Components

### Page Structure

```
📁 HR & Payroll Module
├── 📊 Dashboard                     ← /hr/dashboard
│   ├── Summary Cards (7.1)
│   ├── Department Salary Chart (7.2)
│   └── Pending Leave Approvals (4.8)
│
├── 🏛️ Departments                   ← /hr/departments
│   ├── Department List Table (1.4)
│   ├── Create/Edit Department Modal (1.1, 1.2)
│   └── Department Detail Page (1.3)
│
├── 👤 Employees                      ← /hr/employees
│   ├── Employee List Table (2.4)
│   │   ├── Filter by Department Dropdown (1.5 → 2.5)
│   │   └── Search Bar (2.6)
│   ├── Create Employee Form (2.1)
│   │   ├── Personal Info Section
│   │   ├── Job Info Section
│   │   ├── Bank Info Section
│   │   ├── Allowances (Dynamic Add/Remove)
│   │   └── Deductions (Dynamic Add/Remove)
│   └── Employee Detail Page (2.3)
│       ├── Profile Card
│       ├── Allowances & Deductions Tab
│       ├── Attendance History Tab (3.4)
│       ├── Leave History Tab (4.6)
│       └── Salary History Tab (6.4)
│
├── 📅 Attendance                     ← /hr/attendance
│   ├── Daily Attendance Grid (3.6)
│   │   └── Bulk Mark Form (3.2)
│   ├── Employee Attendance Detail (3.4, 3.5)
│   └── Attendance Summary Report (3.7)
│
├── 🌴 Leave Management               ← /hr/leaves
│   ├── All Leaves Table (4.7)
│   ├── Pending Approvals Tab (4.8)
│   │   └── Approve / Reject Buttons (4.2, 4.3)
│   └── Apply Leave Form (4.1)
│
├── 💰 Salary Amendments              ← /hr/amendments
│   ├── Amendments Table (5.4)
│   └── Create Amendment Modal (5.1)
│
└── 📊 Payroll                        ← /hr/payroll
    ├── Process Payroll Page (6.1)
    │   ├── Select Month/Year
    │   └── Process Button → Review Summary
    ├── Salary Slips Table (6.5)
    │   ├── Mark Individual Paid (6.6)
    │   └── Mark All Paid Button (6.7)
    ├── Salary Slip Detail (6.3)
    │   └── Print-ready Slip View
    └── Payroll History (6.8)
```

---

## 🔄 Workflow Diagrams

### Employee Lifecycle

```
  ┌─────────┐    Create     ┌────────┐    Terminate    ┌────────────┐
  │  Form   │──────────────►│ ACTIVE │───────────────►│ TERMINATED │
  └─────────┘               └───┬────┘                └────────────┘
                                │
                          Update Status
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ INACTIVE │ │ ON_LEAVE │ │  ACTIVE  │
              └──────────┘ └──────────┘ └──────────┘
```

### Leave Approval Workflow

```
  Employee                          Admin
  ────────                          ─────
     │                                │
     │  POST /leave/apply             │
     │───────────────────────────────►│
     │                                │
     │              ┌─────────────────┤
     │              │  PENDING        │
     │              │                 │
     │              │   ┌─────────┐  │
     │              ├──►│ APPROVE │──├──► PUT /leave/approve/{id}
     │              │   └─────────┘  │         → status: APPROVED
     │              │                │
     │              │   ┌─────────┐  │
     │              └──►│ REJECT  │──├──► PUT /leave/reject/{id}
     │                  └─────────┘  │         → status: REJECTED
     │                               │
     │  PUT /leave/cancel/{id}       │
     │───────────────────────────────►│
     │        → status: CANCELLED    │
```

### Monthly Payroll Workflow

```
  Step 1              Step 2              Step 3              Step 4
  ──────              ──────              ──────              ──────

  Setup Data          Process             Review              Disburse
  ──────────          ───────             ──────              ────────

  ┌────────────┐    ┌────────────────┐   ┌──────────────┐   ┌──────────────┐
  │ Add/Update │    │ POST           │   │ GET salary-  │   │ PUT mark-all │
  │ Amendments │───►│ /payroll/      │──►│ slips/org/   │──►│ -paid/{org}  │
  │ for Month  │    │ process        │   │ {orgId}      │   │ ?month&year  │
  └────────────┘    │                │   │              │   │              │
                    │ Calculates:    │   │ Review each  │   │ Marks all as │
                    │ Basic          │   │ employee's   │   │ PAID         │
                    │ + Allowances   │   │ net salary   │   └──────────────┘
                    │ - Deductions   │   └──────────────┘
                    │ ± Amendments   │
                    │ = Net Salary   │          ❌ Issues?
                    └────────────────┘              │
                                           ┌───────▼──────┐
                                           │ PUT /payroll/ │
                                           │ cancel/{id}   │
                                           │ (Re-process)  │
                                           └──────────────┘
```

---

## ⚡ Quick Copy-Paste — API Service Layer

```javascript
// services/hrApi.js
import API, { handleResponse } from './api/hrService';

// ─── DEPARTMENTS ────────────────────────────────────────
export const createDepartment     = (data)             => API.post('/api/hr/department/create', data).then(handleResponse);
export const updateDepartment     = (id, data)         => API.put(`/api/hr/department/update/${id}`, data).then(handleResponse);
export const getDepartment        = (id)               => API.get(`/api/hr/department/${id}`).then(handleResponse);
export const getDepartments       = (orgId, params)    => API.get(`/api/hr/department/organization/${orgId}`, { params }).then(handleResponse);
export const getActiveDepartments = (orgId)            => API.get(`/api/hr/department/active/${orgId}`).then(handleResponse);
export const searchDepartments    = (orgId, params)    => API.get(`/api/hr/department/search/${orgId}`, { params }).then(handleResponse);
export const deleteDepartment     = (id)               => API.delete(`/api/hr/department/delete/${id}`).then(handleResponse);

// ─── EMPLOYEES ──────────────────────────────────────────
export const createEmployee       = (data)             => API.post('/api/hr/employee/create', data).then(handleResponse);
export const updateEmployee       = (id, data)         => API.put(`/api/hr/employee/update/${id}`, data).then(handleResponse);
export const getEmployee          = (id)               => API.get(`/api/hr/employee/${id}`).then(handleResponse);
export const getEmployees         = (orgId, params)    => API.get(`/api/hr/employee/organization/${orgId}`, { params }).then(handleResponse);
export const getEmployeesByDept   = (orgId, deptId, p) => API.get(`/api/hr/employee/department/${orgId}/${deptId}`, { params: p }).then(handleResponse);
export const searchEmployees      = (orgId, params)    => API.get(`/api/hr/employee/search/${orgId}`, { params }).then(handleResponse);
export const terminateEmployee    = (id)               => API.put(`/api/hr/employee/terminate/${id}`).then(handleResponse);
export const deleteEmployee       = (id)               => API.delete(`/api/hr/employee/delete/${id}`).then(handleResponse);

// ─── ATTENDANCE ─────────────────────────────────────────
export const markAttendance       = (data)             => API.post('/api/hr/attendance/mark', data).then(handleResponse);
export const markBulkAttendance   = (data)             => API.post('/api/hr/attendance/mark-bulk', data).then(handleResponse);
export const updateAttendance     = (id, data)         => API.put(`/api/hr/attendance/update/${id}`, data).then(handleResponse);
export const getEmpAttendance     = (empId, params)    => API.get(`/api/hr/attendance/employee/${empId}`, { params }).then(handleResponse);
export const getAttendanceRange   = (empId, params)    => API.get(`/api/hr/attendance/employee/${empId}/range`, { params }).then(handleResponse);
export const getDailyAttendance   = (orgId, params)    => API.get(`/api/hr/attendance/daily/${orgId}`, { params }).then(handleResponse);
export const getAttendanceSummary = (empId, params)    => API.get(`/api/hr/attendance/summary/${empId}`, { params }).then(handleResponse);
export const deleteAttendance     = (id)               => API.delete(`/api/hr/attendance/delete/${id}`).then(handleResponse);

// ─── LEAVES ─────────────────────────────────────────────
export const applyLeave           = (data)             => API.post('/api/hr/leave/apply', data).then(handleResponse);
export const approveLeave         = (id, approvedBy)   => API.put(`/api/hr/leave/approve/${id}`, null, { params: { approvedBy } }).then(handleResponse);
export const rejectLeave          = (id, rejectedBy)   => API.put(`/api/hr/leave/reject/${id}`, null, { params: { rejectedBy } }).then(handleResponse);
export const cancelLeave          = (id)               => API.put(`/api/hr/leave/cancel/${id}`).then(handleResponse);
export const getLeave             = (id)               => API.get(`/api/hr/leave/${id}`).then(handleResponse);
export const getEmpLeaves         = (empId, params)    => API.get(`/api/hr/leave/employee/${empId}`, { params }).then(handleResponse);
export const getOrgLeaves         = (orgId, params)    => API.get(`/api/hr/leave/organization/${orgId}`, { params }).then(handleResponse);
export const getPendingLeaves     = (orgId, params)    => API.get(`/api/hr/leave/pending/${orgId}`, { params }).then(handleResponse);
export const deleteLeave          = (id)               => API.delete(`/api/hr/leave/delete/${id}`).then(handleResponse);

// ─── SALARY AMENDMENTS ──────────────────────────────────
export const createAmendment      = (data)             => API.post('/api/hr/salary-amendment/create', data).then(handleResponse);
export const updateAmendment      = (id, data)         => API.put(`/api/hr/salary-amendment/update/${id}`, data).then(handleResponse);
export const getEmpAmendments     = (empId)            => API.get(`/api/hr/salary-amendment/employee/${empId}`).then(handleResponse);
export const getOrgAmendments     = (orgId, params)    => API.get(`/api/hr/salary-amendment/organization/${orgId}`, { params }).then(handleResponse);
export const deleteAmendment      = (id)               => API.delete(`/api/hr/salary-amendment/delete/${id}`).then(handleResponse);

// ─── PAYROLL & SALARY SLIPS ─────────────────────────────
export const processPayroll       = (data)             => API.post('/api/hr/payroll/process', data).then(handleResponse);
export const generateSlip         = (empId, params)    => API.post(`/api/hr/payroll/generate-slip/${empId}`, null, { params }).then(handleResponse);
export const getSalarySlip        = (id)               => API.get(`/api/hr/payroll/salary-slip/${id}`).then(handleResponse);
export const getEmpSlips          = (empId, params)    => API.get(`/api/hr/payroll/salary-slips/employee/${empId}`, { params }).then(handleResponse);
export const getOrgSlips          = (orgId, params)    => API.get(`/api/hr/payroll/salary-slips/organization/${orgId}`, { params }).then(handleResponse);
export const markSlipPaid         = (id)               => API.put(`/api/hr/payroll/salary-slip/mark-paid/${id}`).then(handleResponse);
export const markAllSlipsPaid     = (orgId, params)    => API.put(`/api/hr/payroll/salary-slips/mark-all-paid/${orgId}`, null, { params }).then(handleResponse);
export const getPayrollHistory    = (orgId, params)    => API.get(`/api/hr/payroll/history/${orgId}`, { params }).then(handleResponse);
export const cancelPayroll        = (id)               => API.put(`/api/hr/payroll/cancel/${id}`).then(handleResponse);

// ─── DASHBOARD ──────────────────────────────────────────
export const getHRDashboard       = (orgId)            => API.get(`/api/hr/payroll/dashboard/${orgId}`).then(handleResponse);
export const getDeptSalarySummary = (orgId)            => API.get(`/api/hr/payroll/department-salary-summary/${orgId}`).then(handleResponse);
```

---

## 📝 Important Notes

1. **All dates** must be sent as `YYYY-MM-DD` strings (e.g., `"2026-03-24"`).
2. **All times** must be sent as `HH:mm:ss` strings (e.g., `"09:00:00"`).
3. **BigDecimal fields** (salaries, amounts) — send as numbers (e.g., `80000.00`).
4. **Partial updates** — only include fields you want to change. `null` fields are ignored.
5. **Allowances/Deductions on update** — if you send these arrays, they **replace all existing** ones. Omit them to keep current values.
6. **Payroll processing** is idempotent per month — cannot process same month twice without cancelling first.
7. **Department deletion** is protected — must have zero employees to delete.
8. **Attendance** is unique per employee + date — duplicate marking returns an error.

---

> 📌 **Questions?** Reach out to the backend team.  
> 📌 **Postman Collection** coming soon.
