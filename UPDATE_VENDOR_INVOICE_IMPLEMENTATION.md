# Update Vendor Invoice API - Implementation Summary

## Overview
Successfully implemented the **Update Vendor Invoice API** with strict business rules ensuring only UNPAID invoices can be modified.

---

## API Endpoint

**Method**: `PUT`  
**Path**: `/api/vendorInvoice/update/{invoiceId}`  
**Authorization**: JWT Token Required

---

## Key Features Implemented

### 1. **Status Validation**
- ✅ Only invoices with status `UNPAID` can be updated
- ✅ Returns clear error message if invoice is `PARTIAL` or `PAID`
- ✅ Prevents data corruption on already-processed invoices

### 2. **Transactional Integrity**
- ✅ Full transaction rollback on any error
- ✅ Atomic operations - all changes succeed or all fail
- ✅ Database consistency maintained

### 3. **GRN Quantity Management**
- ✅ **Rollback Phase**: Reverts previous invoice quantities from GRN items
- ✅ **Validation Phase**: Validates new quantities against available GRN quantities
- ✅ **Update Phase**: Applies new quantities to GRN items
- ✅ Prevents over-invoicing beyond GRN received quantities

### 4. **Invoice Items Handling**
- ✅ Deletes all old invoice items
- ✅ Creates new invoice items from request
- ✅ Recalculates amounts based on quantity × rate
- ✅ Links items correctly to GRN items

### 5. **Audit Trail**
- ✅ Updates `updatedBy` with logged-in user
- ✅ Updates `updatedDate` timestamp
- ✅ Maintains creation audit fields

---

## Implementation Details

### Controller Method
```java
@PutMapping("/update/{invoiceId}")
public Map updateInvoice(
        @PathVariable long invoiceId,
        @RequestBody VendorInvoice invoiceRequest,
        HttpServletRequest request) {
    String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
    return vendorInvoiceService.updateInvoice(invoiceId, invoiceRequest, loggedInUser);
}
```

### Service Method Flow

1. **Fetch & Validate**
   - Fetch existing invoice by ID
   - Validate invoice exists
   - Validate status is UNPAID

2. **Rollback Previous Quantities**
   - Fetch all old invoice items
   - For each item: reduce GRN item's `quantityInvoiced`
   - Delete old invoice items

3. **Validate New Quantities**
   - Check each new invoice item against GRN received quantities
   - Calculate pending invoice quantity per GRN item
   - Prevent exceeding available quantities

4. **Update Invoice**
   - Update invoice header (number, amount, dates)
   - Recalculate pending amount
   - Create new invoice items
   - Update GRN item invoiced quantities

5. **Commit or Rollback**
   - Success: commit all changes
   - Error: rollback entire transaction

---

## Business Rules Enforced

| Rule | Implementation |
|------|----------------|
| Only UNPAID invoices can be updated | ✅ Status check at start of method |
| Cannot exceed GRN quantities | ✅ Validation against remaining GRN quantities |
| Must have at least one item | ✅ Empty list validation |
| Quantities must be positive | ✅ Quantity > 0 validation |
| GRN items must exist | ✅ Foreign key validation |
| Transaction atomicity | ✅ @Transactional with rollback |
| Audit trail | ✅ updatedBy, updatedDate tracking |

---

## Error Handling

### Error Scenarios Covered

1. **Invoice Not Found**
   ```json
   {
     "responseMessage": "Invoice not found with id: X",
     "responseCode": "9998"
   }
   ```

2. **Invoice Not UNPAID**
   ```json
   {
     "responseMessage": "Only UNPAID invoices can be updated. Current status: PAID",
     "responseCode": "9998"
   }
   ```

3. **Quantity Exceeds GRN**
   ```json
   {
     "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 5",
     "responseCode": "9998"
   }
   ```

4. **Invalid GRN Item**
   ```json
   {
     "responseMessage": "GRN Item not found with id: X",
     "responseCode": "9998"
   }
   ```

5. **System Errors**
   ```json
   {
     "responseMessage": "Error details...",
     "responseCode": "9999"
   }
   ```

---

## Testing CURL Commands

### 1. Get Invoice Details (Pre-Update)
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

### 2. Update Invoice (Status Must Be UNPAID)
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "invoiceNumber": "INV-20260301-001",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-01",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    },
    {
      "grnItemId": 2,
      "quantity": 50.0,
      "rate": 300.0
    }
  ]
}'
```

### 3. Verify Update
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

### 4. Test Error: Update PAID Invoice (Should Fail)
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "totalAmount": 70000.00,
  "invoiceItemList": [...]
}'
```

---

## Request Body Structure

### Required Fields
- `totalAmount` (Double) - Total invoice amount
- `invoiceItemList` (List) - Array of invoice items

### Optional Fields
- `invoiceNumber` (String) - Invoice number (keeps existing if not provided)
- `invoiceDate` (LocalDate) - Invoice date (keeps existing if not provided)
- `dueDate` (LocalDate) - Due date (keeps existing if not provided)

### Invoice Item Structure
```json
{
  "grnItemId": 1,      // Required: GRN Item reference
  "quantity": 100.0,   // Required: Must be > 0
  "rate": 500.0        // Required: Unit rate
}
```

---

## Related APIs for Update Flow

### Get All UNPAID Invoices
```bash
POST /api/vendorInvoice/{organizationId}/getByStatus/UNPAID
```

### Get Invoice by ID
```bash
GET /api/vendorInvoice/getById/{invoiceId}
```

### Get GRN Details (for available quantities)
```bash
GET /api/grn/getById/{grnId}
```

---

## Database Impact

### Tables Modified
1. **vendor_invoice**
   - Updates: invoiceNumber, totalAmount, pendingAmount, invoiceDate, dueDate, updatedBy, updatedDate

2. **vendor_invoice_item**
   - Deletes: All old items for the invoice
   - Inserts: New items from request

3. **grn_items**
   - Updates: quantityInvoiced (rollback old + apply new)

---

## Success Response

```json
{
  "data": null,
  "responseMessage": "Invoice updated successfully",
  "responseCode": "0000"
}
```

---

## Complete Documentation

For comprehensive CURL commands and usage examples, refer to:
📄 **`VENDOR_INVOICE_CURL_COMMANDS.md`**

---

## Files Modified

1. ✅ `VendorInvoiceController.java` - Added updateInvoice endpoint
2. ✅ `VendorInvoiceService.java` - Implemented updateInvoice method with full business logic
3. ✅ Created `VENDOR_INVOICE_CURL_COMMANDS.md` - Complete API documentation

---

## Summary

✅ **Update API Implemented**  
✅ **UNPAID-Only Restriction Enforced**  
✅ **Transaction Safety Guaranteed**  
✅ **GRN Quantity Integrity Maintained**  
✅ **Complete Error Handling**  
✅ **Comprehensive Documentation**  

The update vendor invoice API is production-ready and follows all existing patterns in the ERP system.
