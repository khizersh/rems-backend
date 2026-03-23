# Update Vendor Invoice - Quick CURL Reference

## ⚠️ IMPORTANT: Only UNPAID Invoices Can Be Updated

---

## Quick Commands

### 1. Check Invoice Status Before Update
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

**Look for**: `"status": "UNPAID"` in response

---

### 2. Get All UNPAID Invoices (Updateable)
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getByStatus/UNPAID' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 3. Update Invoice (Full Example)
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

---

### 4. Update Invoice (Minimal - Keep Existing Data)
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "totalAmount": 60000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}'
```

---

### 5. Verify Update
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

---

## Request Body Fields

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `invoiceNumber` | String | No | Keeps existing if not provided |
| `totalAmount` | Double | Yes | Total invoice amount |
| `invoiceDate` | Date | No | Format: YYYY-MM-DD |
| `dueDate` | Date | No | Format: YYYY-MM-DD |
| `invoiceItemList` | Array | Yes | At least one item required |
| `invoiceItemList[].grnItemId` | Long | Yes | Must exist in GRN |
| `invoiceItemList[].quantity` | Double | Yes | Must be > 0 |
| `invoiceItemList[].rate` | Double | Yes | Unit rate |

---

## Response Codes

| Code | Message | Meaning |
|------|---------|---------|
| `0000` | Request Success! | Update successful |
| `9998` | Invalid Parameter | Validation error or UNPAID check failed |
| `9999` | System Failure | Server error |

---

## Common Errors

### Error 1: Invoice Not UNPAID
```json
{
  "responseMessage": "Only UNPAID invoices can be updated. Current status: PAID",
  "responseCode": "9998"
}
```
**Solution**: Cannot update. Invoice has payments recorded.

---

### Error 2: Quantity Exceeds GRN
```json
{
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 5",
  "responseCode": "9998"
}
```
**Solution**: Reduce quantity or check GRN available quantities.

---

### Error 3: Invoice Not Found
```json
{
  "responseMessage": "Invoice not found with id: 999",
  "responseCode": "9998"
}
```
**Solution**: Check invoice ID exists.

---

## What Gets Updated

✅ Invoice number (if provided)  
✅ Total amount  
✅ Invoice date (if provided)  
✅ Due date (if provided)  
✅ Pending amount (auto-calculated)  
✅ All invoice items (old deleted, new created)  
✅ GRN item invoiced quantities  
✅ Updated by (from JWT token)  
✅ Updated date (auto-set)  

❌ Cannot change: GRN ID, PO ID, Vendor ID, Organization ID, Project ID, Created by, Created date

---

## What Cannot Be Updated

- **PARTIAL status invoices** (partial payment received)
- **PAID status invoices** (fully paid)
- Foreign key references (GRN, PO, Vendor, Org, Project)
- Paid amount (managed by payment APIs)

---

## Testing Workflow

1. Create invoice → Status: UNPAID
2. Update invoice → ✅ Works
3. Record payment → Status: PARTIAL or PAID
4. Try update again → ❌ Fails with error

---

## Tips

💡 Always fetch invoice first to check status  
💡 Validate quantities against GRN before updating  
💡 All changes are transactional - no partial updates  
💡 Invoice items are completely replaced on update  
💡 GRN quantities are automatically managed  

---

## Full Documentation

📄 See `VENDOR_INVOICE_CURL_COMMANDS.md` for complete API documentation
