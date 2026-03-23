# Customer Management Module - Quick API Reference

## 🏠 Customer Account APIs

### Get All Accounts (Paginated)
```bash
curl -X POST "http://localhost:8080/api/customer/accounts/getAll" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"createdDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Account Details
```bash
curl -X GET "http://localhost:8080/api/customer/accounts/{accountId}" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Account Summary
```bash
curl -X GET "http://localhost:8080/api/customer/accounts/summary" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Accounts by Project
```bash
curl -X POST "http://localhost:8080/api/customer/accounts/getByProject/{projectId}" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"createdDate","sortDir":"desc","id":[],"filteredBy":""}'
```

## 💰 Customer Payment APIs

### Get All Payments
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getAll" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"paidDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Payments by Account
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getByAccount/{accountId}" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"paidDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Payment Details
```bash
curl -X GET "http://localhost:8080/api/customer/payments/{paymentId}" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Payments by Status
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getByStatus/PAID" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"paidDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Payments by Date Range
```bash
curl -X POST "http://localhost:8080/api/customer/payments/getByDateRange?startDate=2024-01-01&endDate=2024-12-31" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sortBy":"paidDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Payment Summary
```bash
curl -X GET "http://localhost:8080/api/customer/payments/summary" \
  -H "Authorization: Bearer JWT_TOKEN"
```

## 📊 Customer Ledger APIs

### Get Complete Ledger
```bash
curl -X POST "http://localhost:8080/api/customer/ledger/getAll" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":20,"sortBy":"transactionDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Account Ledger
```bash
curl -X POST "http://localhost:8080/api/customer/ledger/getByAccount/{accountId}" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":15,"sortBy":"transactionDate","sortDir":"desc","id":[],"filteredBy":""}'
```

**Enhanced Response**: Now includes additional account summary data from `getAllPaymentDetailsByAccountId`:
- `ledgerEntries`: Array of transaction entries sorted by createdDate ASC (oldest first)
- `totalAmount`: Account total amount  
- `grandTotal`: Total amount paid
- `balanceAmount`: Remaining balance
- `customer`: Complete customer details

### Get Ledger by Payment Type
```bash
curl -X POST "http://localhost:8080/api/customer/ledger/getByPaymentType/CASH" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":15,"sortBy":"transactionDate","sortDir":"desc","id":[],"filteredBy":""}'
```

### Get Ledger Summary
```bash
curl -X GET "http://localhost:8080/api/customer/ledger/summary" \
  -H "Authorization: Bearer JWT_TOKEN"
```

## 📈 Customer Dashboard APIs (Existing)

### Get Customer Summary
```bash
curl -X GET "http://localhost:8080/api/customer/dashboard/summary" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Payment Chart Data
```bash
curl -X GET "http://localhost:8080/api/customer/dashboard/payment-chart" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Payment Mode Distribution
```bash
curl -X GET "http://localhost:8080/api/customer/dashboard/payment-modes" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Recent Payments (Limited by payment details)
```bash
curl -X GET "http://localhost:8080/api/customer/dashboard/recent-payments?limit=10" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Get Account Status
```bash
curl -X GET "http://localhost:8080/api/customer/dashboard/accounts" \
  -H "Authorization: Bearer JWT_TOKEN"
```

## 🔐 Authentication Notes

- Replace `JWT_TOKEN` with actual JWT token from login
- All APIs automatically resolve customer from JWT token username
- No need to pass customerId or userId in requests
- Token must belong to a user mapped to a customer record

## 📋 Data Relationships

```
Customer (1) → CustomerAccount (Many) → CustomerPayment (Many) → CustomerPaymentDetail (Many)
```

## 🎯 Key Response Fields

### CustomerAccount
- `id`, `customerName`, `projectName`, `unitSerial`
- `totalAmount`, `totalPaidAmount`, `totalBalanceAmount`
- `paymentStatus`: `FULLY_PAID`, `PARTIALLY_PAID`, `UNPAID`

### CustomerPayment
- `id`, `amount`, `receivedAmount`, `paymentStatus`
- `paymentDetails[]`: Array of payment method breakdowns

### CustomerLedger
- `transactionType`, `creditAmount`, `debitAmount`
- `runningBalance`, `paymentMode`, `transactionDate`

## 🛠️ Frontend Integration Priority

1. **Customer Dashboard** - Overview and summary
2. **Customer Accounts** - Account listing and details  
3. **Customer Payments** - Payment history and details
4. **Customer Ledger** - Transaction history and statement

## 📱 Mobile-First Considerations

- All APIs support pagination for performance
- Rich response objects minimize additional API calls
- Summary APIs provide aggregated data for quick overview
- Date range filtering for efficient data loading

## 🔄 Real-time Features (Future)

- Payment status updates via WebSocket
- Real-time balance calculations
- Instant notification for new payments
- Live ledger updates
