# GRN Invoice Status - Complete Testing Guide

## Testing Overview
This guide provides comprehensive test scenarios to validate the GRN invoice status enhancement implementation.

---

## Pre-Testing Checklist

- [ ] Database migration completed
- [ ] Application restarted with new code
- [ ] Valid JWT token obtained
- [ ] Test data available (GRN with items)

---

## Test Suite

### Test 1: Create GRN and Verify Initial Status

**Objective**: Confirm new GRNs have `NOT_INVOICED` status

**Steps**:
1. Create a new GRN with items
2. Check GRN status in response

**Expected Result**: 
```json
{
  "invoiceStatus": "NOT_INVOICED"
}
```

**CURL**:
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
    }
  ]
}'
```

---

### Test 2: Filter GRNs by NOT_INVOICED Status

**Objective**: Verify filter returns only GRNs without invoices

**CURL**:
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "NOT_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected**: List of GRNs with `invoiceStatus = "NOT_INVOICED"`

---

### Test 3: Create Partial Invoice

**Objective**: Verify GRN status changes to `PARTIALLY_INVOICED`

**Scenario**: 
- GRN has Item A (100 qty), Item B (50 qty)
- Create invoice for Item A (50 qty only)

**Steps**:
1. Note GRN ID and item IDs
2. Create invoice for 50% of one item

**CURL**:
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

3. Get GRN by ID and check status

**CURL**:
```bash
curl --location 'http://localhost:8081/api/grn/getById/1' \
--header 'Authorization: Bearer YOUR_TOKEN'
```

**Expected Result**:
```json
{
  "invoiceStatus": "PARTIALLY_INVOICED",
  "grnItemsList": [
    {
      "id": 1,
      "quantityReceived": 100.0,
      "quantityInvoiced": 50.0
    },
    {
      "id": 2,
      "quantityReceived": 50.0,
      "quantityInvoiced": 0.0
    }
  ]
}
```

---

### Test 4: Complete Invoice to FULLY_INVOICED

**Objective**: Verify GRN status changes to `FULLY_INVOICED` when all items invoiced

**Steps**:
1. Continuing from Test 3
2. Create second invoice for remaining quantities

**CURL**:
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

3. Get GRN and verify status

**Expected Result**:
```json
{
  "invoiceStatus": "FULLY_INVOICED",
  "grnItemsList": [
    {
      "id": 1,
      "quantityReceived": 100.0,
      "quantityInvoiced": 100.0
    },
    {
      "id": 2,
      "quantityReceived": 50.0,
      "quantityInvoiced": 50.0
    }
  ]
}
```

---

### Test 5: Filter by PARTIALLY_INVOICED

**Objective**: Verify filter returns only partially invoiced GRNs

**CURL**:
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "PARTIALLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected**: List of GRNs with `invoiceStatus = "PARTIALLY_INVOICED"`

---

### Test 6: Filter by FULLY_INVOICED

**Objective**: Verify filter returns only fully invoiced GRNs

**CURL**:
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "FULLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected**: List of GRNs with `invoiceStatus = "FULLY_INVOICED"`

---

### Test 7: Over-Invoicing Prevention

**Objective**: Verify system prevents over-invoicing

**Scenario**: Try to invoice more than received quantity

**CURL**:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "grnId": 1,
  "totalAmount": 75000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 150.0,
      "rate": 500.0
    }
  ]
}'
```

**Expected Error Response**:
```json
{
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 1",
  "responseCode": "9998"
}
```

**Verify**: GRN status should remain unchanged

---

### Test 8: Update Invoice and Status Recalculation

**Objective**: Verify status recalculates after invoice update

**Scenario**: 
- GRN initially PARTIALLY_INVOICED
- Update invoice to fully invoice all items
- Verify status changes to FULLY_INVOICED

**Steps**:
1. Create partial invoice (50% of items)
2. Verify status is PARTIALLY_INVOICED
3. Update invoice to 100% of items
4. Verify status changes to FULLY_INVOICED

**CURL for Update**:
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "totalAmount": 65000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
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

**Expected**: GRN status changes from PARTIALLY_INVOICED → FULLY_INVOICED

---

### Test 9: Multiple Invoices for Same GRN

**Objective**: Verify system supports multiple invoices per GRN

**Steps**:
1. Create Invoice 1 for 30% of items
2. Create Invoice 2 for 30% of items
3. Create Invoice 3 for 40% of items
4. Verify status progresses: NOT_INVOICED → PARTIALLY_INVOICED → PARTIALLY_INVOICED → FULLY_INVOICED

**Verification Query**:
```bash
# Check invoice count per GRN
curl --location 'http://localhost:8081/api/grn/getById/1' \
--header 'Authorization: Bearer YOUR_TOKEN'
```

**Expected**: Multiple invoices exist, status = FULLY_INVOICED after all invoices

---

### Test 10: Filter with NULL invoiceStatus

**Objective**: Verify NULL filter returns all GRNs regardless of status

**CURL**:
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": null,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected**: All GRNs returned (mix of NOT_INVOICED, PARTIALLY_INVOICED, FULLY_INVOICED)

---

### Test 11: Status Consistency After Invoice Delete (if implemented)

**Objective**: Verify status recalculates if invoice is deleted

**Note**: Only test if delete invoice functionality exists

**Steps**:
1. Create invoice making GRN PARTIALLY_INVOICED
2. Delete the invoice
3. Verify status returns to NOT_INVOICED

---

### Test 12: Database Validation

**Objective**: Verify database records are consistent

**SQL Queries**:

```sql
-- Check status distribution
SELECT 
    invoice_status,
    COUNT(*) as count
FROM grn
GROUP BY invoice_status;

-- Verify PARTIALLY_INVOICED GRNs have partial invoicing
SELECT 
    g.id,
    g.grn_number,
    g.invoice_status,
    gi.quantity_received,
    gi.quantity_invoiced,
    ROUND((gi.quantity_invoiced / gi.quantity_received) * 100, 2) as percent_invoiced
FROM grn g
JOIN grn_items gi ON g.id = gi.grn_id
WHERE g.invoice_status = 'PARTIALLY_INVOICED';

-- Verify FULLY_INVOICED GRNs are 100% invoiced
SELECT 
    g.id,
    g.grn_number,
    g.invoice_status,
    SUM(gi.quantity_received) as total_received,
    SUM(gi.quantity_invoiced) as total_invoiced,
    CASE 
        WHEN SUM(gi.quantity_received) = SUM(gi.quantity_invoiced) THEN 'CORRECT'
        ELSE 'MISMATCH'
    END as status_check
FROM grn g
JOIN grn_items gi ON g.id = gi.grn_id
WHERE g.invoice_status = 'FULLY_INVOICED'
GROUP BY g.id, g.grn_number, g.invoice_status;
```

**Expected**: No mismatches found

---

## Test Results Checklist

Mark each test as passed or failed:

- [ ] Test 1: Initial NOT_INVOICED status
- [ ] Test 2: Filter NOT_INVOICED
- [ ] Test 3: Partial invoice creates PARTIALLY_INVOICED
- [ ] Test 4: Complete invoice creates FULLY_INVOICED
- [ ] Test 5: Filter PARTIALLY_INVOICED
- [ ] Test 6: Filter FULLY_INVOICED
- [ ] Test 7: Over-invoicing prevented
- [ ] Test 8: Update invoice recalculates status
- [ ] Test 9: Multiple invoices supported
- [ ] Test 10: NULL filter returns all
- [ ] Test 11: Delete recalculates (if applicable)
- [ ] Test 12: Database consistency validated

---

## Performance Testing

### Large Dataset Test

**Objective**: Verify performance with large GRN count

**Setup**: Create 1000 GRNs with varying invoice statuses

**Test**:
```bash
# Time this request
time curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN' \
--data '{
  "orgId": 1,
  "page": 0,
  "size": 100,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected**: Response time < 2 seconds

---

## Edge Cases

### Edge Case 1: GRN with Zero Items
**Test**: Create GRN with no items (should fail validation)

### Edge Case 2: Invoice with Zero Quantity
**Test**: Create invoice with qty = 0 (should fail validation)

### Edge Case 3: Negative Quantity
**Test**: Create invoice with negative qty (should fail validation)

### Edge Case 4: Concurrent Invoice Creation
**Test**: Create two invoices simultaneously for same GRN item

---

## Regression Testing

After implementation, verify:

- [ ] Existing GRN APIs still work
- [ ] Existing Invoice APIs still work
- [ ] PO workflows unaffected
- [ ] Warehouse integration unaffected
- [ ] Reporting features work with new status

---

## Acceptance Criteria

✅ All tests pass  
✅ No over-invoicing possible  
✅ Status always reflects actual invoiced quantities  
✅ Multiple invoices per GRN work correctly  
✅ Filters return correct results  
✅ Database records are consistent  
✅ Performance is acceptable  
✅ No regressions in existing features  

---

## Troubleshooting

### Issue: Status not updating after invoice
**Solution**: Check if `calculateAndUpdateGrnInvoiceStatus()` is being called

### Issue: Wrong status calculated
**Solution**: Verify `quantity_invoiced` values in `grn_items` table

### Issue: Over-invoicing allowed
**Solution**: Check validation logic in invoice creation

### Issue: Filter returns wrong GRNs
**Solution**: Verify query in `GrnRepo.findByConditionalFilters()`

---

## Test Environment Setup

1. **Fresh Database**: Use migrated database with test data
2. **JWT Token**: Obtain valid token for API calls
3. **Test Data**: Create at least 5 GRNs with various scenarios
4. **Logging**: Enable debug logging for invoice and GRN services

---

## Sign-Off

Testing completed by: __________________  
Date: __________________  
All tests passed: ☐ Yes ☐ No  
Production-ready: ☐ Yes ☐ No  

---

## End of Testing Guide
