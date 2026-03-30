# HR & Payroll Module - Employee Management Frontend Integration
## Complete Guide for Create & Update Employee Page

**Document Version:** 1.0  
**Last Updated:** 2024-01-26  
**Module:** HR & Payroll - Employee Management  
**Audience:** Frontend Development Team

---

## Quick Start

This document provides everything needed to integrate Employee Management APIs (Get, Create, Update) into your frontend application.

### Base Configuration
```
API Base URL: http://localhost:8080
Header: Content-Type: application/json
```

---

## Employee Data Model

### Request/Response Structure

```javascript
{
  "organizationId": 1,           // Required
  "employeeCode": "EMP-001",     // Required, auto-generated
  "fullName": "John Doe",        // Required
  "email": "john@company.com",   // Required, validated email
  "phone": "+92-300-1234567",    // Required, +92-XXX-XXXXXXX format
  "cnic": "12345-6789012-3",     // Required, XXXXX-XXXXXXX-X format
  "address": "123 Main St",      // Optional
  "city": "Karachi",             // Required
  "gender": "Male",              // Required
  "dateOfBirth": "1990-05-15",   // Required, YYYY-MM-DD
  "departmentId": 1,             // Required
  "designation": "Developer",    // Required
  "employmentType": "Permanent", // Required
  "status": "Active",            // Required, Active/Inactive/On Leave
  "joiningDate": "2022-01-15",   // Required, YYYY-MM-DD
  "basicSalary": 150000.00,      // Required, positive number
  "bankName": "HBL",             // Required
  "bankAccountNumber": "1234567890", // Required
  "bankBranchCode": "KHI-001",   // Required
  "allowances": [
    {
      "allowanceType": "HRA",
      "amount": 30000.00
    }
  ],
  "deductions": [
    {
      "deductionType": "Income Tax",
      "amount": 15000.00
    }
  ]
}
```

---

## Core API Endpoints

### 1. GET Employee by ID
```bash
GET /api/hr/employee/{id}
```

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/hr/employee/1" \
  -H "Content-Type: application/json"
```

**Response:**
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
      }
    ],
    "deductions": [
      {
        "id": 1,
        "employeeId": 1,
        "deductionType": "Income Tax",
        "amount": 15000.00,
        "createdDate": "2024-01-15T10:30:00"
      }
    ],
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-20T14:45:00"
  }
}
```

---

### 2. CREATE Employee
```bash
POST /api/hr/employee/create
```

**cURL Example:**
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
    {"allowanceType": "House Rent Allowance", "amount": 24000.00},
    {"allowanceType": "Medical Allowance", "amount": 8000.00}
  ],
  "deductions": [
    {"deductionType": "Income Tax", "amount": 12000.00}
  ]
}'
```

**Response (201 Created):**
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

### 3. UPDATE Employee
```bash
PUT /api/hr/employee/update/{id}
```

**cURL Example:**
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
    {"allowanceType": "House Rent Allowance", "amount": 36000.00},
    {"allowanceType": "Conveyance Allowance", "amount": 12000.00}
  ],
  "deductions": [
    {"deductionType": "Income Tax", "amount": 18000.00},
    {"deductionType": "EOBI", "amount": 6000.00}
  ]
}'
```

**Response (200 OK):**
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

## Frontend Implementation

### JavaScript/TypeScript Service Layer

```javascript
// employeeService.js

const API_BASE_URL = 'http://localhost:8080/api/hr/employee';

export const employeeService = {
  // Get employee by ID
  async getEmployeeById(id) {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    return this.handleResponse(response);
  },

  // Create new employee
  async createEmployee(data) {
    const response = await fetch(`${API_BASE_URL}/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return this.handleResponse(response);
  },

  // Update employee
  async updateEmployee(id, data) {
    const response = await fetch(`${API_BASE_URL}/update/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return this.handleResponse(response);
  },

  // Handle response
  async handleResponse(response) {
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'API request failed');
    }
    if (!data.success) {
      throw new Error(data.message || 'Operation failed');
    }
    return data.data;
  }
};
```

### React Component Example

```javascript
// EmployeeFormPage.jsx

import React, { useState, useEffect } from 'react';
import { employeeService } from './services/employeeService';

export default function EmployeeFormPage({ employeeId }) {
  const isEditMode = !!employeeId;
  
  const [formData, setFormData] = useState({
    organizationId: 1,
    employeeCode: '',
    fullName: '',
    email: '',
    phone: '',
    cnic: '',
    address: '',
    city: '',
    gender: '',
    dateOfBirth: '',
    departmentId: null,
    designation: '',
    employmentType: 'Permanent',
    status: 'Active',
    joiningDate: '',
    basicSalary: 0,
    bankName: '',
    bankAccountNumber: '',
    bankBranchCode: '',
    allowances: [],
    deductions: []
  });

  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  // Load employee data if editing
  useEffect(() => {
    if (isEditMode) {
      loadEmployee();
    }
  }, [employeeId]);

  const loadEmployee = async () => {
    try {
      const employee = await employeeService.getEmployeeById(employeeId);
      setFormData(employee);
    } catch (error) {
      setMessage(`Error loading employee: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.fullName.trim()) newErrors.fullName = 'Full name is required';
    if (!formData.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) newErrors.email = 'Invalid email';
    if (!formData.phone.match(/^\+\d{2}-\d{3}-\d{7}$/)) newErrors.phone = 'Phone: +92-XXX-XXXXXXX';
    if (!formData.cnic.match(/^\d{5}-\d{7}-\d{1}$/)) newErrors.cnic = 'CNIC: XXXXX-XXXXXXX-X';
    if (formData.basicSalary <= 0) newErrors.basicSalary = 'Salary must be > 0';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      const result = isEditMode
        ? await employeeService.updateEmployee(employeeId, formData)
        : await employeeService.createEmployee(formData);
      
      setMessage(`Employee ${isEditMode ? 'updated' : 'created'} successfully!`);
      setTimeout(() => {
        window.location.href = `/employees/${result.id}`;
      }, 1500);
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) return <div>Loading...</div>;

  return (
    <div className="employee-form-page">
      <h1>{isEditMode ? 'Edit Employee' : 'Create Employee'}</h1>
      
      {message && (
        <div className={`message ${message.includes('Error') ? 'error' : 'success'}`}>
          {message}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Personal Information */}
        <fieldset>
          <legend>Personal Information</legend>
          
          <div className="form-group">
            <label>Full Name *</label>
            <input
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleInputChange}
              className={errors.fullName ? 'error' : ''}
              required
            />
            {errors.fullName && <span className="error-msg">{errors.fullName}</span>}
          </div>

          <div className="form-group">
            <label>Email *</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange}
              className={errors.email ? 'error' : ''}
              required
            />
            {errors.email && <span className="error-msg">{errors.email}</span>}
          </div>

          <div className="form-group">
            <label>Phone *</label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleInputChange}
              placeholder="+92-300-1234567"
              className={errors.phone ? 'error' : ''}
              required
            />
            {errors.phone && <span className="error-msg">{errors.phone}</span>}
          </div>

          <div className="form-group">
            <label>CNIC *</label>
            <input
              type="text"
              name="cnic"
              value={formData.cnic}
              onChange={handleInputChange}
              placeholder="12345-6789012-3"
              className={errors.cnic ? 'error' : ''}
              required
            />
            {errors.cnic && <span className="error-msg">{errors.cnic}</span>}
          </div>

          <div className="form-group">
            <label>City *</label>
            <input
              type="text"
              name="city"
              value={formData.city}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Date of Birth *</label>
            <input
              type="date"
              name="dateOfBirth"
              value={formData.dateOfBirth}
              onChange={handleInputChange}
              required
            />
          </div>
        </fieldset>

        {/* Job Information */}
        <fieldset>
          <legend>Job Information</legend>
          
          <div className="form-group">
            <label>Department *</label>
            <select
              name="departmentId"
              value={formData.departmentId || ''}
              onChange={handleInputChange}
              required
            >
              <option value="">Select Department</option>
              {/* Populate from API */}
            </select>
          </div>

          <div className="form-group">
            <label>Designation *</label>
            <input
              type="text"
              name="designation"
              value={formData.designation}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Employment Type *</label>
            <select
              name="employmentType"
              value={formData.employmentType}
              onChange={handleInputChange}
              required
            >
              <option value="Permanent">Permanent</option>
              <option value="Contract">Contract</option>
              <option value="Temporary">Temporary</option>
            </select>
          </div>

          <div className="form-group">
            <label>Joining Date *</label>
            <input
              type="date"
              name="joiningDate"
              value={formData.joiningDate}
              onChange={handleInputChange}
              required
            />
          </div>
        </fieldset>

        {/* Financial Information */}
        <fieldset>
          <legend>Financial Information</legend>
          
          <div className="form-group">
            <label>Basic Salary *</label>
            <input
              type="number"
              name="basicSalary"
              value={formData.basicSalary}
              onChange={handleInputChange}
              min="0"
              step="0.01"
              className={errors.basicSalary ? 'error' : ''}
              required
            />
            {errors.basicSalary && <span className="error-msg">{errors.basicSalary}</span>}
          </div>

          <div className="form-group">
            <label>Bank Name *</label>
            <input
              type="text"
              name="bankName"
              value={formData.bankName}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Bank Account Number *</label>
            <input
              type="text"
              name="bankAccountNumber"
              value={formData.bankAccountNumber}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Bank Branch Code *</label>
            <input
              type="text"
              name="bankBranchCode"
              value={formData.bankBranchCode}
              onChange={handleInputChange}
              required
            />
          </div>
        </fieldset>

        {/* Allowances */}
        <fieldset>
          <legend>Allowances</legend>
          <button
            type="button"
            onClick={() => setFormData(prev => ({
              ...prev,
              allowances: [...prev.allowances, { allowanceType: '', amount: 0 }]
            }))}
            className="btn-secondary"
          >
            + Add Allowance
          </button>
          <div className="list">
            {formData.allowances.map((allowance, idx) => (
              <div key={idx} className="item">
                <input
                  type="text"
                  placeholder="Type"
                  value={allowance.allowanceType}
                  onChange={(e) => {
                    const newAllowances = [...formData.allowances];
                    newAllowances[idx].allowanceType = e.target.value;
                    setFormData(prev => ({ ...prev, allowances: newAllowances }));
                  }}
                />
                <input
                  type="number"
                  placeholder="Amount"
                  value={allowance.amount}
                  onChange={(e) => {
                    const newAllowances = [...formData.allowances];
                    newAllowances[idx].amount = parseFloat(e.target.value);
                    setFormData(prev => ({ ...prev, allowances: newAllowances }));
                  }}
                  min="0"
                />
                <button
                  type="button"
                  onClick={() => setFormData(prev => ({
                    ...prev,
                    allowances: prev.allowances.filter((_, i) => i !== idx)
                  }))}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </fieldset>

        {/* Deductions */}
        <fieldset>
          <legend>Deductions</legend>
          <button
            type="button"
            onClick={() => setFormData(prev => ({
              ...prev,
              deductions: [...prev.deductions, { deductionType: '', amount: 0 }]
            }))}
            className="btn-secondary"
          >
            + Add Deduction
          </button>
          <div className="list">
            {formData.deductions.map((deduction, idx) => (
              <div key={idx} className="item">
                <input
                  type="text"
                  placeholder="Type"
                  value={deduction.deductionType}
                  onChange={(e) => {
                    const newDeductions = [...formData.deductions];
                    newDeductions[idx].deductionType = e.target.value;
                    setFormData(prev => ({ ...prev, deductions: newDeductions }));
                  }}
                />
                <input
                  type="number"
                  placeholder="Amount"
                  value={deduction.amount}
                  onChange={(e) => {
                    const newDeductions = [...formData.deductions];
                    newDeductions[idx].amount = parseFloat(e.target.value);
                    setFormData(prev => ({ ...prev, deductions: newDeductions }));
                  }}
                  min="0"
                />
                <button
                  type="button"
                  onClick={() => setFormData(prev => ({
                    ...prev,
                    deductions: prev.deductions.filter((_, i) => i !== idx)
                  }))}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </fieldset>

        <div className="form-actions">
          <button type="button" onClick={() => window.history.back()}>Cancel</button>
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : isEditMode ? 'Update Employee' : 'Create Employee'}
          </button>
        </div>
      </form>
    </div>
  );
}
```

---

## Error Handling

### Common Errors and Solutions

| Error | Status | Solution |
|-------|--------|----------|
| Email format is invalid | 400 | Use format: name@domain.com |
| Phone must start with country code | 400 | Use format: +92-XXX-XXXXXXX |
| CNIC format invalid | 400 | Use format: XXXXX-XXXXXXX-X |
| Employee not found | 404 | Verify employee ID exists |
| Organization not found | 400 | Verify organizationId exists |
| Department not found | 400 | Verify departmentId exists |
| Server error | 500 | Check backend logs |

---

## Validation Patterns

```javascript
const PATTERNS = {
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  phone: /^\+\d{2}-\d{3}-\d{7}$/,
  cnic: /^\d{5}-\d{7}-\d{1}$/,
  bankAccount: /^\d{8,20}$/,
  salary: /^\d+(\.\d{1,2})?$/
};
```

---

## Form State Management

```javascript
const initialFormState = {
  organizationId: 1,
  employeeCode: '',
  fullName: '',
  email: '',
  phone: '',
  cnic: '',
  address: '',
  city: '',
  gender: '',
  dateOfBirth: '',
  departmentId: null,
  designation: '',
  employmentType: 'Permanent',
  status: 'Active',
  joiningDate: '',
  basicSalary: 0,
  bankName: '',
  bankAccountNumber: '',
  bankBranchCode: '',
  allowances: [],
  deductions: []
};
```

---

## Testing Scenarios

1. **Create Flow:**
   - Fill form → Validate → Submit → Success → Redirect

2. **Edit Flow:**
   - Load employee → Populate form → Modify → Submit → Success

3. **Error Handling:**
   - Network errors → Show message
   - Validation errors → Highlight fields
   - API errors → Display message

---

## Postman Collection

A Postman collection is available: `HR_PAYROLL_Employee_API.postman_collection.json`

Import and use for testing all endpoints.

---

**Version:** 1.0  
**Last Updated:** 2024-01-26
