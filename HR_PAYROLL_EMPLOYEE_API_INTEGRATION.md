# HR & Payroll Module - Employee API Integration Guide

## Overview
This document provides comprehensive API integration details for the Employee Management module within the HR & Payroll system. It includes curl requests, response formats, and implementation guidelines for the frontend team to create, read, and update employee records.

---

## Table of Contents
1. [Base URL & Authentication](#base-url--authentication)
2. [Employee Data Model](#employee-data-model)
3. [API Endpoints](#api-endpoints)
4. [Error Handling](#error-handling)
5. [Integration Examples](#integration-examples)

---

## Base URL & Authentication

**Base URL:** `http://localhost:8080` (Development)

**Headers Required:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN> (if required)
```

---

## Employee Data Model

### Employee Request Schema

```json
{
  "organizationId": 1,
  "employeeCode": "EMP-001",
  "fullName": "John Doe",
  "email": "john.doe@company.com",
  "phone": "+92-300-1234567",
  "cnic": "12345-6789012-3",
  "address": "123 Main Street",
  "city": "Karachi",
  "gender": "Male",
  "dateOfBirth": "1990-05-15",
  "departmentId": 1,
  "designation": "Senior Developer",
  "employmentType": "Permanent",
  "status": "Active",
  "joiningDate": "2022-01-15",
  "basicSalary": 150000.00,
  "bankName": "HBL",
  "bankAccountNumber": "1234567890",
  "bankBranchCode": "KHI-001",
  "allowances": [
    {
      "allowanceType": "House Rent Allowance",
      "amount": 30000.00
    },
    {
      "allowanceType": "Conveyance Allowance",
      "amount": 10000.00
    }
  ],
  "deductions": [
    {
      "deductionType": "Income Tax",
      "amount": 15000.00
    },
    {
      "deductionType": "EOBI",
      "amount": 5000.00
    }
  ]
}
```

### Employee Response Schema

```json
{
  "success": true,
  "message": "Operation successful",
  "statusCode": 200,
  "data": {
    "id": 1,
    "organizationId": 1,
    "employeeCode": "EMP-001",
    "fullName": "John Doe",
    "email": "john.doe@company.com",
    "phone": "+92-300-1234567",
    "cnic": "12345-6789012-3",
    "address": "123 Main Street",
    "city": "Karachi",
    "gender": "Male",
    "dateOfBirth": "1990-05-15",
    "departmentId": 1,
    "departmentName": "Engineering",
    "designation": "Senior Developer",
    "employmentType": "Permanent",
    "status": "Active",
    "joiningDate": "2022-01-15",
    "basicSalary": 150000.00,
    "bankName": "HBL",
    "bankAccountNumber": "1234567890",
    "bankBranchCode": "KHI-001",
    "allowances": [
      {
        "id": 1,
        "employeeId": 1,
        "allowanceType": "House Rent Allowance",
        "amount": 30000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "allowanceType": "Conveyance Allowance",
        "amount": 10000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "deductions": [
      {
        "id": 1,
        "employeeId": 1,
        "deductionType": "Income Tax",
        "amount": 15000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "deductionType": "EOBI",
        "amount": 5000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-20T14:45:00"
  }
}
```

---

## API Endpoints

### 1. Get Employee by ID

**Endpoint:** `GET /api/hr/employee/{id}`

**Description:** Retrieve a single employee record by their ID.

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Employee ID |

**Response:** Single Employee Object

---

### 2. Create Employee

**Endpoint:** `POST /api/hr/employee/create`

**Description:** Create a new employee record with personal, job, and financial information.

**Request Body:** Employee Request Object

**Response:** Created Employee Object with ID

---

### 3. Update Employee

**Endpoint:** `PUT /api/hr/employee/update/{id}`

**Description:** Update an existing employee record.

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Employee ID to update |

**Request Body:** Employee Request Object (All fields or partial fields)

**Response:** Updated Employee Object

---

### 4. Get Employees by Organization (Paginated)

**Endpoint:** `GET /api/hr/employee/organization/{organizationId}`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 10 | Number of records per page |
| sortBy | string | createdDate | Sort field |
| sortDir | string | desc | Sort direction (asc/desc) |

**Response:** Paginated Employee List

---

### 5. Get Employees by Department (Paginated)

**Endpoint:** `GET /api/hr/employee/department/{organizationId}/{departmentId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| organizationId | Long | Yes | Organization ID |
| departmentId | Long | Yes | Department ID |

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 10 | Number of records per page |
| sortBy | string | createdDate | Sort field |
| sortDir | string | desc | Sort direction (asc/desc) |

**Response:** Paginated Employee List

---

### 6. Search Employees

**Endpoint:** `GET /api/hr/employee/search/{organizationId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| organizationId | Long | Yes | Organization ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| keyword | string | Yes | Search keyword (name, email, code, phone) |
| page | int | No | Page number (default: 0) |
| size | int | No | Records per page (default: 10) |
| sortBy | string | No | Sort field (default: createdDate) |
| sortDir | string | No | Sort direction (default: desc) |

**Response:** Paginated Filtered Employee List

---

### 7. Terminate Employee

**Endpoint:** `PUT /api/hr/employee/terminate/{id}`

**Description:** Terminate an active employee (changes status to Inactive).

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Employee ID |

**Response:** Updated Employee Object with status changed to Inactive

---

### 8. Delete Employee

**Endpoint:** `DELETE /api/hr/employee/delete/{id}`

**Description:** Permanently delete an employee record.

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Employee ID |

**Response:** Success/Failure message

---

## Error Handling

### Common Error Responses

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Invalid request data",
  "statusCode": 400,
  "errors": [
    "Email is invalid",
    "Phone number format is incorrect"
  ]
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Employee not found",
  "statusCode": 404,
  "data": null
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "message": "An error occurred while processing your request",
  "statusCode": 500,
  "data": null
}
```

---

## Integration Examples

### Example 1: Get Employee by ID

**Request:**
```bash
curl -X GET "http://localhost:8080/api/hr/employee/1" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employee retrieved successfully",
  "statusCode": 200,
  "data": {
    "id": 1,
    "organizationId": 1,
    "employeeCode": "EMP-001",
    "fullName": "John Doe",
    "email": "john.doe@company.com",
    "phone": "+92-300-1234567",
    "cnic": "12345-6789012-3",
    "address": "123 Main Street",
    "city": "Karachi",
    "gender": "Male",
    "dateOfBirth": "1990-05-15",
    "departmentId": 1,
    "departmentName": "Engineering",
    "designation": "Senior Developer",
    "employmentType": "Permanent",
    "status": "Active",
    "joiningDate": "2022-01-15",
    "basicSalary": 150000.00,
    "bankName": "HBL",
    "bankAccountNumber": "****7890",
    "bankBranchCode": "KHI-001",
    "allowances": [
      {
        "id": 1,
        "employeeId": 1,
        "allowanceType": "House Rent Allowance",
        "amount": 30000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "allowanceType": "Conveyance Allowance",
        "amount": 10000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "deductions": [
      {
        "id": 1,
        "employeeId": 1,
        "deductionType": "Income Tax",
        "amount": 15000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "deductionType": "EOBI",
        "amount": 5000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-20T14:45:00"
  }
}
```

**Response (Error - Not Found):**
```json
{
  "success": false,
  "message": "Employee with ID 999 not found",
  "statusCode": 404,
  "data": null
}
```

---

### Example 2: Create Employee

**Request:**
```bash
curl -X POST "http://localhost:8080/api/hr/employee/create" \
  -H "Content-Type: application/json" \
  -d '{
  "organizationId": 1,
  "employeeCode": "EMP-002",
  "fullName": "Jane Smith",
  "email": "jane.smith@company.com",
  "phone": "+92-300-9876543",
  "cnic": "54321-9876543-2",
  "address": "456 Oak Avenue",
  "city": "Lahore",
  "gender": "Female",
  "dateOfBirth": "1992-08-20",
  "departmentId": 2,
  "designation": "HR Manager",
  "employmentType": "Permanent",
  "status": "Active",
  "joiningDate": "2023-06-01",
  "basicSalary": 120000.00,
  "bankName": "MCB",
  "bankAccountNumber": "9876543210",
  "bankBranchCode": "LHR-002",
  "allowances": [
    {
      "allowanceType": "House Rent Allowance",
      "amount": 24000.00
    },
    {
      "allowanceType": "Medical Allowance",
      "amount": 8000.00
    }
  ],
  "deductions": [
    {
      "deductionType": "Income Tax",
      "amount": 12000.00
    }
  ]
}'
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employee created successfully",
  "statusCode": 201,
  "data": {
    "id": 2,
    "organizationId": 1,
    "employeeCode": "EMP-002",
    "fullName": "Jane Smith",
    "email": "jane.smith@company.com",
    "phone": "+92-300-9876543",
    "cnic": "54321-9876543-2",
    "address": "456 Oak Avenue",
    "city": "Lahore",
    "gender": "Female",
    "dateOfBirth": "1992-08-20",
    "departmentId": 2,
    "departmentName": "Human Resources",
    "designation": "HR Manager",
    "employmentType": "Permanent",
    "status": "Active",
    "joiningDate": "2023-06-01",
    "basicSalary": 120000.00,
    "bankName": "MCB",
    "bankAccountNumber": "****3210",
    "bankBranchCode": "LHR-002",
    "allowances": [
      {
        "id": 3,
        "employeeId": 2,
        "allowanceType": "House Rent Allowance",
        "amount": 24000.00,
        "createdDate": "2024-01-25T09:15:00"
      },
      {
        "id": 4,
        "employeeId": 2,
        "allowanceType": "Medical Allowance",
        "amount": 8000.00,
        "createdDate": "2024-01-25T09:15:00"
      }
    ],
    "deductions": [
      {
        "id": 3,
        "employeeId": 2,
        "deductionType": "Income Tax",
        "amount": 12000.00,
        "createdDate": "2024-01-25T09:15:00"
      }
    ],
    "createdDate": "2024-01-25T09:15:00",
    "lastModifiedDate": "2024-01-25T09:15:00"
  }
}
```

---

### Example 3: Update Employee

**Request:**
```bash
curl -X PUT "http://localhost:8080/api/hr/employee/update/1" \
  -H "Content-Type: application/json" \
  -d '{
  "organizationId": 1,
  "employeeCode": "EMP-001",
  "fullName": "John Michael Doe",
  "email": "john.m.doe@company.com",
  "phone": "+92-300-1111111",
  "cnic": "12345-6789012-3",
  "address": "789 New Street",
  "city": "Karachi",
  "gender": "Male",
  "dateOfBirth": "1990-05-15",
  "departmentId": 1,
  "designation": "Lead Developer",
  "employmentType": "Permanent",
  "status": "Active",
  "joiningDate": "2022-01-15",
  "basicSalary": 180000.00,
  "bankName": "HBL",
  "bankAccountNumber": "1234567890",
  "bankBranchCode": "KHI-001",
  "allowances": [
    {
      "allowanceType": "House Rent Allowance",
      "amount": 36000.00
    },
    {
      "allowanceType": "Conveyance Allowance",
      "amount": 12000.00
    }
  ],
  "deductions": [
    {
      "deductionType": "Income Tax",
      "amount": 18000.00
    },
    {
      "deductionType": "EOBI",
      "amount": 6000.00
    }
  ]
}'
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employee updated successfully",
  "statusCode": 200,
  "data": {
    "id": 1,
    "organizationId": 1,
    "employeeCode": "EMP-001",
    "fullName": "John Michael Doe",
    "email": "john.m.doe@company.com",
    "phone": "+92-300-1111111",
    "cnic": "12345-6789012-3",
    "address": "789 New Street",
    "city": "Karachi",
    "gender": "Male",
    "dateOfBirth": "1990-05-15",
    "departmentId": 1,
    "departmentName": "Engineering",
    "designation": "Lead Developer",
    "employmentType": "Permanent",
    "status": "Active",
    "joiningDate": "2022-01-15",
    "basicSalary": 180000.00,
    "bankName": "HBL",
    "bankAccountNumber": "****7890",
    "bankBranchCode": "KHI-001",
    "allowances": [
      {
        "id": 1,
        "employeeId": 1,
        "allowanceType": "House Rent Allowance",
        "amount": 36000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "allowanceType": "Conveyance Allowance",
        "amount": 12000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "deductions": [
      {
        "id": 1,
        "employeeId": 1,
        "deductionType": "Income Tax",
        "amount": 18000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "employeeId": 1,
        "deductionType": "EOBI",
        "amount": 6000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-25T11:45:00"
  }
}
```

**Response (Error - Validation Failed):**
```json
{
  "success": false,
  "message": "Validation failed",
  "statusCode": 400,
  "errors": [
    "Email format is invalid",
    "Phone number must start with country code"
  ]
}
```

---

### Example 4: Get Employees by Organization (Paginated)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/hr/employee/organization/1?page=0&size=10&sortBy=fullName&sortDir=asc" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employees retrieved successfully",
  "statusCode": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "organizationId": 1,
        "employeeCode": "EMP-001",
        "fullName": "John Michael Doe",
        "email": "john.m.doe@company.com",
        "phone": "+92-300-1111111",
        "departmentId": 1,
        "departmentName": "Engineering",
        "designation": "Lead Developer",
        "status": "Active",
        "basicSalary": 180000.00,
        "createdDate": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "organizationId": 1,
        "employeeCode": "EMP-002",
        "fullName": "Jane Smith",
        "email": "jane.smith@company.com",
        "phone": "+92-300-9876543",
        "departmentId": 2,
        "departmentName": "Human Resources",
        "designation": "HR Manager",
        "status": "Active",
        "basicSalary": 120000.00,
        "createdDate": "2024-01-25T09:15:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1,
      "isFirst": true,
      "isLast": true
    }
  }
}
```

---

### Example 5: Search Employees

**Request:**
```bash
curl -X GET "http://localhost:8080/api/hr/employee/search/1?keyword=John&page=0&size=10" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employees found",
  "statusCode": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "organizationId": 1,
        "employeeCode": "EMP-001",
        "fullName": "John Michael Doe",
        "email": "john.m.doe@company.com",
        "phone": "+92-300-1111111",
        "departmentId": 1,
        "departmentName": "Engineering",
        "designation": "Lead Developer",
        "status": "Active",
        "basicSalary": 180000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1,
      "isFirst": true,
      "isLast": true
    }
  }
}
```

---

### Example 6: Get Employees by Department

**Request:**
```bash
curl -X GET "http://localhost:8080/api/hr/employee/department/1/1?page=0&size=10" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Department employees retrieved successfully",
  "statusCode": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "organizationId": 1,
        "employeeCode": "EMP-001",
        "fullName": "John Michael Doe",
        "email": "john.m.doe@company.com",
        "phone": "+92-300-1111111",
        "departmentId": 1,
        "departmentName": "Engineering",
        "designation": "Lead Developer",
        "status": "Active",
        "basicSalary": 180000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1,
      "isFirst": true,
      "isLast": true
    }
  }
}
```

---

### Example 7: Terminate Employee

**Request:**
```bash
curl -X PUT "http://localhost:8080/api/hr/employee/terminate/1" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employee terminated successfully",
  "statusCode": 200,
  "data": {
    "id": 1,
    "organizationId": 1,
    "employeeCode": "EMP-001",
    "fullName": "John Michael Doe",
    "email": "john.m.doe@company.com",
    "phone": "+92-300-1111111",
    "departmentId": 1,
    "departmentName": "Engineering",
    "designation": "Lead Developer",
    "status": "Inactive",
    "basicSalary": 180000.00,
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-26T15:30:00"
  }
}
```

---

### Example 8: Delete Employee

**Request:**
```bash
curl -X DELETE "http://localhost:8080/api/hr/employee/delete/1" \
  -H "Content-Type: application/json"
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Employee deleted successfully",
  "statusCode": 200,
  "data": null
}
```

**Response (Error - Employee Not Found):**
```json
{
  "success": false,
  "message": "Employee with ID 1 not found",
  "statusCode": 404,
  "data": null
}
```

---

## Frontend Implementation Guidelines

### Create/Update Employee Page

1. **Form Fields Required:**
   - Personal Information: Full Name, Email, Phone, CNIC, Address, City, Gender, Date of Birth
   - Job Information: Department, Designation, Employment Type, Joining Date
   - Financial Information: Basic Salary, Bank Name, Account Number, Branch Code
   - Allowances & Deductions: Dynamic lists with add/remove functionality

2. **Validation Requirements:**
   - Email must be valid format
   - Phone number must include country code
   - CNIC format: 12345-6789012-3
   - Dates must be in YYYY-MM-DD format
   - Basic Salary must be positive number
   - Bank Account Number must be numeric

3. **API Call Examples (JavaScript/TypeScript):**

   **Get Employee:**
   ```javascript
   const getEmployee = async (employeeId) => {
     try {
       const response = await fetch(`/api/hr/employee/${employeeId}`, {
         method: 'GET',
         headers: {
           'Content-Type': 'application/json'
         }
       });
       const result = await response.json();
       if (result.success) {
         return result.data;
       } else {
         console.error(result.message);
       }
     } catch (error) {
       console.error('Error fetching employee:', error);
     }
   };
   ```

   **Create Employee:**
   ```javascript
   const createEmployee = async (employeeData) => {
     try {
       const response = await fetch('/api/hr/employee/create', {
         method: 'POST',
         headers: {
           'Content-Type': 'application/json'
         },
         body: JSON.stringify(employeeData)
       });
       const result = await response.json();
       if (result.success) {
         return result.data;
       } else {
         throw new Error(result.message);
       }
     } catch (error) {
       console.error('Error creating employee:', error);
       throw error;
     }
   };
   ```

   **Update Employee:**
   ```javascript
   const updateEmployee = async (employeeId, employeeData) => {
     try {
       const response = await fetch(`/api/hr/employee/update/${employeeId}`, {
         method: 'PUT',
         headers: {
           'Content-Type': 'application/json'
         },
         body: JSON.stringify(employeeData)
       });
       const result = await response.json();
       if (result.success) {
         return result.data;
       } else {
         throw new Error(result.message);
       }
     } catch (error) {
       console.error('Error updating employee:', error);
       throw error;
     }
   };
   ```

4. **UI/UX Considerations:**
   - Show loading state during API calls
   - Display error messages in user-friendly format
   - Auto-populate form when editing
   - Disable certain fields for terminated employees
   - Show confirmation dialog before deleting
   - Implement form validation before submission

---

## Common HTTP Status Codes

| Status Code | Meaning | Action |
|------------|---------|--------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid input data |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Access denied |
| 404 | Not Found | Resource not found |
| 500 | Server Error | Internal server error |

---

## Notes for Frontend Team

1. **Always validate form data** before sending API requests
2. **Handle network errors** gracefully with retry mechanisms
3. **Implement proper error boundaries** to catch and display errors
4. **Use loading states** to prevent multiple submissions
5. **Sanitize sensitive data** (e.g., bank account numbers) for display
6. **Implement proper authentication** before accessing HR APIs
7. **Cache employee data** where appropriate to reduce API calls
8. **Implement pagination** for lists to handle large datasets
9. **Use date picker components** for date fields
10. **Implement role-based access control** for sensitive operations

---

## Support & Troubleshooting

For issues or questions regarding the API integration:
1. Check the error response message for specific details
2. Verify all required fields are included in the request
3. Ensure organizationId and departmentId exist in the system
4. Contact the backend team with the request/response for debugging

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-26  
**API Base URL:** http://localhost:8080  
**Module:** HR & Payroll - Employee Management
