# 📄 Post-Dated Cheque (PDC) Payment Flow — Frontend Integration Guide

> **Version:** 1.0  
> **Last Updated:** March 30, 2026  
> **Module:** Expense & Vendor Payments  
> **Base URL:** `/api`

---

## 📌 Overview

The PDC module introduces support for **Post-Dated Cheques** in the Expense and Vendor payment system. When a payment is made via PDC, no immediate balance deduction occurs. The cheque is tracked with a `PENDING` status until it is manually **cleared** or **marked as failed**.

### Payment Mode Flow Summary

| Payment Mode | Balance Deducted Immediately? | Vendor Payable Reduced? | Journal Entry Created? |
|:-------------|:------------------------------|:------------------------|:-----------------------|
| `CASH`       | ✅ Yes                         | ✅ Yes                   | ✅ Yes                  |
| `CREDIT`     | ✅ Yes (partial)               | ✅ Yes (payable created)  | ✅ Yes                  |
| `PDC`        | ❌ No                          | ❌ No (until cleared)     | ❌ No (until cleared)   |

---

## 🔄 New Enums

### `PaymentMode`
```
CASH | CREDIT | PDC
```

### `PdcStatus`
```
PENDING | CLEARED | FAILED
```

---

## 📑 API Reference

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

---

### 1️⃣ Create Expense with PDC Payment

**Endpoint:** `POST /api/expense/addExpense`  
**Content-Type:** `application/json`

This is the **existing** expense creation endpoint — now enhanced to accept PDC fields.

#### Request Body

```json
{
  "expenseType": "CONSTRUCTION",
  "amountPaid": 50000,
  "creditAmount": 0,
  "totalAmount": 50000,
  "organizationId": 1,
  "organizationAccountId": 3,
  "vendorAccountId": 5,
  "projectId": 2,
  "expenseTypeId": 1,
  "comments": "Cement purchase via post-dated cheque",

  "paymentType": "CHEQUE",

  "paymentMode": "PDC",
  "chequeNumber": "CHQ-2026-00451",
  "chequeDate": "2026-04-15",
  "bankName": "HBL Bank"
}
```

#### New/PDC-Specific Fields

| Field           | Type     | Required for PDC | Description                                    |
|:----------------|:---------|:-----------------|:-----------------------------------------------|
| `paymentMode`   | `string` | ✅ Yes            | Must be `"PDC"` for post-dated cheque flow     |
| `chequeNumber`  | `string` | ✅ Yes            | Unique cheque number                           |
| `chequeDate`    | `string` | ✅ Yes            | ISO date (`YYYY-MM-DD`). Must be today or future |
| `bankName`      | `string` | Optional          | Bank name for the cheque                       |

#### Validations (PDC Mode)
- `chequeNumber` → required, non-blank  
- `chequeDate` → required, must be ≥ today  
- `totalAmount` or `amountPaid` → must be > 0  
- Balance is **NOT** validated (no insufficient funds error)

#### Success Response

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "id": 142,
    "amountPaid": 50000,
    "creditAmount": 0,
    "totalAmount": 50000,
    "vendorAccountId": 5,
    "organizationAccountId": 3,
    "organizationId": 1,
    "projectId": 2,
    "projectName": "DHA Phase 6",
    "vendorName": "Ali Cement Works",
    "expenseTitle": "Cement",
    "expenseType": "CONSTRUCTION",
    "paymentStatus": "PENDING",
    "paymentMode": "PDC",
    "pdcStatus": "PENDING",
    "chequeNumber": "CHQ-2026-00451",
    "chequeDate": "2026-04-15",
    "bankName": "HBL Bank",
    "orgAccountTitle": "HBL Current Account",
    "createdBy": "admin@company.com",
    "updatedBy": "admin@company.com",
    "createdDate": "2026-03-30T14:22:10",
    "updatedDate": "2026-03-30T14:22:10"
  }
}
```

> **Note:** For `CASH` and `CREDIT` modes, the existing flow is completely unchanged. Simply omit `paymentMode` or send `"CASH"` / `"CREDIT"`.

---

### 2️⃣ Process (Clear) PDC Payment

**Endpoint:** `POST /api/payments/process-pdc/{id}`  
**Description:** Clears a pending PDC. Validates balance → deducts from org account → updates status → creates ledger entries.

#### Path Parameters

| Param | Type   | Description                |
|:------|:-------|:---------------------------|
| `id`  | `long` | Expense ID of the PDC record |

#### Request Body
None required.

#### Success Response

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "id": 142,
    "amountPaid": 50000,
    "totalAmount": 50000,
    "paymentStatus": "PAID",
    "paymentMode": "PDC",
    "pdcStatus": "CLEARED",
    "chequeNumber": "CHQ-2026-00451",
    "chequeDate": "2026-04-15",
    "bankName": "HBL Bank",
    "vendorName": "Ali Cement Works",
    "expenseTitle": "Cement",
    "orgAccountTitle": "HBL Current Account"
  }
}
```

#### Error Responses

| Scenario                        | Response Code | Message                                                       |
|:--------------------------------|:-------------|:--------------------------------------------------------------|
| Not a PDC transaction           | `0002`       | `"This transaction is not a PDC payment"`                     |
| Already cleared                 | `0002`       | `"This PDC has already been cleared"`                         |
| Already failed                  | `0002`       | `"This PDC has been marked as failed. Create a new expense."` |
| Insufficient balance            | `0002`       | `"Insufficient balance in account: {name}. Available: X, Required: Y"` |
| PDC not found                   | `0002`       | `"PDC transaction not found"`                                 |

#### What happens on clearance:
1. Organization account balance is deducted  
2. `pdcStatus` → `CLEARED`, `paymentStatus` → `PAID`  
3. Expense detail record is created  
4. Vendor payable is reduced (for Construction expenses)  
5. Vendor payment history record is created  
6. Journal entry created (Dr Expense/Payable, Cr Bank)

---

### 3️⃣ Mark PDC as Failed

**Endpoint:** `POST /api/payments/mark-failed/{id}`  
**Description:** Marks a pending PDC as failed. **No balance impact.**

#### Path Parameters

| Param | Type   | Description                |
|:------|:-------|:---------------------------|
| `id`  | `long` | Expense ID of the PDC record |

#### Request Body
None required.

#### Success Response

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "id": 142,
    "paymentStatus": "UNPAID",
    "paymentMode": "PDC",
    "pdcStatus": "FAILED",
    "chequeNumber": "CHQ-2026-00451",
    "chequeDate": "2026-04-15"
  }
}
```

#### Error Responses

| Scenario                | Response Code | Message                                     |
|:------------------------|:-------------|:---------------------------------------------|
| Already cleared         | `0002`       | `"Cannot mark a cleared PDC as failed"`      |
| Already failed          | `0002`       | `"This PDC is already marked as failed"`     |

---

### 4️⃣ List PDC Payments (with filters)

**Endpoint:** `POST /api/payments/pdc/list`  
**Description:** Fetch paginated list of PDC payments with scheduling filters.

#### Request Body

```json
{
  "organizationId": 1,
  "filter": "dueToday",
  "vendorAccountId": null,
  "projectId": null,
  "page": 0,
  "size": 10,
  "sortBy": "chequeDate",
  "sortDir": "asc"
}
```

#### Filter Options

| Filter Value  | Description                                          |
|:-------------|:-----------------------------------------------------|
| `"dueToday"` | Cheque date == today AND status = PENDING            |
| `"overdue"`  | Cheque date < today AND status = PENDING             |
| `"upcoming"` | Cheque date > today AND status = PENDING             |
| `"all"`      | All PENDING PDCs (default). Supports vendor/project filter |

#### Optional Filters (with `"all"`)

| Field             | Type   | Description                              |
|:------------------|:-------|:-----------------------------------------|
| `vendorAccountId` | `Long` | Filter by specific vendor                |
| `projectId`       | `Long` | Filter by specific project               |

#### Success Response

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "content": [
      {
        "id": 142,
        "amountPaid": 50000,
        "totalAmount": 50000,
        "vendorName": "Ali Cement Works",
        "projectName": "DHA Phase 6",
        "expenseTitle": "Cement",
        "paymentMode": "PDC",
        "pdcStatus": "PENDING",
        "chequeNumber": "CHQ-2026-00451",
        "chequeDate": "2026-04-15",
        "bankName": "HBL Bank",
        "createdDate": "2026-03-30T14:22:10"
      }
    ],
    "totalPages": 1,
    "totalElements": 1,
    "size": 10,
    "number": 0
  }
}
```

---

### 5️⃣ Get PDC by ID

**Endpoint:** `GET /api/payments/pdc/{id}`  
**Description:** Fetch a single PDC payment record by expense ID.

#### Success Response

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "id": 142,
    "amountPaid": 50000,
    "creditAmount": 0,
    "totalAmount": 50000,
    "vendorAccountId": 5,
    "organizationAccountId": 3,
    "organizationId": 1,
    "projectId": 2,
    "projectName": "DHA Phase 6",
    "vendorName": "Ali Cement Works",
    "expenseTitle": "Cement",
    "expenseType": "CONSTRUCTION",
    "paymentStatus": "PENDING",
    "paymentMode": "PDC",
    "pdcStatus": "PENDING",
    "chequeNumber": "CHQ-2026-00451",
    "chequeDate": "2026-04-15",
    "bankName": "HBL Bank",
    "orgAccountTitle": "HBL Current Account",
    "createdBy": "admin@company.com",
    "createdDate": "2026-03-30T14:22:10"
  }
}
```

---

## 🔐 Standard Response Format

All responses follow this structure:

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": { ... }
}
```

| Response Code | Meaning             |
|:-------------|:---------------------|
| `0000`       | Success              |
| `0001`       | No Data Found        |
| `0002`       | Invalid Parameter    |
| `0003`       | Invalid Credentials  |
| `9999`       | System Failure       |

---

## 🎨 Frontend UI Recommendations

### Expense Form Enhancement
- Add a **Payment Mode** dropdown: `CASH` | `CREDIT` | `PDC`
- When `PDC` is selected, show additional fields:
  - **Cheque Number** (text input, required)
  - **Cheque Date** (date picker, must be ≥ today, required)
  - **Bank Name** (text input, optional)
- Hide balance validation messages when PDC is selected

### PDC Dashboard / List Page
- Create a dedicated **PDC Payments** page or tab
- Show filter tabs: `Due Today` | `Overdue` | `Upcoming` | `All`
- **Overdue** tab should show a red indicator/badge
- Each PDC row should have action buttons:
  - ✅ **Process** → calls `POST /api/payments/process-pdc/{id}`
  - ❌ **Mark Failed** → calls `POST /api/payments/mark-failed/{id}`
  - 👁️ **View** → calls `GET /api/payments/pdc/{id}`

### Status Badges
| PDC Status | Suggested Color | Icon   |
|:-----------|:----------------|:-------|
| `PENDING`  | 🟡 Yellow/Amber  | ⏳ Clock |
| `CLEARED`  | 🟢 Green         | ✅ Check |
| `FAILED`   | 🔴 Red           | ❌ Cross |

### Backward Compatibility
- Existing expense forms continue to work **without any changes**
- If `paymentMode` is not sent, the system defaults to `CASH` or `CREDIT` based on existing logic
- All existing CASH/CREDIT expense list pages remain unaffected

---

## 📊 Data Model Changes (Expense Table)

| New Column       | Type          | Nullable | Description                        |
|:-----------------|:-------------|:---------|:-----------------------------------|
| `payment_mode`   | `VARCHAR(10)` | ✅ Yes    | `CASH`, `CREDIT`, or `PDC`        |
| `pdc_status`     | `VARCHAR(10)` | ✅ Yes    | `PENDING`, `CLEARED`, or `FAILED` |
| `cheque_number`  | `VARCHAR(255)`| ✅ Yes    | Cheque number                      |
| `cheque_date`    | `DATE`        | ✅ Yes    | Post-dated cheque date             |
| `bank_name`      | `VARCHAR(255)`| ✅ Yes    | Issuing bank name                  |

> All new columns are **nullable** to maintain backward compatibility with existing data.

---

## ⚡ cURL Examples

### Create PDC Expense
```bash
curl -X POST http://localhost:8080/api/expense/addExpense \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "expenseType": "CONSTRUCTION",
    "amountPaid": 50000,
    "creditAmount": 0,
    "totalAmount": 50000,
    "organizationId": 1,
    "organizationAccountId": 3,
    "vendorAccountId": 5,
    "projectId": 2,
    "expenseTypeId": 1,
    "paymentType": "CHEQUE",
    "paymentMode": "PDC",
    "chequeNumber": "CHQ-2026-00451",
    "chequeDate": "2026-04-15",
    "bankName": "HBL Bank",
    "comments": "Cement purchase via PDC"
  }'
```

### Process PDC
```bash
curl -X POST http://localhost:8080/api/payments/process-pdc/142 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Mark PDC Failed
```bash
curl -X POST http://localhost:8080/api/payments/mark-failed/142 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### List PDC Payments
```bash
curl -X POST http://localhost:8080/api/payments/pdc/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "organizationId": 1,
    "filter": "overdue",
    "page": 0,
    "size": 10,
    "sortBy": "chequeDate",
    "sortDir": "asc"
  }'
```

### Get PDC by ID
```bash
curl -X GET http://localhost:8080/api/payments/pdc/142 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

*This document is auto-generated from the backend implementation. For questions, refer to the source code or contact the backend team.*
