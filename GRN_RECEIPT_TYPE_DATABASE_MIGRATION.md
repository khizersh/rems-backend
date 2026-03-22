# GRN Receipt Type Database Migration Guide

## Overview

The `ReceiptType` enum has been updated from old values (`WAREHOUSE_STOCK`, `DIRECT_CONSUME`) to new values (`STOCK`, `DIRECT`).

Existing GRN records in the database still contain the old enum values, causing errors when fetching GRNs.

---

## Error Description

**Error Message:**
```
No enum constant com.rem.backend.enums.ReceiptType.WAREHOUSE_STOCK
```

**Root Cause:**
- Database contains old enum values: `WAREHOUSE_STOCK`, `DIRECT_CONSUME`
- Java enum only has new values: `STOCK`, `DIRECT`
- JPA fails to map old database values to new enum constants

---

## Solution: Database Migration

Execute the following SQL statements to update all existing GRN records:

### SQL Migration Script

```sql
-- Update all WAREHOUSE_STOCK to STOCK
UPDATE grn 
SET receipt_type = 'STOCK' 
WHERE receipt_type = 'WAREHOUSE_STOCK';

-- Update all DIRECT_CONSUME to DIRECT
UPDATE grn 
SET receipt_type = 'DIRECT' 
WHERE receipt_type = 'DIRECT_CONSUME';

-- Verify the migration
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;
```

**Expected Result:**
```
receipt_type | count
-------------|------
STOCK        | X
DIRECT       | Y
```

---

## Migration Steps

### Step 1: Backup Database
Before running any migration, create a backup:
```bash
# For MySQL
mysqldump -u username -p database_name > backup_before_grn_migration.sql

# For PostgreSQL
pg_dump -U username database_name > backup_before_grn_migration.sql
```

### Step 2: Check Current Data
```sql
-- Check which old values exist
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;
```

### Step 3: Run Migration
```sql
-- Update receipt_type values
UPDATE grn 
SET receipt_type = 'STOCK' 
WHERE receipt_type = 'WAREHOUSE_STOCK';

UPDATE grn 
SET receipt_type = 'DIRECT' 
WHERE receipt_type = 'DIRECT_CONSUME';
```

### Step 4: Verify Migration
```sql
-- Verify no old values remain
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;

-- Should only show STOCK and DIRECT
```

### Step 5: Test API
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "orgId": 1,
  "status": "RECEIVED",
  "startDate": "2026-02-01",
  "endDate": "2026-03-31",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

## Rollback Plan

If you need to rollback the migration:

```sql
-- Rollback to old values (use only if needed)
UPDATE grn 
SET receipt_type = 'WAREHOUSE_STOCK' 
WHERE receipt_type = 'STOCK';

UPDATE grn 
SET receipt_type = 'DIRECT_CONSUME' 
WHERE receipt_type = 'DIRECT';
```

---

## Enum Value Mapping

| Old Value (Database) | New Value (Database) | Java Enum |
|---------------------|---------------------|-----------|
| `WAREHOUSE_STOCK` | `STOCK` | `ReceiptType.STOCK` |
| `DIRECT_CONSUME` | `DIRECT` | `ReceiptType.DIRECT` |

---

## Alternative: Temporary Enum Compatibility (Not Recommended)

If immediate database migration is not possible, you can temporarily add old values to the enum for backward compatibility:

```java
public enum ReceiptType {
    STOCK,      
    DIRECT,
    
    // Deprecated - for backward compatibility only
    @Deprecated
    WAREHOUSE_STOCK,
    
    @Deprecated
    DIRECT_CONSUME
}
```

Then in your service layer, convert old values to new values:

```java
private ReceiptType normalizeReceiptType(ReceiptType type) {
    if (type == ReceiptType.WAREHOUSE_STOCK) {
        return ReceiptType.STOCK;
    } else if (type == ReceiptType.DIRECT_CONSUME) {
        return ReceiptType.DIRECT;
    }
    return type;
}
```

**⚠️ Warning:** This is only a temporary solution. Run the database migration as soon as possible.

---

## Affected Tables

- **Primary:** `grn` table (column: `receipt_type`)

---

## Post-Migration Checklist

- [ ] Database backup created
- [ ] SQL migration executed successfully
- [ ] No old enum values remain in database
- [ ] GRN list API tested
- [ ] GRN filter API tested
- [ ] GRN create API tested with both STOCK and DIRECT types
- [ ] Invoice creation against GRN tested
- [ ] Vendor payment flow tested

---

## Related Documentation

- [GRN_RECEIPT_TYPE_FLOW_CHANGES.md](GRN_RECEIPT_TYPE_FLOW_CHANGES.md) - Business flow and API changes
- [VENDOR_INVOICE_API_DOCUMENTATION.md](VENDOR_INVOICE_API_DOCUMENTATION.md) - Invoice integration

---

## Support

If you encounter any issues during migration:
1. Check database backup exists
2. Verify SQL syntax for your database type (MySQL/PostgreSQL)
3. Run migration in transaction if possible
4. Test on staging environment first

---

## Database-Specific Notes

### MySQL
```sql
START TRANSACTION;
UPDATE grn SET receipt_type = 'STOCK' WHERE receipt_type = 'WAREHOUSE_STOCK';
UPDATE grn SET receipt_type = 'DIRECT' WHERE receipt_type = 'DIRECT_CONSUME';
COMMIT;
```

### PostgreSQL
```sql
BEGIN;
UPDATE grn SET receipt_type = 'STOCK' WHERE receipt_type = 'WAREHOUSE_STOCK';
UPDATE grn SET receipt_type = 'DIRECT' WHERE receipt_type = 'DIRECT_CONSUME';
COMMIT;
```

---

**Migration Status:** Required for production deployment
**Priority:** High
**Estimated Time:** 5 minutes
