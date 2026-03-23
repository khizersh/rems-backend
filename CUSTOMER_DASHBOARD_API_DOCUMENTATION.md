# Customer Dashboard API Documentation

## Overview
This module provides REST APIs for a **Customer Home Page Dashboard** in the Real Estate ERP system. All APIs are secured and derive customer identity from JWT token - no customer ID is required in requests.

## Security Model
- **Authentication**: JWT token required in header
- **User Resolution**: Username extracted from JWT → User table → Customer table
- **No Manual IDs**: All customer data is automatically filtered based on logged-in user

## Base URL
```
/api/customer/dashboard
```

## API Endpoints

### 1. Customer Summary API
**Endpoint**: `GET /api/customer/dashboard/summary`

**Description**: Returns overview KPIs for the customer dashboard including total bookings, amount payable, paid, and remaining.

**Authentication**: Required (JWT Token)

**Response Structure**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "customerName": "John Doe",
    "nationalId": "12345-6789012-3",
    "contactNo": "+92-300-1234567",
    "email": "john.doe@example.com",
    "totalBookings": 2,
    "totalUnitsBooked": 2,
    "totalAmountPayable": 5000000.00,
    "totalAmountPaid": 2000000.00,
    "totalRemainingAmount": 3000000.00,
    "overdueAmount": 0.0
  }
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/summary' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Use Case**: Display KPI cards on dashboard home page

---

### 2. Payment Chart API
**Endpoint**: `GET /api/customer/dashboard/payment-chart`

**Description**: Returns monthly aggregated payment data for visualization (bar charts, line graphs).

**Authentication**: Required (JWT Token)

**Response Structure**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": [
    {
      "month": 1,
      "year": 2024,
      "monthName": "Jan",
      "totalPaidAmount": 150000.00,
      "totalDueAmount": 0.0,
      "cumulativeRemaining": 0.0
    },
    {
      "month": 2,
      "year": 2024,
      "monthName": "Feb",
      "totalPaidAmount": 200000.00,
      "totalDueAmount": 0.0,
      "cumulativeRemaining": 0.0
    }
  ]
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/payment-chart' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Use Case**: Monthly payment trends visualization

---

### 3. Payment Mode Distribution API
**Endpoint**: `GET /api/customer/dashboard/payment-modes`

**Description**: Returns breakdown of payments by payment method (Cash, Bank Transfer, Cheque, etc.) for pie chart visualization.

**Authentication**: Required (JWT Token)

**Response Structure**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": [
    {
      "paymentMode": "BANK_TRANSFER",
      "totalAmount": 1500000.00,
      "transactionCount": 15
    },
    {
      "paymentMode": "CASH",
      "totalAmount": 300000.00,
      "transactionCount": 5
    },
    {
      "paymentMode": "CHEQUE",
      "totalAmount": 200000.00,
      "transactionCount": 2
    }
  ]
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/payment-modes' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Use Case**: Payment mode distribution pie chart

---

### 4. Recent Payments API
**Endpoint**: `GET /api/customer/dashboard/recent-payments`

**Description**: Returns the most recent payment transactions with details.

**Authentication**: Required (JWT Token)

**Query Parameters**:
- `limit` (optional, default: 10) - Number of recent payments to fetch

**Response Structure**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": [
    {
      "paymentId": 123,
      "accountId": 45,
      "projectName": "Green Valley Apartments",
      "unitSerial": "A-101",
      "totalPaymentAmount": 150000.00,
      "receivedAmount": 150000.00,
      "paidDate": "2024-01-15T10:30:00",
      "paymentStatus": "PAID",
      "paymentDetails": [
        {
          "paymentType": "BANK_TRANSFER",
          "amount": 100000.00,
          "chequeNo": null,
          "chequeDate": null
        },
        {
          "paymentType": "CASH",
          "amount": 50000.00,
          "chequeNo": null,
          "chequeDate": null
        }
      ]
    }
  ]
}
```

**CURL Commands**:

Default (10 records):
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/recent-payments' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

With custom limit:
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/recent-payments?limit=20' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Use Case**: Recent transactions table on dashboard

---

### 5. Account Status API
**Endpoint**: `GET /api/customer/dashboard/accounts`

**Description**: Returns all customer accounts (bookings/units) with payment status and remaining balance.

**Authentication**: Required (JWT Token)

**Response Structure**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": [
    {
      "accountId": 45,
      "projectName": "Green Valley Apartments",
      "unitSerial": "A-101",
      "unitType": "APARTMENT",
      "totalAmount": 2500000.00,
      "totalPaidAmount": 1000000.00,
      "totalBalanceAmount": 1500000.00,
      "status": "ACTIVE",
      "durationInMonths": 36
    },
    {
      "accountId": 46,
      "projectName": "Sunshine Plaza",
      "unitSerial": "S-205",
      "unitType": "SHOP",
      "totalAmount": 2500000.00,
      "totalPaidAmount": 2500000.00,
      "totalBalanceAmount": 0.0,
      "status": "CLOSED",
      "durationInMonths": 24
    }
  ]
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8080/api/customer/dashboard/accounts' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Use Case**: List all units/properties with payment status

---

## Authentication

All endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

### Getting JWT Token

First, login to get the token:

```bash
curl --location 'http://localhost:8080/api/user/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "customer_username",
  "password": "customer_password"
}'
```

Response will contain:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {...}
  }
}
```

Use the `token` value in subsequent requests.

---

## Error Responses

### Invalid/Missing Token
```json
{
  "responseCode": "0003",
  "responseMessage": "Invalid Credentials!"
}
```

### Customer Not Found
```json
{
  "responseCode": "0002",
  "responseMessage": "Invalid Parameter!",
  "data": "Customer profile not found for this user"
}
```

### System Error
```json
{
  "responseCode": "9999",
  "responseMessage": "System Failure!",
  "data": "Error details..."
}
```

---

## Dashboard UI Recommendations

### Layout Suggestions

1. **Top KPI Cards** (from `/summary`)
   - Total Bookings
   - Total Amount Payable
   - Total Paid
   - Remaining Balance

2. **Charts Section**
   - Monthly Payment Bar Chart (from `/payment-chart`)
   - Payment Mode Pie Chart (from `/payment-modes`)

3. **Recent Activity Table** (from `/recent-payments?limit=5`)
   - Last 5 payments
   - Show: Date, Project, Unit, Amount, Status

4. **My Properties/Units Section** (from `/accounts`)
   - Cards or table showing all units
   - Each card shows: Project, Unit, Paid, Remaining, Status
   - Color coding: Green (Closed), Blue (Active), Red (Overdue)

---

## Database Schema

### Relationship Chain
```
users → customer → customer_account → customer_payment → customer_payment_detail
```

### Key Tables

**users**
- id (PK)
- username
- organization_id

**customer**
- customer_id (PK)
- user_id (FK → users.id)
- name, national_id, contact_no, email

**customer_account**
- id (PK)
- customer_id (FK → customer.customer_id)
- project_id, unit_id
- total_amount, total_paid_amount, total_balance_amount

**customer_payment**
- id (PK)
- customer_account_id (FK → customer_account.id)
- amount, received_amount, remaining_amount
- payment_status, paid_date

**customer_payment_detail**
- id (PK)
- customer_payment_id (FK → customer_payment.id)
- payment_type (CASH, BANK_TRANSFER, CHEQUE, etc.)
- amount, cheque_no, cheque_date

---

## Implementation Details

### Files Created

**DTOs**:
- `CustomerSummaryDTO.java`
- `PaymentChartDTO.java`
- `PaymentModeDistributionDTO.java`
- `RecentPaymentDTO.java`
- `AccountStatusDTO.java`

**Service**:
- `CustomerDashboardService.java`

**Controller**:
- `CustomerDashboardController.java`

**Repository Extensions**:
- `CustomerAccountRepo.java` - Added dashboard aggregate queries
- `CustomerPaymentRepo.java` - Added monthly payment queries
- `CustomerPaymentDetailRepo.java` - Added payment mode distribution query

### Key Features

✅ **JWT-based security** - No manual customer ID needed  
✅ **Aggregate queries** - Optimized for performance  
✅ **DTO projections** - Clean API responses  
✅ **Zero null safety** - Uses COALESCE in queries  
✅ **Pagination ready** - Recent payments support limit  
✅ **Enum handling** - Proper enum to string conversion  
✅ **Error handling** - Comprehensive exception management  

---

## Testing Checklist

- [ ] Login as customer user and get JWT token
- [ ] Test each endpoint with valid token
- [ ] Verify data matches database
- [ ] Test with user having no customer profile (should fail gracefully)
- [ ] Test with invalid/expired token
- [ ] Test payment chart with different date ranges
- [ ] Test recent payments with different limits
- [ ] Verify all amounts aggregate correctly
- [ ] Check payment mode distribution sums correctly
- [ ] Verify account status calculations

---

## Future Enhancements

1. **Overdue Calculation**: Integrate with payment schedule to calculate overdue amounts
2. **Next Due Date**: Show next installment due date
3. **Payment Reminders**: Flag accounts with upcoming due dates
4. **Download Statements**: Generate PDF payment history
5. **Payment Filters**: Filter by date range, project, or payment mode
6. **Caching**: Add Redis caching for frequently accessed data
7. **Push Notifications**: Alert customers about due payments

---

## Support

For issues or questions, contact the development team or refer to the main project documentation.
