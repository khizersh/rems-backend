# GRN Invoice Status API - Before & After Comparison

## Quick Reference: What Changed

### OLD API (Boolean Flag)
```json
{
  "invoiceCreated": true   // ❌ Only knows "yes" or "no"
}
```

### NEW API (Enum Status)
```json
{
  "invoiceStatus": "PARTIALLY_INVOICED"  // ✅ Knows exact state
}
```

---

## API Request Changes

### Filter GRNs by Invoice Status

#### ❌ OLD Request (No longer works)
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceCreated": true,    // ❌ REMOVED
  "page": 0,
  "size": 10
}'
```

#### ✅ NEW Request - Get Not Invoiced GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "NOT_INVOICED",  // ✅ NEW
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

#### ✅ NEW Request - Get Partially Invoiced GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ NEW
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

#### ✅ NEW Request - Get Fully Invoiced GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "FULLY_INVOICED",  // ✅ NEW
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

#### ✅ NEW Request - Get All GRNs (No Filter)
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": null,  // ✅ NEW (null = all)
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## Response Changes

### Old GRN Response
```json
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "status": "RECEIVED",
    "invoiceCreated": true,  // ❌ Boolean - no detail
    "grnItemsList": [
      {
        "id": 1,
        "quantityReceived": 100.0,
        "quantityInvoiced": 50.0  // Invoiced but flag says "true"
      }
    ]
  }
}
```

### New GRN Response
```json
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "status": "RECEIVED",
    "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ Accurate state
    "grnItemsList": [
      {
        "id": 1,
        "quantityReceived": 100.0,
        "quantityInvoiced": 50.0  // Matches PARTIALLY_INVOICED
      }
    ]
  }
}
```

---

## Migration Mapping

### Old Boolean → New Enum

| Old Value | New Value | When |
|-----------|-----------|------|
| `false` | `NOT_INVOICED` | No invoices created |
| `true` | `PARTIALLY_INVOICED` or `FULLY_INVOICED` | Depends on actual quantities |

---

## Complete API Examples

### Example 1: Create GRN
```bash
curl --location 'http://localhost:8081/api/grn/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "poId": 1,
  "receiptType": "WAREHOUSE_STOCK",
  "warehouseId": 1,
  "grnItemsList": [
    {
      "poItemId": 1,
      "itemId": 1,
      "quantityReceived": 100.0
    },
    {
      "poItemId": 2,
      "itemId": 2,
      "quantityReceived": 50.0
    }
  ]
}'
```

**Response includes**:
```json
{
  "invoiceStatus": "NOT_INVOICED"  // Initial status
}
```

---

### Example 2: Create Partial Invoice
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "grnId": 1,
  "totalAmount": 25000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-03-31",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 50.0,
      "rate": 500.0
    }
  ]
}'
```

**After this**: GRN.invoiceStatus changes to `PARTIALLY_INVOICED`

---

### Example 3: Get GRN Status
```bash
curl --location 'http://localhost:8081/api/grn/getById/1' \
--header 'Authorization: Bearer YOUR_TOKEN'
```

**Response**:
```json
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "status": "RECEIVED",
    "invoiceStatus": "PARTIALLY_INVOICED",
    "grnItemsList": [
      {
        "id": 1,
        "itemId": 1,
        "itemName": "Cement",
        "quantityReceived": 100.0,
        "quantityInvoiced": 50.0
      },
      {
        "id": 2,
        "itemId": 2,
        "itemName": "Steel",
        "quantityReceived": 50.0,
        "quantityInvoiced": 0.0
      }
    ],
    "projectName": "Tower A",
    "vendorName": "ABC Suppliers",
    "poNumber": "PO-20260201-001"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### Example 4: Complete the Invoice
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "grnId": 1,
  "totalAmount": 40000.00,
  "invoiceDate": "2026-03-05",
  "dueDate": "2026-04-05",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 50.0,
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

**After this**: GRN.invoiceStatus changes to `FULLY_INVOICED`

---

### Example 5: Update Invoice and Recalculate Status
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "totalAmount": 30000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
      "rate": 300.0
    }
  ]
}'
```

**After this**: Status recalculated based on new quantities

---

## Filter Combinations

### Get NOT_INVOICED GRNs for Specific Vendor
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "vendorId": 3,
  "invoiceStatus": "NOT_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get PARTIALLY_INVOICED GRNs for Specific PO
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "poId": 7,
  "invoiceStatus": "PARTIALLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get FULLY_INVOICED GRNs in Date Range
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "FULLY_INVOICED",
  "startDate": "2026-02-01",
  "endDate": "2026-02-28",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## Status Values Reference

| Value | Meaning | Use Case |
|-------|---------|----------|
| `NOT_INVOICED` | No invoices created yet | Find GRNs ready for invoicing |
| `PARTIALLY_INVOICED` | Some items invoiced, some pending | Track incomplete invoicing |
| `FULLY_INVOICED` | All items fully invoiced | Identify completed GRNs |
| `null` | No filter applied | Get all GRNs |

---

## Common Use Cases

### Use Case 1: Find GRNs Ready for Invoicing
**Filter**: `invoiceStatus = "NOT_INVOICED"`

**Business Need**: Create first invoices for received goods

---

### Use Case 2: Track Pending Invoices
**Filter**: `invoiceStatus = "PARTIALLY_INVOICED"`

**Business Need**: Complete partial invoices

---

### Use Case 3: Audit Completed Invoices
**Filter**: `invoiceStatus = "FULLY_INVOICED"`

**Business Need**: Verify all received goods are invoiced

---

### Use Case 4: Vendor-Specific Invoice Status
**Filters**: `vendorId = X` + `invoiceStatus = "PARTIALLY_INVOICED"`

**Business Need**: Follow up with specific vendor on pending invoices

---

## Error Scenarios

### Error 1: Try to Over-Invoice
```bash
# This will FAIL if trying to invoice more than received
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "grnId": 1,
  "totalAmount": 75000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 150.0,  // Exceeds received qty (100)
      "rate": 500.0
    }
  ]
}'
```

**Response**:
```json
{
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 1",
  "responseCode": "9998"
}
```

---

## Quick Migration Checklist for API Consumers

- [ ] Replace `invoiceCreated` with `invoiceStatus` in filter requests
- [ ] Change from boolean `true/false` to enum values
- [ ] Handle three states instead of two
- [ ] Update UI to show `PARTIALLY_INVOICED` state
- [ ] Update reports to use new status values
- [ ] Test all GRN-related workflows
- [ ] Update integration documentation

---

## Postman Collection Variables

Set these in your Postman environment:

```json
{
  "baseUrl": "http://localhost:8081",
  "token": "YOUR_JWT_TOKEN",
  "orgId": "1",
  "grnId": "1",
  "invoiceStatus": "PARTIALLY_INVOICED"
}
```

---

## Summary

### What Changed
- ❌ Removed: `invoiceCreated` (Boolean)
- ✅ Added: `invoiceStatus` (Enum with 3 values)

### Why
- Support multiple invoices per GRN
- Track partial invoicing accurately
- Prevent over-invoicing
- Better business intelligence

### Impact
- **Breaking Change**: API consumers must update requests
- **Database Change**: Column renamed and type changed
- **Benefits**: More accurate status tracking, better filtering

---

## End of Comparison Guide
