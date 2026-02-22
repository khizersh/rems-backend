# Customer Management Module - Frontend Integration Guide

## Overview

This document provides complete integration details for the Customer Management Module APIs designed for customer account pages, payment management, and ledger functionality. The system supports **one customer having multiple accounts**, **one account having multiple payments**, and **one payment having multiple payment details**.

## Architecture Overview

```
Customer (1) → CustomerAccount (N) → CustomerPayment (N) → CustomerPaymentDetail (N)
```

- **Customer**: Individual customer profile
- **CustomerAccount**: Booking/unit accounts (multiple per customer)  
- **CustomerPayment**: Payment records (multiple per account)
- **CustomerPaymentDetail**: Payment method breakdown (multiple per payment)

## Authentication & Security

**All APIs use JWT token-based authentication:**
- Customer identity is derived from JWT token username
- No need to pass customerId or userId in requests
- Include `Authorization: Bearer <JWT_TOKEN>` header in all requests

## API Endpoints Structure

### Base URLs:
- **Customer Accounts**: `/api/customer/accounts`
- **Customer Payments**: `/api/customer/payments`  
- **Customer Ledger**: `/api/customer/ledger`
- **Customer Dashboard**: `/api/customer/dashboard`

---

## 1. Customer Accounts APIs

### 1.1 Get All Customer Accounts
**Endpoint**: `POST /api/customer/accounts/getAll`

**Purpose**: Retrieve all accounts for logged-in customer with pagination

**Request Body**:
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc",
  "id": [],
  "filteredBy": ""
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 123,
        "customerId": 456,
        "customerName": "John Doe",
        "customerContact": "+1234567890",
        "customerEmail": "john.doe@email.com",
        "projectId": 789,
        "projectName": "Green Valley Apartments",
        "unitId": 101,
        "unitSerial": "A-001",
        "unitType": "TWO_BED",
        "durationInMonths": 36,
        "actualAmount": 500000.0,
        "miscellaneousAmount": 25000.0,
        "developmentAmount": 50000.0,
        "downPayment": 100000.0,
        "totalAmount": 675000.0,
        "quarterlyPayment": 56250.0,
        "halfYearly": 112500.0,
        "onPossessionAmount": 50000.0,
        "totalPaidAmount": 200000.0,
        "totalBalanceAmount": 475000.0,
        "active": true,
        "createdBy": "admin",
        "createdDate": "2024-02-15T10:30:00",
        "updatedDate": "2024-02-15T10:30:00",
        "totalPaymentsCount": 3,
        "lastPaymentAmount": 50000.0,
        "lastPaymentDate": "2024-02-10T14:20:00",
        "paymentStatus": "PARTIALLY_PAID"
      }
    ],
    "totalPages": 5,
    "totalElements": 45,
    "number": 0,
    "size": 10
  }
}
```

### 1.2 Get Account by ID
**Endpoint**: `GET /api/customer/accounts/{accountId}`

**Purpose**: Get detailed information for a specific account

**Response**: Same as single account object above

### 1.3 Get Account Summary
**Endpoint**: `GET /api/customer/accounts/summary`

**Purpose**: Get aggregate summary of all customer accounts

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "customerId": 456,
    "customerName": "John Doe",
    "customerContact": "+1234567890",
    "totalAccounts": 3,
    "activeAccounts": 2,
    "closedAccounts": 1,
    "totalBookingAmount": 2025000.0,
    "totalPaidAmount": 800000.0,
    "totalBalanceAmount": 1225000.0,
    "totalOverdueAmount": 0.0,
    "totalPaymentsCount": 15,
    "averagePaymentAmount": 53333.33
  }
}
```

### 1.4 Get Accounts by Project
**Endpoint**: `POST /api/customer/accounts/getByProject/{projectId}`

**Purpose**: Get all customer accounts for a specific project

**Request Body**: Same pagination format as 1.1

---

## 2. Customer Payments APIs

### 2.1 Get All Customer Payments
**Endpoint**: `POST /api/customer/payments/getAll`

**Purpose**: Get all payments across all customer accounts

**Request Body**: Standard pagination format

**Response**:
```json
{
  "status": "SUCCESS",
  "data": [
    {
      "id": 789,
      "serialNo": 1001,
      "customerAccountId": 123,
      "customerName": "John Doe",
      "unitSerial": "A-001",
      "projectName": "Green Valley Apartments",
      "amount": 50000.0,
      "receivedAmount": 50000.0,
      "remainingAmount": 0.0,
      "paymentType": "INSTALLMENT",
      "paymentStatus": "PAID",
      "paidDate": "2024-02-10T14:20:00",
      "isPaymentAddedToAccount": true,
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdDate": "2024-02-10T14:20:00",
      "updatedDate": "2024-02-10T14:20:00",
      "paymentDetails": [
        {
          "id": 1001,
          "customerPaymentId": 789,
          "amount": 30000.0,
          "paymentType": "CASH",
          "customerPaymentReason": "INSTALLMENT_PAYMENT",
          "chequeNo": null,
          "chequeDate": null,
          "createdBy": "admin",
          "createdDate": "2024-02-10T14:20:00"
        },
        {
          "id": 1002,
          "customerPaymentId": 789,
          "amount": 20000.0,
          "paymentType": "BANK_TRANSFER",
          "customerPaymentReason": "INSTALLMENT_PAYMENT",
          "chequeNo": null,
          "chequeDate": null,
          "createdBy": "admin",
          "createdDate": "2024-02-10T14:20:00"
        }
      ]
    }
  ]
}
```

### 2.2 Get Payments by Account
**Endpoint**: `POST /api/customer/payments/getByAccount/{accountId}`

**Purpose**: Get all payments for a specific account with pagination

### 2.3 Get Payment Details
**Endpoint**: `GET /api/customer/payments/{paymentId}`

**Purpose**: Get detailed information for a specific payment

### 2.4 Get Payments by Status
**Endpoint**: `POST /api/customer/payments/getByStatus/{status}`

**Purpose**: Filter payments by status (PAID, UNPAID, PENDING)

**Valid Status Values**: `PAID`, `UNPAID`, `PENDING`

### 2.5 Get Payments by Date Range
**Endpoint**: `POST /api/customer/payments/getByDateRange?startDate=2024-01-01&endDate=2024-12-31`

**Purpose**: Get payments within a date range

### 2.6 Get Payment Summary
**Endpoint**: `GET /api/customer/payments/summary`

**Purpose**: Get payment statistics for the customer

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "totalPayments": 15,
    "paidPayments": 12,
    "pendingPayments": 3,
    "totalAmount": 750000.0,
    "receivedAmount": 600000.0,
    "remainingAmount": 150000.0
  }
}
```

---

## 3. Customer Ledger APIs

### 3.1 Get Complete Customer Ledger
**Endpoint**: `POST /api/customer/ledger/getAll`

**Purpose**: Get complete transaction ledger across all accounts

**Request Body**: Standard pagination format

**Response**:
```json
{
  "status": "SUCCESS",
  "data": [
    {
      "id": 1001,
      "transactionType": "PAYMENT_IN",
      "referenceType": "PAYMENT_DETAIL",
      "referenceId": 1001,
      "description": "Payment received - INSTALLMENT_PAYMENT",
      "debitAmount": 0.0,
      "creditAmount": 30000.0,
      "runningBalance": 600000.0,
      "transactionDate": "2024-02-10T14:20:00",
      "customerName": "John Doe",
      "unitSerial": "A-001",
      "projectName": "Green Valley Apartments",
      "paymentMode": "CASH",
      "chequeNo": null,
      "chequeDate": null,
      "createdBy": "admin"
    }
  ]
}
```

### 3.2 Get Account Ledger
**Endpoint**: `POST /api/customer/ledger/getByAccount/{accountId}`

**Purpose**: Get ledger for a specific account only

**Sorting**: Returns transactions sorted by createdDate ASC (oldest transactions first)

### 3.3 Get Ledger by Payment Type
**Endpoint**: `POST /api/customer/ledger/getByPaymentType/{paymentType}`

**Purpose**: Filter ledger entries by payment type

**Valid Payment Types**: `CASH`, `BANK_TRANSFER`, `CHEQUE`, `ONLINE_PAYMENT`

### 3.4 Get Ledger Summary
**Endpoint**: `GET /api/customer/ledger/summary`

**Purpose**: Get ledger summary statistics

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "totalCredits": 600000.0,
    "totalDebits": 0.0,
    "totalTransactions": 25,
    "paymentTypeBreakdown": {
      "CASH": 200000.0,
      "BANK_TRANSFER": 300000.0,
      "CHEQUE": 100000.0
    },
    "netBalance": 600000.0
  }
}
```

---

## 4. Customer Dashboard APIs (Already Implemented)

### 4.1 Customer Summary
**Endpoint**: `GET /api/customer/dashboard/summary`

### 4.2 Payment Chart Data
**Endpoint**: `GET /api/customer/dashboard/payment-chart`

### 4.3 Payment Mode Distribution
**Endpoint**: `GET /api/customer/dashboard/payment-modes`

### 4.4 Recent Payments (Modified)
**Endpoint**: `GET /api/customer/dashboard/recent-payments?limit=10`

### 4.5 Account Status
**Endpoint**: `GET /api/customer/dashboard/accounts`

---

## Frontend Implementation Guide

### React Component Structure

```jsx
// Main Customer Portal Structure
CustomerPortal/
├── Dashboard/
│   ├── CustomerSummary.jsx
│   ├── PaymentChart.jsx
│   └── RecentPayments.jsx
├── Accounts/
│   ├── AccountsList.jsx
│   ├── AccountDetails.jsx
│   └── AccountSummary.jsx
├── Payments/
│   ├── PaymentsList.jsx
│   ├── PaymentDetails.jsx
│   └── PaymentHistory.jsx
└── Ledger/
    ├── LedgerView.jsx
    ├── AccountLedger.jsx
    └── LedgerSummary.jsx
```

### Sample React Hook for API Calls

```jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

// Custom hook for customer accounts
export const useCustomerAccounts = (page = 0, size = 10) => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(0);

  const fetchAccounts = async () => {
    setLoading(true);
    try {
      const response = await axios.post('/api/customer/accounts/getAll', {
        page,
        size,
        sortBy: 'createdDate',
        sortDir: 'desc',
        id: [],
        filteredBy: ''
      }, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.data.status === 'SUCCESS') {
        setAccounts(response.data.data.content);
        setTotalPages(response.data.data.totalPages);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, [page, size]);

  return { accounts, loading, error, totalPages, refetch: fetchAccounts };
};
```

### Sample Component Implementation

```jsx
import React from 'react';
import { useCustomerAccounts } from '../hooks/useCustomerAccounts';

const AccountsList = () => {
  const [currentPage, setCurrentPage] = useState(0);
  const { accounts, loading, error, totalPages } = useCustomerAccounts(currentPage, 10);

  if (loading) return <div className="loading">Loading accounts...</div>;
  if (error) return <div className="error">Error: {error}</div>;

  return (
    <div className="accounts-list">
      <h2>My Accounts</h2>
      <div className="accounts-grid">
        {accounts.map(account => (
          <div key={account.id} className="account-card">
            <h3>{account.projectName}</h3>
            <p>Unit: {account.unitSerial}</p>
            <p>Total Amount: ${account.totalAmount.toLocaleString()}</p>
            <p>Paid: ${account.totalPaidAmount.toLocaleString()}</p>
            <p>Balance: ${account.totalBalanceAmount.toLocaleString()}</p>
            <span className={`status ${account.paymentStatus.toLowerCase()}`}>
              {account.paymentStatus}
            </span>
          </div>
        ))}
      </div>
      
      {/* Pagination Component */}
      <div className="pagination">
        {Array.from({ length: totalPages }, (_, i) => (
          <button
            key={i}
            onClick={() => setCurrentPage(i)}
            className={currentPage === i ? 'active' : ''}
          >
            {i + 1}
          </button>
        ))}
      </div>
    </div>
  );
};
```

### Error Handling

```jsx
const handleApiError = (error) => {
  if (error.response?.status === 401) {
    // Redirect to login
    window.location.href = '/login';
  } else if (error.response?.status === 403) {
    // Show access denied message
    showNotification('Access denied', 'error');
  } else {
    // Show general error message
    showNotification(error.response?.data?.message || 'An error occurred', 'error');
  }
};
```

## cURL Examples for Testing

### 1. Get All Accounts
```bash
curl -X POST "http://localhost:8080/api/customer/accounts/getAll" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 10,
    "sortBy": "createdDate",
    "sortDir": "desc",
    "id": [],
    "filteredBy": ""
  }'
```

### 2. Get Account Summary
```bash
curl -X GET "http://localhost:8080/api/customer/accounts/summary" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Get All Payments
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getAll" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 10,
    "sortBy": "paidDate",
    "sortDir": "desc",
    "id": [],
    "filteredBy": ""
  }'
```

### 4. Get Payment Details
```bash
curl -X GET "http://localhost:8080/api/customer/payments/789" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Get Payments by Status
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getByStatus/PAID" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 10,
    "sortBy": "paidDate",
    "sortDir": "desc",
    "id": [],
    "filteredBy": ""
  }'
```

### 6. Get Complete Ledger
```bash
curl -X POST "http://localhost:8080/api/customer/ledger/getAll" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 20,
    "sortBy": "transactionDate",
    "sortDir": "desc",
    "id": [],
    "filteredBy": ""
  }'
```

### 7. Get Account Ledger
```bash
curl -X POST "http://localhost:8080/api/customer/ledger/getByAccount/123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 15,
    "sortBy": "transactionDate",
    "sortDir": "desc",
    "id": [],
    "filteredBy": ""
  }'
```

### 8. Get Ledger Summary
```bash
curl -X GET "http://localhost:8080/api/customer/ledger/summary" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Data Relationships Visualization

```
Customer (John Doe)
├── CustomerAccount #1 (Green Valley - Unit A-001)
│   ├── Payment #1 ($50,000)
│   │   ├── PaymentDetail #1 (Cash: $30,000)
│   │   └── PaymentDetail #2 (Bank Transfer: $20,000)
│   ├── Payment #2 ($25,000)
│   │   └── PaymentDetail #3 (Cheque: $25,000)
│   └── Payment #3 ($75,000)
│       ├── PaymentDetail #4 (Cash: $50,000)
│       └── PaymentDetail #5 (Online: $25,000)
├── CustomerAccount #2 (Blue Heights - Unit B-102)
│   └── Payment #4 ($100,000)
│       ├── PaymentDetail #6 (Bank Transfer: $60,000)
│       └── PaymentDetail #7 (Cash: $40,000)
└── CustomerAccount #3 (City Center - Unit C-205)
    ├── Payment #5 ($80,000)
    │   └── PaymentDetail #8 (Online: $80,000)
    └── Payment #6 ($45,000)
        └── PaymentDetail #9 (Cheque: $45,000)
```

## Key Features Implemented

✅ **JWT-based Authentication** - All APIs extract customer from token  
✅ **Multiple Accounts per Customer** - Full support  
✅ **Multiple Payments per Account** - Full support  
✅ **Multiple Payment Details per Payment** - Full support  
✅ **Pagination** - Consistent across all list APIs  
✅ **Sorting & Filtering** - Standard pagination request format  
✅ **Comprehensive DTOs** - Rich response objects with all needed data  
✅ **Error Handling** - Proper error responses with meaningful messages  
✅ **Security Validation** - Customer ownership verification for all operations  
✅ **Running Balance Calculation** - In ledger APIs  
✅ **Summary & Analytics** - Multiple summary endpoints  
✅ **Date Range Filtering** - For payments and transactions  

## Next Steps for Frontend Integration

1. **Create API Service Layer**: Set up axios interceptors and base configurations
2. **Implement State Management**: Use Redux/Context for customer data
3. **Create Reusable Components**: Data tables, pagination, filters
4. **Add Loading States**: Skeleton loaders for better UX
5. **Implement Error Boundaries**: Graceful error handling
6. **Add Offline Support**: Cache critical data for offline viewing
7. **Implement Real-time Updates**: WebSocket for payment notifications
8. **Add Export Functionality**: PDF/Excel export for statements and ledgers

This comprehensive module provides a complete foundation for customer-facing account, payment, and ledger management in your real estate ERP system.
