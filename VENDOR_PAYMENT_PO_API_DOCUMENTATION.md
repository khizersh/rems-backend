# Vendor Payment (PO-based) API - Frontend Integration Guide

## Overview
This document provides complete API specifications for the **Vendor Payment PO** module in the Real Estate ERP system. This module manages payments made against vendor invoices that are created from GRNs (Goods Receipt Notes). It integrates with the Organization Account module to track payment sources and automatically updates invoice payment status.

**Module Flow**: `Purchase Order → GRN → Vendor Invoice → Vendor Payment`

---

## Base URL
```
http://localhost:8081/api/vendorPaymentPO
```

---

## Authentication
All endpoints require JWT authentication. Include the token in the request header:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## Common Response Wrapper
All API responses use the standard ResponseMapper format:

**Success Response:**
```json
{
  "data": { /* response data */ },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error Response:**
```json
{
  "data": null,
  "responseMessage": "Error description",
  "responseCode": "error_code"
}
```

---

## API Endpoints

### 1. Create Vendor Payment

**Endpoint:** `POST /api/vendorPaymentPO/create`

**Description:** Creates a new payment against a vendor invoice. Automatically:
- Validates payment amount against invoice pending amount
- Deducts amount from organization account balance
- Creates organization account detail entry (credit transaction)
- Updates invoice paid amount and status (UNPAID → PARTIALLY_PAID → PAID)
- Prevents over-payment

**Request Body:**
```json
{
  "invoiceId": 101,
  "amount": 50000.0,
  "organizationAccountId": 5,
  "paymentMode": "BANK_TRANSFER",
  "referenceNumber": "TXN-20260228-001",
  "paymentDate": "2026-02-28",
  "remarks": "First installment payment"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| invoiceId | Long | Yes | ID of the vendor invoice to pay against |
| amount | Double | Yes | Payment amount (must be > 0 and ≤ pending amount) |
| organizationAccountId | Long | Yes | Organization account ID to deduct from |
| paymentMode | String | No | Payment method: CASH, CHEQUE, BANK_TRANSFER, ONLINE, etc. |
| referenceNumber | String | No | Transaction/cheque reference number |
| paymentDate | LocalDate | No | Payment date (defaults to current date if not provided) |
| remarks | String | No | Additional payment notes |

**Success Response (200):**
```json
{
  "data": {
    "payment": {
      "id": 1001,
      "orgId": 1,
      "projectId": 20000,
      "vendorId": 3,
      "invoiceId": 101,
      "amount": 50000.0,
      "paymentMode": "BANK_TRANSFER",
      "referenceNumber": "TXN-20260228-001",
      "paymentDate": "2026-02-28",
      "remarks": "First installment payment",
      "organizationAccountId": 5,
      "createdBy": "user1",
      "updatedBy": "user1",
      "createdDate": "2026-02-28T10:30:00",
      "updatedDate": "2026-02-28T10:30:00"
    },
    "invoiceStatus": "PARTIALLY_PAID",
    "pendingAmount": 150000.0,
    "orgAccountBalance": 450000.0,
    "organizationAccountId": 5
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error Scenarios:**

| Error | Message | Code |
|-------|---------|------|
| Invoice not found | "Invoice not found" | 1001 |
| Already paid | "Invoice is already fully paid" | 1002 |
| Amount exceeds pending | "Payment amount exceeds pending amount" | 1003 |
| Invalid org account | "Organization account not found or does not belong to this organization" | 1004 |
| Insufficient balance | "Insufficient balance in organization account" | 1005 |
| Amount ≤ 0 | "Payment amount must be greater than 0" | 1006 |

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <JWT_TOKEN>' \
--data '{
  "invoiceId": 101,
  "amount": 50000.0,
  "organizationAccountId": 5,
  "paymentMode": "BANK_TRANSFER",
  "referenceNumber": "TXN-20260228-001",
  "paymentDate": "2026-02-28",
  "remarks": "First installment payment"
}'
```

---

### 2. Get Payment by ID

**Endpoint:** `GET /api/vendorPaymentPO/getById/{paymentId}`

**Description:** Retrieves complete details of a specific payment record.

**Path Parameter:**
- `paymentId` (Long) - Payment ID

**Success Response (200):**
```json
{
  "data": {
    "id": 1001,
    "orgId": 1,
    "projectId": 20000,
    "vendorId": 3,
    "invoiceId": 101,
    "amount": 50000.0,
    "paymentMode": "BANK_TRANSFER",
    "referenceNumber": "TXN-20260228-001",
    "paymentDate": "2026-02-28",
    "remarks": "First installment payment",
    "organizationAccountId": 5,
    "createdBy": "user1",
    "updatedBy": "user1",
    "createdDate": "2026-02-28T10:30:00",
    "updatedDate": "2026-02-28T10:30:00"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error Response (404):**
```json
{
  "data": null,
  "responseMessage": "Payment not found",
  "responseCode": "1001"
}
```

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/getById/1001' \
--header 'Authorization: Bearer <JWT_TOKEN>'
```

---

### 3. Get Payments by Invoice

**Endpoint:** `GET /api/vendorPaymentPO/getByInvoice/{invoiceId}`

**Description:** Retrieves all payments made against a specific invoice. Useful for displaying payment history on invoice detail page.

**Path Parameter:**
- `invoiceId` (Long) - Invoice ID

**Success Response (200):**
```json
{
  "data": [
    {
      "id": 1001,
      "orgId": 1,
      "projectId": 20000,
      "vendorId": 3,
      "invoiceId": 101,
      "amount": 50000.0,
      "paymentMode": "BANK_TRANSFER",
      "referenceNumber": "TXN-20260228-001",
      "paymentDate": "2026-02-28",
      "remarks": "First installment",
      "organizationAccountId": 5,
      "createdBy": "user1",
      "createdDate": "2026-02-28T10:30:00"
    },
    {
      "id": 1002,
      "orgId": 1,
      "projectId": 20000,
      "vendorId": 3,
      "invoiceId": 101,
      "amount": 50000.0,
      "paymentMode": "CHEQUE",
      "referenceNumber": "CHQ-789456",
      "paymentDate": "2026-03-15",
      "remarks": "Second installment",
      "organizationAccountId": 5,
      "createdBy": "user2",
      "createdDate": "2026-03-15T14:20:00"
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Empty Result:**
```json
{
  "data": [],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/getByInvoice/101' \
--header 'Authorization: Bearer <JWT_TOKEN>'
```

---

### 4. Get Payments by Vendor (Paginated)

**Endpoint:** `POST /api/vendorPaymentPO/getByVendor/{vendorId}`

**Description:** Retrieves all payments for a specific vendor with pagination. Useful for vendor payment history page.

**Path Parameter:**
- `vendorId` (Long) - Vendor ID

**Request Body:**
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "paymentDate",
  "sortDir": "desc"
}
```

**Request Fields:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| page | Integer | Yes | 0 | Page number (0-based) |
| size | Integer | Yes | 10 | Items per page |
| sortBy | String | Yes | "createdDate" | Field to sort by (paymentDate, amount, createdDate) |
| sortDir | String | Yes | "desc" | Sort direction: "asc" or "desc" |

**Success Response (200):**
```json
{
  "data": {
    "content": [
      {
        "id": 1001,
        "orgId": 1,
        "projectId": 20000,
        "vendorId": 3,
        "invoiceId": 101,
        "amount": 50000.0,
        "paymentMode": "BANK_TRANSFER",
        "referenceNumber": "TXN-20260228-001",
        "paymentDate": "2026-02-28",
        "remarks": "First installment",
        "organizationAccountId": 5,
        "createdBy": "user1",
        "createdDate": "2026-02-28T10:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      }
    },
    "totalElements": 25,
    "totalPages": 3,
    "last": false,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 10,
    "empty": false
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/getByVendor/3' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <JWT_TOKEN>' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "paymentDate",
  "sortDir": "desc"
}'
```

---

### 5. Get All Payments by Organization (Paginated)

**Endpoint:** `POST /api/vendorPaymentPO/{organizationId}/getAll`

**Description:** Retrieves all vendor payments for an organization with pagination. Main listing page for payment management.

**Path Parameter:**
- `organizationId` (Long) - Organization ID

**Request Body:**
```json
{
  "page": 0,
  "size": 20,
  "sortBy": "paymentDate",
  "sortDir": "desc"
}
```

**Success Response (200):**
```json
{
  "data": {
    "content": [
      {
        "id": 1001,
        "orgId": 1,
        "projectId": 20000,
        "vendorId": 3,
        "invoiceId": 101,
        "amount": 50000.0,
        "paymentMode": "BANK_TRANSFER",
        "referenceNumber": "TXN-20260228-001",
        "paymentDate": "2026-02-28",
        "remarks": "First installment",
        "organizationAccountId": 5,
        "createdBy": "user1",
        "createdDate": "2026-02-28T10:30:00"
      },
      {
        "id": 1002,
        "orgId": 1,
        "projectId": 20001,
        "vendorId": 5,
        "invoiceId": 102,
        "amount": 75000.0,
        "paymentMode": "CHEQUE",
        "referenceNumber": "CHQ-456123",
        "paymentDate": "2026-02-25",
        "remarks": "Payment for steel supplies",
        "organizationAccountId": 6,
        "createdBy": "user2",
        "createdDate": "2026-02-25T16:45:00"
      }
    ],
    "pageable": { /* pagination metadata */ },
    "totalElements": 150,
    "totalPages": 8,
    "last": false,
    "size": 20,
    "number": 0
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/1/getAll' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <JWT_TOKEN>' \
--data '{
  "page": 0,
  "size": 20,
  "sortBy": "paymentDate",
  "sortDir": "desc"
}'
```

---

### 6. Get Total Paid Amount for Invoice

**Endpoint:** `GET /api/vendorPaymentPO/getTotalPaid/{invoiceId}`

**Description:** Retrieves payment summary for a specific invoice including total amount, paid amount, pending amount, and current status. Useful for dashboard cards and invoice summary displays.

**Path Parameter:**
- `invoiceId` (Long) - Invoice ID

**Success Response (200):**
```json
{
  "data": {
    "invoiceId": 101,
    "totalAmount": 200000.0,
    "paidAmount": 100000.0,
    "pendingAmount": 100000.0,
    "status": "PARTIALLY_PAID"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Status Values:**
- `UNPAID` - No payments made yet
- `PARTIALLY_PAID` - Some payments made, amount pending
- `PAID` - Fully paid (paidAmount = totalAmount)

**Error Response (404):**
```json
{
  "data": null,
  "responseMessage": "Invoice not found",
  "responseCode": "1001"
}
```

**cURL Example:**
```bash
curl --location 'http://localhost:8081/api/vendorPaymentPO/getTotalPaid/101' \
--header 'Authorization: Bearer <JWT_TOKEN>'
```

---

### 7. Update Vendor Payment

**Endpoint:** `PUT /api/vendorPaymentPO/update/{paymentId}`

**Description:** Updates an existing payment record. Automatically:
- Reverts previous payment amount from invoice
- Refunds previous amount to organization account
- Applies new payment amount to invoice
- Deducts new amount from organization account
- Recalculates invoice status based on new paid amount
- Creates organization account detail entries for both revert and new payment

**Path Parameter:**
- `paymentId` (Long) - Payment ID to update

**Request Body:**
```json
{
  "amount": 75000.0,
  "organizationAccountId": 5,
  "paymentMode": "BANK_TRANSFER",
  "referenceNumber": "TXN-20260228-UPDATED",
  "paymentDate": "2026-02-28",
  "remarks": "Updated payment amount"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | Double | Yes | New payment amount (must be > 0 and ≤ invoice pending + old payment) |
| organizationAccountId | Long | Yes | Organization account ID |
| paymentMode | String | No | Updated payment method |
| referenceNumber | String | No | Updated reference number |
| paymentDate | LocalDate | No | Updated payment date |
| remarks | String | No | Updated remarks |

**Success Response (200):**
```json
{
  "data": {
    "payment": {
      "id": 1001,
      "orgId": 1,
      "projectId": 20000,
      "vendorId": 3,
      "invoiceId": 101,
      "amount": 75000.0,
      "paymentMode": "BANK_TRANSFER",
      "referenceNumber": "TXN-20260228-UPDATED",
      "paymentDate": "2026-02-28",
      "remarks": "Updated payment amount",
      "organizationAccountId": 5,
      "createdBy": "user1",
      "updatedBy": "user2",
      "createdDate": "2026-02-28T10:30:00",
      "updatedDate": "2026-03-03T15:20:00"
    },
    "invoiceStatus": "PARTIALLY_PAID",
    "pendingAmount": 125000.0,
    "orgAccountBalance": 425000.0
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error Scenarios:**

| Error | Message | Code |
|-------|---------|------|
| Payment not found | "Payment not found" | 1001 |
| Amount exceeds limit | "Payment amount exceeds allowed limit for this invoice" | 1002 |
| Insufficient balance | "Insufficient balance in organization account" | 1003 |
| Amount ≤ 0 | "Payment amount must be greater than 0" | 1004 |

**cURL Example:**
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorPaymentPO/update/1001' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <JWT_TOKEN>' \
--data '{
  "amount": 75000.0,
  "organizationAccountId": 5,
  "paymentMode": "BANK_TRANSFER",
  "referenceNumber": "TXN-20260228-UPDATED",
  "paymentDate": "2026-02-28",
  "remarks": "Updated payment amount"
}'
```

---

## Data Models

### VendorPaymentPO Entity

```typescript
interface VendorPaymentPO {
  id: number;
  orgId: number;
  projectId: number;
  vendorId: number;
  invoiceId: number;
  amount: number;
  paymentMode: string;          // CASH, CHEQUE, BANK_TRANSFER, ONLINE
  referenceNumber: string;
  paymentDate: string;          // LocalDate format: YYYY-MM-DD
  remarks: string;
  organizationAccountId: number;
  createdBy: string;
  updatedBy: string;
  createdDate: string;          // ISO DateTime
  updatedDate: string;          // ISO DateTime
}
```

### Payment Mode Options
```typescript
const PAYMENT_MODES = [
  'CASH',
  'CHEQUE',
  'BANK_TRANSFER',
  'ONLINE',
  'UPI',
  'CARD',
  'PAY_ORDER'
];
```

---

## Frontend Integration Guidelines

### 1. Payment Creation Flow

**Recommended UI Flow:**
1. User opens invoice detail page
2. Display invoice summary (total, paid, pending)
3. Show "Make Payment" button if status is UNPAID or PARTIALLY_PAID
4. Open payment form modal/page
5. Pre-fill invoice details and display pending amount
6. User enters payment details
7. Validate amount ≤ pending amount client-side
8. Call create payment API
9. On success, refresh invoice details and show payment confirmation
10. Update invoice status badge

**Sample React Component Logic:**
```typescript
const makePayment = async (paymentData) => {
  try {
    // Client-side validation
    if (paymentData.amount <= 0) {
      showError("Payment amount must be greater than 0");
      return;
    }
    
    if (paymentData.amount > invoice.pendingAmount) {
      showError("Payment amount exceeds pending amount");
      return;
    }

    // API call
    const response = await fetch('/api/vendorPaymentPO/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(paymentData)
    });

    const result = await response.json();
    
    if (result.responseCode === '0000') {
      showSuccess('Payment created successfully');
      // Update invoice status in UI
      setInvoiceStatus(result.data.invoiceStatus);
      setPendingAmount(result.data.pendingAmount);
      // Refresh payment list
      fetchPaymentHistory(invoice.id);
    } else {
      showError(result.responseMessage);
    }
  } catch (error) {
    showError('Failed to create payment');
  }
};
```

### 2. Payment History Display

**Recommended UI Components:**
- Data table with columns: Payment Date, Amount, Payment Mode, Reference, Created By, Actions
- Filter by date range
- Export to Excel/PDF
- Sum total paid in footer
- Edit/Delete actions (if allowed)

**Sample API Integration:**
```typescript
const fetchPaymentHistory = async (invoiceId) => {
  const response = await fetch(`/api/vendorPaymentPO/getByInvoice/${invoiceId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const result = await response.json();
  
  if (result.responseCode === '0000') {
    setPayments(result.data);
    // Calculate total
    const total = result.data.reduce((sum, p) => sum + p.amount, 0);
    setTotalPaid(total);
  }
};
```

### 3. Payment Summary Card

**Display on Invoice Detail Page:**
```typescript
const PaymentSummaryCard = ({ invoiceId }) => {
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    fetch(`/api/vendorPaymentPO/getTotalPaid/${invoiceId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => res.json())
    .then(result => {
      if (result.responseCode === '0000') {
        setSummary(result.data);
      }
    });
  }, [invoiceId]);

  if (!summary) return <Loading />;

  return (
    <Card>
      <h3>Payment Summary</h3>
      <div>Total Amount: ₹{summary.totalAmount.toLocaleString()}</div>
      <div>Paid Amount: ₹{summary.paidAmount.toLocaleString()}</div>
      <div>Pending Amount: ₹{summary.pendingAmount.toLocaleString()}</div>
      <Badge status={summary.status}>{summary.status}</Badge>
    </Card>
  );
};
```

### 4. Organization Account Selection

**Important:** Always fetch and display available organization accounts with sufficient balance before payment creation.

**Recommended Flow:**
1. Fetch organization accounts for the organization
2. Filter accounts with balance ≥ payment amount
3. Display dropdown/select with account name and balance
4. Show warning if no accounts have sufficient balance

### 5. Payment Update Flow

**Use Cases:**
- Correcting payment amount entry errors
- Changing payment mode/reference
- Updating payment date

**Important Notes:**
- Update functionality performs full revert and reapply
- Ensure user has permission to update payments
- Show confirmation dialog before update
- Display old and new values for user verification

---

## Error Handling Best Practices

### 1. Display User-Friendly Messages
```typescript
const getErrorMessage = (responseMessage) => {
  const errorMap = {
    "Invoice not found": "The invoice you're trying to pay doesn't exist",
    "Invoice is already fully paid": "This invoice has been fully paid already",
    "Payment amount exceeds pending amount": "Payment amount is more than the pending balance",
    "Insufficient balance": "The selected account doesn't have enough balance"
  };
  
  return errorMap[responseMessage] || responseMessage;
};
```

### 2. Handle Network Errors
```typescript
try {
  const response = await fetch(url, options);
  const result = await response.json();
  
  if (result.responseCode === '0000') {
    // Success
  } else {
    showError(getErrorMessage(result.responseMessage));
  }
} catch (error) {
  showError('Network error. Please check your connection and try again.');
}
```

### 3. Form Validation
- Amount must be > 0
- Amount must be ≤ invoice pending amount
- Payment date cannot be future date
- Organization account must be selected
- Payment mode should be selected from dropdown

---

## Testing Checklist

### Functional Testing
- [ ] Create payment with valid data
- [ ] Create payment with amount > pending amount (should fail)
- [ ] Create payment with amount = 0 (should fail)
- [ ] Create payment without organization account (should fail)
- [ ] Create payment with insufficient balance (should fail)
- [ ] Get payment by ID
- [ ] Get payments by invoice (verify all payments returned)
- [ ] Get payments by vendor with pagination
- [ ] Get all payments by organization with pagination
- [ ] Get total paid summary
- [ ] Update payment with valid data
- [ ] Update payment with amount > allowed limit (should fail)
- [ ] Verify invoice status changes correctly (UNPAID → PARTIALLY_PAID → PAID)
- [ ] Verify organization account balance updates correctly
- [ ] Verify organization account detail entries are created

### UI Testing
- [ ] Payment form displays correctly
- [ ] Validation messages appear for invalid inputs
- [ ] Organization account dropdown shows accounts with balances
- [ ] Payment history table displays correctly
- [ ] Pagination works in payment list
- [ ] Payment summary card shows correct amounts
- [ ] Invoice status badge updates after payment
- [ ] Edit payment modal pre-fills existing data
- [ ] Success/error notifications appear

### Edge Cases
- [ ] Making payment equal to exact pending amount
- [ ] Making multiple small payments to fully pay invoice
- [ ] Updating payment amount from smaller to larger
- [ ] Updating payment amount from larger to smaller
- [ ] Creating payment for invoice with single GRN
- [ ] Creating payment for invoice with multiple GRNs

---

## Quick Reference - Status Flow

### Invoice Status Progression
```
UNPAID (initial)
   ↓ (partial payment)
PARTIALLY_PAID
   ↓ (remaining payment)
PAID (final)
```

### Payment Flow Diagram
```
User selects Invoice
    ↓
Check pending amount > 0
    ↓
User enters payment details
    ↓
Select Organization Account
    ↓
Validate balance >= payment amount
    ↓
Create Payment
    ↓
System deducts from Org Account
    ↓
System updates Invoice paid amount
    ↓
System recalculates Invoice status
    ↓
Payment confirmation displayed
```

---

## Support & Contact

For API issues or clarifications, contact the backend development team.

**Document Version:** 1.0  
**Last Updated:** March 3, 2026  
**Module:** Vendor Payment (PO-based)  
**Backend Controller:** `VendorPaymentPOController.java`
