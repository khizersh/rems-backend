# GRN Invoice Status Enhancement - Implementation Summary

## Overview
Successfully updated the GRN module to support **multiple invoices per GRN** and **partial invoicing** by replacing the boolean `invoiceCreated` flag with an enum-based `invoiceStatus` field that accurately represents the invoicing state.

---

## Changes Implemented

### 1. **Created New Enum: GrnInvoiceStatus**
**File**: `GrnInvoiceStatus.java`

```java
public enum GrnInvoiceStatus {
    NOT_INVOICED,        // No invoices created (total invoiced qty = 0)
    PARTIALLY_INVOICED,  // Some items invoiced (0 < invoiced qty < received qty)
    FULLY_INVOICED       // All items fully invoiced (invoiced qty = received qty)
}
```

**Purpose**: Provides accurate representation of invoicing progress for a GRN

---

### 2. **Updated GRN Entity**
**File**: `Grn.java`

**Removed**:
```java
@Column(nullable = false)
private Boolean invoiceCreated = false;
```

**Added**:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private GrnInvoiceStatus invoiceStatus = GrnInvoiceStatus.NOT_INVOICED;
```

**Import Added**:
```java
import com.rem.backend.purchasemanagement.enums.GrnInvoiceStatus;
```

---

### 3. **Added Status Calculation Logic in GrnService**
**File**: `GrnService.java`

**New Method**: `calculateAndUpdateGrnInvoiceStatus(Long grnId, String loggedInUser)`

**Business Logic**:
1. Fetches all GRN items for the given GRN
2. For each item, compares `quantityInvoiced` vs `quantityReceived`
3. Categorizes items as:
   - **Not Invoiced**: `quantityInvoiced = 0`
   - **Fully Invoiced**: `quantityInvoiced = quantityReceived`
   - **Partially Invoiced**: `0 < quantityInvoiced < quantityReceived`
4. Determines GRN-level status:
   - If **all items** are not invoiced → `NOT_INVOICED`
   - If **all items** are fully invoiced → `FULLY_INVOICED`
   - Otherwise → `PARTIALLY_INVOICED`
5. Validates over-invoicing (throws error if `quantityInvoiced > quantityReceived`)
6. Updates GRN status if changed

**Key Features**:
- ✅ Transactional method
- ✅ Over-invoicing prevention
- ✅ Automatic status calculation
- ✅ Audit trail (updatedBy, updatedDate)

---

### 4. **Updated VendorInvoiceService**
**File**: `VendorInvoiceService.java`

**Changes in `createInvoice` method**:

**Removed**:
```java
grn.setInvoiceCreated(true);
grn.setUpdatedBy(loggedInUser);
grn.setUpdatedDate(now);
grnRepo.save(grn);
```

**Added**:
```java
grnService.calculateAndUpdateGrnInvoiceStatus(grn.getId(), loggedInUser);
```

**Changes in `updateInvoice` method**:
- Added `grnService.calculateAndUpdateGrnInvoiceStatus(grn.getId(), loggedInUser);` after updating invoice items
- Status recalculated automatically after rollback and new item creation

**Dependency Added**:
```java
private final GrnService grnService;
```

---

### 5. **Updated GrnFilterRequest DTO**
**File**: `GrnFilterRequest.java`

**Removed**:
```java
private Boolean invoiceCreated; // Old boolean filter
```

**Added**:
```java
private GrnInvoiceStatus invoiceStatus; // New enum filter
```

**Import Added**:
```java
import com.rem.backend.purchasemanagement.enums.GrnInvoiceStatus;
```

---

### 6. **Updated GrnRepo Repository**
**File**: `GrnRepo.java`

**Query Updated**:

**Old**:
```java
"AND (:invoiceCreated IS NULL OR g.invoiceCreated = :invoiceCreated) "
```

**New**:
```java
"AND (:invoiceStatus IS NULL OR g.invoiceStatus = :invoiceStatus) "
```

**Method Signature Updated**:

**Old**:
```java
Page<Grn> findByConditionalFilters(
    ...
    @Param("invoiceCreated") Boolean invoiceCreated,
    ...
);
```

**New**:
```java
Page<Grn> findByConditionalFilters(
    ...
    @Param("invoiceStatus") GrnInvoiceStatus invoiceStatus,
    ...
);
```

**Import Added**:
```java
import com.rem.backend.purchasemanagement.enums.GrnInvoiceStatus;
```

---

### 7. **Updated GrnService Filter Method**
**File**: `GrnService.java`

**Method**: `getByConditionalFilters`

**Parameter Changed**:

**Old**:
```java
Boolean invoiceCreated
```

**New**:
```java
GrnInvoiceStatus invoiceStatus
```

**Repository Call Updated**:
```java
Page<Grn> grnPage = grnRepo.findByConditionalFilters(
    orgId, poId, vendorId, status, 
    startDate, endDate,
    invoiceStatus,  // Changed from invoiceCreated
    pageable
);
```

---

### 8. **Updated GrnController**
**File**: `GrnController.java`

**Endpoint**: `POST /api/grn/getByStatusAndDateRange`

**Service Call Updated**:

**Old**:
```java
return grnService.getByConditionalFilters(
    ...
    request.getInvoiceCreated(),
    ...
);
```

**New**:
```java
return grnService.getByConditionalFilters(
    ...
    request.getInvoiceStatus(),
    ...
);
```

---

## Business Logic Summary

### Invoice Status Calculation Rules

#### Item Level:
```
if (quantityInvoiced == 0)
    → Item is NOT_INVOICED
    
else if (quantityInvoiced == quantityReceived)
    → Item is FULLY_INVOICED
    
else if (0 < quantityInvoiced < quantityReceived)
    → Item is PARTIALLY_INVOICED
    
else if (quantityInvoiced > quantityReceived)
    → ERROR: Over-invoicing detected
```

#### GRN Level:
```
if (ALL items are NOT_INVOICED)
    → GRN status = NOT_INVOICED
    
else if (ALL items are FULLY_INVOICED)
    → GRN status = FULLY_INVOICED
    
else
    → GRN status = PARTIALLY_INVOICED
```

---

## When Status is Calculated

The `calculateAndUpdateGrnInvoiceStatus` method is called automatically:

1. ✅ **After creating a vendor invoice** (`VendorInvoiceService.createInvoice`)
2. ✅ **After updating a vendor invoice** (`VendorInvoiceService.updateInvoice`)
3. 🔜 **After deleting/canceling a vendor invoice** (to be implemented if needed)

---

## API Changes

### Updated API Request

**Endpoint**: `POST /api/grn/getByStatusAndDateRange`

**Old Request Body**:
```json
{
  "orgId": 1,
  "poId": null,
  "vendorId": null,
  "status": "RECEIVED",
  "startDate": "2026-02-18",
  "endDate": "2026-02-18",
  "invoiceCreated": true,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

**New Request Body**:
```json
{
  "orgId": 1,
  "poId": null,
  "vendorId": null,
  "status": "RECEIVED",
  "startDate": "2026-02-18",
  "endDate": "2026-02-18",
  "invoiceStatus": "PARTIALLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

**Valid `invoiceStatus` Values**:
- `"NOT_INVOICED"` - Filter GRNs with no invoices
- `"PARTIALLY_INVOICED"` - Filter GRNs with partial invoices
- `"FULLY_INVOICED"` - Filter GRNs fully invoiced
- `null` - Show all GRNs regardless of invoice status

---

## Updated CURL Commands

### Get NOT_INVOICED GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 1,
  "poId": null,
  "vendorId": null,
  "status": "RECEIVED",
  "startDate": null,
  "endDate": null,
  "invoiceStatus": "NOT_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get PARTIALLY_INVOICED GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "PARTIALLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
'
```

### Get FULLY_INVOICED GRNs
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": "FULLY_INVOICED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get All GRNs (No Invoice Filter)
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 1,
  "invoiceStatus": null,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## Database Migration

### Required SQL for Existing Database

If you have existing data, run this SQL to migrate:

```sql
-- Step 1: Add new invoiceStatus column
ALTER TABLE grn 
ADD COLUMN invoice_status VARCHAR(50) NOT NULL DEFAULT 'NOT_INVOICED';

-- Step 2: Migrate data from invoiceCreated to invoiceStatus
UPDATE grn 
SET invoice_status = CASE 
    WHEN invoice_created = TRUE THEN 'FULLY_INVOICED'
    ELSE 'NOT_INVOICED'
END;

-- Step 3: Drop old invoiceCreated column
ALTER TABLE grn 
DROP COLUMN invoice_created;

-- Step 4: Verify migration
SELECT id, grn_number, invoice_status FROM grn LIMIT 10;
```

**Note**: The initial migration sets status as either `FULLY_INVOICED` or `NOT_INVOICED`. After running the application and processing invoices, the status will automatically update to `PARTIALLY_INVOICED` where applicable.

---

## Benefits of This Change

### 1. **Accurate Status Representation**
- ❌ **Old**: Boolean couldn't represent partial invoicing
- ✅ **New**: Three states accurately reflect invoice progress

### 2. **Multiple Invoices Support**
- ❌ **Old**: Once `invoiceCreated = true`, couldn't track additional invoices
- ✅ **New**: Status updates dynamically as invoices are added

### 3. **Partial Invoicing Support**
- ❌ **Old**: No distinction between full and partial invoicing
- ✅ **New**: `PARTIALLY_INVOICED` state explicitly tracks partial progress

### 4. **Better Filtering**
- ❌ **Old**: Filter by `true/false` only
- ✅ **New**: Filter by `NOT_INVOICED`, `PARTIALLY_INVOICED`, `FULLY_INVOICED`

### 5. **Over-Invoicing Prevention**
- ✅ Automatic validation prevents invoicing more than received quantity
- ✅ Throws clear error messages

### 6. **Automatic Status Management**
- ✅ Status calculated automatically after each invoice operation
- ✅ No manual status updates needed
- ✅ Always consistent with actual invoiced quantities

---

## Important Notes

### Separation of Concerns
- **GrnStatus** (RECEIVED, CANCELLED) → Physical receipt status
- **GrnInvoiceStatus** (NOT_INVOICED, PARTIALLY_INVOICED, FULLY_INVOICED) → Financial billing status

These are **separate and independent** statuses.

### Status Update Triggers
Status is recalculated automatically:
- ✅ After invoice creation
- ✅ After invoice update
- 🔜 After invoice deletion (if implemented)

### Manual Status Setting
❌ **Do NOT manually set `invoiceStatus`**  
✅ Always use `grnService.calculateAndUpdateGrnInvoiceStatus()`

---

## Testing Scenarios

### Scenario 1: Create First Invoice (Partial)
1. GRN has 2 items: Item A (100 qty), Item B (50 qty)
2. Create invoice for: Item A (50 qty), Item B (0 qty)
3. **Result**: GRN status = `PARTIALLY_INVOICED`

### Scenario 2: Create Second Invoice (Complete)
1. Continuing from Scenario 1
2. Create invoice for: Item A (50 qty), Item B (50 qty)
3. **Result**: GRN status = `FULLY_INVOICED`

### Scenario 3: Update Invoice (Change Quantities)
1. GRN has 1 item: Item A (100 qty), currently 50 invoiced
2. Update invoice from 50 qty to 100 qty
3. **Result**: GRN status changes from `PARTIALLY_INVOICED` to `FULLY_INVOICED`

### Scenario 4: Over-Invoicing Prevention
1. GRN has 1 item: Item A (100 qty received)
2. Try to create invoice for 150 qty
3. **Result**: Error thrown, transaction rolled back

---

## Files Modified

1. ✅ **GrnInvoiceStatus.java** - New enum created
2. ✅ **Grn.java** - Field replaced (invoiceCreated → invoiceStatus)
3. ✅ **GrnService.java** - Added calculateAndUpdateGrnInvoiceStatus method
4. ✅ **VendorInvoiceService.java** - Updated createInvoice and updateInvoice
5. ✅ **GrnFilterRequest.java** - DTO field updated
6. ✅ **GrnRepo.java** - Query and method signature updated
7. ✅ **GrnController.java** - Service call updated

---

## Backward Compatibility

⚠️ **Breaking Change**: This is a **breaking change** for:
- Existing API clients using `invoiceCreated` filter
- Database schema (column renamed)
- Any code directly accessing the `invoiceCreated` field

**Migration Required**:
- Update all API clients to use `invoiceStatus` instead of `invoiceCreated`
- Run database migration script
- Update any custom queries or reports

---

## Future Enhancements

### Potential Additions:
1. **Invoice Deletion Support**: Implement status recalculation on invoice deletion
2. **Status History**: Track status changes over time
3. **Dashboard Metrics**: Show counts by invoice status
4. **Alerts**: Notify when GRN is fully invoiced
5. **Validation Reports**: List GRNs with over-invoicing issues

---

## Conclusion

✅ **Successfully implemented GRN invoice status enhancement**  
✅ **Supports multiple invoices per GRN**  
✅ **Accurate partial invoicing tracking**  
✅ **Automatic status calculation**  
✅ **Over-invoicing prevention**  
✅ **Production-ready code**

The system now properly tracks invoicing progress across all GRNs with accurate, enum-based status representation.

---

## End of Document
