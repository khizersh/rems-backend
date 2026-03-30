# Vendor Invoice API - CURL Commands

This document contains CURL commands for all Vendor Invoice APIs.

**Base URL**: `http://localhost:8081`

**Note**: Replace the `Authorization` token with a valid JWT token from your login response.

---

## 1. Create Vendor Invoice

Creates a new vendor invoice against a GRN (Goods Receipt Note).

**Endpoint**: `POST /api/vendorInvoice/create`

**Authorization**: Required (JWT Token)

**Request Body**:
```json
{
  "grnId": 1,
  "invoiceNumber": "INV-001",
  "totalAmount": 50000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-03-31",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
      "rate": 500.0
    }
  ]
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "grnId": 1,
  "invoiceNumber": "INV-001",
  "totalAmount": 50000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-03-31",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
      "rate": 500.0
    }
  ]
}'
```

---

## 2. Get Invoice by ID

Retrieves a single invoice by its ID with all details including items.

**Endpoint**: `GET /api/vendorInvoice/getById/{invoiceId}`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `invoiceId`: Invoice ID (Long)

**CURL Command**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

---

## 3. Get All Invoices by Organization (Paginated)

Retrieves all vendor invoices for a specific organization with pagination.

**Endpoint**: `POST /api/vendorInvoice/{organizationId}/getAll`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `organizationId`: Organization ID (Long)

**Request Body**:
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getAll' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## 4. Get Invoices by Vendor (Paginated)

Retrieves all invoices for a specific vendor with pagination.

**Endpoint**: `POST /api/vendorInvoice/getByVendor/{vendorId}`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `vendorId`: Vendor ID (Long)

**Request Body**:
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

**CURL Command**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getByVendor/3' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## 5. Get Invoices by Status (Paginated)

Retrieves all invoices for an organization filtered by invoice status.

**Endpoint**: `POST /api/vendorInvoice/{organizationId}/getByStatus/{status}`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `organizationId`: Organization ID (Long)
- `status`: Invoice Status (String) - Values: `UNPAID`, `PARTIAL`, `PAID`

**Request Body**:
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

**CURL Commands**:

### Get UNPAID Invoices:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getByStatus/UNPAID' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get PARTIAL Invoices:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getByStatus/PARTIAL' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get PAID Invoices:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getByStatus/PAID' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## 6. Update Vendor Invoice ⚠️ (Only UNPAID Invoices)

Updates an existing vendor invoice. **Important**: Only invoices with status `UNPAID` can be updated.

**Endpoint**: `PUT /api/vendorInvoice/update/{invoiceId}`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `invoiceId`: Invoice ID to update (Long)

**Request Body**:
```json
{
  "invoiceNumber": "INV-001-UPDATED",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-01",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}
```

**CURL Command**:
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "invoiceNumber": "INV-001-UPDATED",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-01",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}'
```

**Update Business Rules**:
1. ✅ Only `UNPAID` invoices can be updated
2. ✅ System automatically rolls back previous invoice quantities from GRN items
3. ✅ Validates new quantities against available GRN quantities
4. ✅ Deletes old invoice items and creates new ones
5. ✅ Updates GRN item invoiced quantities
6. ✅ Prevents exceeding GRN received quantities
7. ✅ Transaction rollback on any validation failure

**Error Cases**:
- Invoice not found → Error response
- Invoice status is not UNPAID → Error: "Only UNPAID invoices can be updated"
- Invoice quantity exceeds available GRN quantity → Error with details
- Invalid GRN Item ID → Error response

---

## 7. Get Pending Amount by Vendor

Retrieves the total pending payment amount for a specific vendor across all invoices.

**Endpoint**: `GET /api/vendorInvoice/getPendingAmount/{vendorId}`

**Authorization**: Required (JWT Token)

**Path Parameters**:
- `vendorId`: Vendor ID (Long)

**CURL Command**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getPendingAmount/3' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

**Response Example**:
```json
{
  "data": {
    "vendorId": 3,
    "pendingAmount": 150000.00
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## Common Response Format

All APIs return responses in the following standard format:

### Success Response:
```json
{
  "data": { ... },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

### Error Response:
```json
{
  "data": null,
  "responseMessage": "Error message here",
  "responseCode": "Error code"
}
```

---

## Invoice Status Enum Values

- `UNPAID` - No payment received yet (can be updated)
- `PARTIAL` - Partial payment received (cannot be updated)
- `PAID` - Fully paid (cannot be updated)

---

## APIs Used During Update Invoice Process

When updating a vendor invoice, you may need data from these supporting APIs:

### 1. Get Invoice Details (Before Update)
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

This API returns:
- Current invoice details
- Invoice items with item names
- Project name, vendor name, PO number, GRN number
- Current status (to verify it's UNPAID)

### 2. Get GRN Details (To Check Available Quantities)
```bash
curl --location 'http://localhost:8081/api/grn/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

This API returns:
- GRN items with quantities received
- Already invoiced quantities per item
- Remaining quantities available for invoicing

### 3. Get GRN Items (For Item Selection)

Use the GRN details API above to get all items in a GRN with their available quantities.

---

## Complete Update Flow

1. **Fetch Invoice Details**:
   ```bash
   GET /api/vendorInvoice/getById/{invoiceId}
   ```
   - Verify status is UNPAID
   - Get current invoice items and amounts

2. **Fetch GRN Details** (if needed):
   ```bash
   GET /api/grn/getById/{grnId}
   ```
   - View available items and quantities
   - Check remaining quantities per item

3. **Update Invoice**:
   ```bash
   PUT /api/vendorInvoice/update/{invoiceId}
   ```
   - Send updated invoice data
   - System validates and updates

4. **Verify Update**:
   ```bash
   GET /api/vendorInvoice/getById/{invoiceId}
   ```
   - Confirm changes applied correctly

---

## Notes

1. **Authentication**: All APIs require a valid JWT token in the Authorization header
2. **Pagination**: Default page size is 10, page starts from 0
3. **Date Format**: Use ISO date format (YYYY-MM-DD) for all date fields
4. **Update Restriction**: Only UNPAID invoices can be updated. Once payment is recorded (PARTIAL or PAID status), the invoice becomes read-only
5. **Automatic Invoice Number**: If not provided during creation, system generates invoice number in format: `INV-YYYYMMDD-XXX`
6. **Quantity Validation**: System prevents invoicing more quantity than received in GRN
7. **Transaction Safety**: All operations are transactional - either all changes succeed or all rollback

---

## Testing Sequence

### Scenario 1: Create and Update UNPAID Invoice

1. Create Invoice:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "grnId": 1,
  "totalAmount": 50000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-03-31",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
      "rate": 500.0
    }
  ]
}'
```

2. Get Invoice (note the ID from response):
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

3. Update Invoice (only if status is UNPAID):
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-01",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}'
```

4. Verify Update:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

### Scenario 2: Attempt to Update PAID Invoice (Should Fail)

1. Try updating an invoice with PAID status (should return error):
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
  "totalAmount": 60000.00,
  "invoiceItemList": [...]
}'
```

Expected Error Response:
```json
{
  "data": null,
  "responseMessage": "Only UNPAID invoices can be updated. Current status: PAID",
  "responseCode": "9998"
}
```

---

## End of Document
