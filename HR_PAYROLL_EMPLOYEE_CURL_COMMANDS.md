# HR & Payroll Employee API - CURL Commands Reference

## Quick Setup

**Base URL:** `http://localhost:8080`

Save these commands in a `.ps1` file to run them in PowerShell.

---

## 1. GET EMPLOYEE BY ID

### Command:
```powershell
$employeeId = 1
$url = "http://localhost:8080/api/hr/employee/$employeeId"

$response = Invoke-WebRequest -Uri $url `
    -Method Get `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Alternative CURL:
```powershell
curl -X GET "http://localhost:8080/api/hr/employee/1" `
  -H "Content-Type: application/json"
```

### Expected Response:
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

---

## 2. CREATE EMPLOYEE

### Command (PowerShell):
```powershell
$url = "http://localhost:8080/api/hr/employee/create"

$body = @{
    organizationId = 1
    employeeCode = "EMP-002"
    fullName = "Jane Smith"
    email = "jane.smith@company.com"
    phone = "+92-300-9876543"
    cnic = "54321-9876543-2"
    address = "456 Oak Avenue"
    city = "Lahore"
    gender = "Female"
    dateOfBirth = "1992-08-20"
    departmentId = 2
    designation = "HR Manager"
    employmentType = "Permanent"
    status = "Active"
    joiningDate = "2023-06-01"
    basicSalary = 120000.00
    bankName = "MCB"
    bankAccountNumber = "9876543210"
    bankBranchCode = "LHR-002"
    allowances = @(
        @{
            allowanceType = "House Rent Allowance"
            amount = 24000.00
        },
        @{
            allowanceType = "Medical Allowance"
            amount = 8000.00
        }
    )
    deductions = @(
        @{
            deductionType = "Income Tax"
            amount = 12000.00
        }
    )
} | ConvertTo-Json -Depth 10

$response = Invoke-WebRequest -Uri $url `
    -Method Post `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Alternative CURL:
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

### Expected Response:
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

## 3. UPDATE EMPLOYEE

### Command (PowerShell):
```powershell
$employeeId = 1
$url = "http://localhost:8080/api/hr/employee/update/$employeeId"

$body = @{
    organizationId = 1
    employeeCode = "EMP-001"
    fullName = "John Michael Doe"
    email = "john.m.doe@company.com"
    phone = "+92-300-1111111"
    cnic = "12345-6789012-3"
    address = "789 New Street"
    city = "Karachi"
    gender = "Male"
    dateOfBirth = "1990-05-15"
    departmentId = 1
    designation = "Lead Developer"
    employmentType = "Permanent"
    status = "Active"
    joiningDate = "2022-01-15"
    basicSalary = 180000.00
    bankName = "HBL"
    bankAccountNumber = "1234567890"
    bankBranchCode = "KHI-001"
    allowances = @(
        @{
            allowanceType = "House Rent Allowance"
            amount = 36000.00
        },
        @{
            allowanceType = "Conveyance Allowance"
            amount = 12000.00
        }
    )
    deductions = @(
        @{
            deductionType = "Income Tax"
            amount = 18000.00
        },
        @{
            deductionType = "EOBI"
            amount = 6000.00
        }
    )
} | ConvertTo-Json -Depth 10

$response = Invoke-WebRequest -Uri $url `
    -Method Put `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Alternative CURL:
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

### Expected Response:
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

---

## 4. GET EMPLOYEES BY ORGANIZATION (Paginated)

### Command:
```powershell
$organizationId = 1
$page = 0
$size = 10
$sortBy = "createdDate"
$sortDir = "desc"

$url = "http://localhost:8080/api/hr/employee/organization/$organizationId?page=$page&size=$size&sortBy=$sortBy&sortDir=$sortDir"

$response = Invoke-WebRequest -Uri $url `
    -Method Get `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### CURL:
```bash
curl -X GET "http://localhost:8080/api/hr/employee/organization/1?page=0&size=10&sortBy=createdDate&sortDir=desc" \
  -H "Content-Type: application/json"
```

---

## 5. GET EMPLOYEES BY DEPARTMENT

### Command:
```powershell
$organizationId = 1
$departmentId = 1
$page = 0
$size = 10

$url = "http://localhost:8080/api/hr/employee/department/$organizationId/$departmentId?page=$page&size=$size"

$response = Invoke-WebRequest -Uri $url `
    -Method Get `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### CURL:
```bash
curl -X GET "http://localhost:8080/api/hr/employee/department/1/1?page=0&size=10" \
  -H "Content-Type: application/json"
```

---

## 6. SEARCH EMPLOYEES

### Command:
```powershell
$organizationId = 1
$keyword = "John"
$page = 0
$size = 10

$url = "http://localhost:8080/api/hr/employee/search/$organizationId?keyword=$keyword&page=$page&size=$size"

$response = Invoke-WebRequest -Uri $url `
    -Method Get `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### CURL:
```bash
curl -X GET "http://localhost:8080/api/hr/employee/search/1?keyword=John&page=0&size=10" \
  -H "Content-Type: application/json"
```

---

## 7. TERMINATE EMPLOYEE

### Command:
```powershell
$employeeId = 1
$url = "http://localhost:8080/api/hr/employee/terminate/$employeeId"

$response = Invoke-WebRequest -Uri $url `
    -Method Put `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### CURL:
```bash
curl -X PUT "http://localhost:8080/api/hr/employee/terminate/1" \
  -H "Content-Type: application/json"
```

### Expected Response:
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
    "status": "Inactive",
    "lastModifiedDate": "2024-01-26T15:30:00"
  }
}
```

---

## 8. DELETE EMPLOYEE

### Command:
```powershell
$employeeId = 1
$url = "http://localhost:8080/api/hr/employee/delete/$employeeId"

$response = Invoke-WebRequest -Uri $url `
    -Method Delete `
    -Headers @{"Content-Type" = "application/json"} `
    -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### CURL:
```bash
curl -X DELETE "http://localhost:8080/api/hr/employee/delete/1" \
  -H "Content-Type: application/json"
```

### Expected Response:
```json
{
  "success": true,
  "message": "Employee deleted successfully",
  "statusCode": 200,
  "data": null
}
```

---

## Error Response Examples

### Validation Error:
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

### Not Found Error:
```json
{
  "success": false,
  "message": "Employee with ID 999 not found",
  "statusCode": 404,
  "data": null
}
```

### Server Error:
```json
{
  "success": false,
  "message": "An error occurred while processing your request",
  "statusCode": 500,
  "data": null
}
```

---

## Usage Tips

1. **Replace values** in examples with your actual data
2. **Test with Postman** for GUI-based testing
3. **Use PowerShell ISE** or Windows Terminal for script execution
4. **Check server logs** for detailed error messages
5. **Ensure base URL** is correct for your environment
6. **Validate JSON** before sending requests
7. **Handle errors** gracefully in your frontend code

---

## Field Validation Rules

| Field | Format | Example | Required |
|-------|--------|---------|----------|
| Email | valid@email.com | john@company.com | Yes |
| Phone | +92-XXX-XXXXXXX | +92-300-1234567 | Yes |
| CNIC | XXXXX-XXXXXXX-X | 12345-6789012-3 | Yes |
| Date of Birth | YYYY-MM-DD | 1990-05-15 | Yes |
| Basic Salary | Positive decimal | 150000.00 | Yes |
| Employment Type | Permanent/Contract/Temporary | Permanent | Yes |
| Status | Active/Inactive/On Leave | Active | Yes |

---

**Version:** 1.0  
**Last Updated:** 2024-01-26
