# Database Migration Guide - GRN Invoice Status

## Migration Overview
This guide helps you migrate from the old `invoice_created` (BOOLEAN) column to the new `invoice_status` (ENUM) column in the `grn` table.

---

## Step-by-Step Migration

### Step 1: Backup Database
```sql
-- Create backup before migration
mysqldump -u your_username -p your_database_name > backup_before_grn_migration.sql
```

---

### Step 2: Add New Column
```sql
-- Add invoice_status column with default value
ALTER TABLE grn 
ADD COLUMN invoice_status VARCHAR(50) NOT NULL DEFAULT 'NOT_INVOICED'
AFTER direct_consume_project_id;
```

**Expected Result**: Column added successfully

---

### Step 3: Migrate Existing Data

#### Option A: Simple Migration (Conservative)
```sql
-- Set status based on old invoiceCreated flag
UPDATE grn 
SET invoice_status = CASE 
    WHEN invoice_created = TRUE THEN 'FULLY_INVOICED'
    WHEN invoice_created = FALSE THEN 'NOT_INVOICED'
    ELSE 'NOT_INVOICED'
END;
```

#### Option B: Accurate Migration (Recommended)
```sql
-- Calculate accurate status based on actual invoice data
UPDATE grn g
SET invoice_status = (
    CASE
        -- Check if no invoices exist for this GRN
        WHEN NOT EXISTS (
            SELECT 1 FROM vendor_invoice vi WHERE vi.grn_id = g.id
        ) THEN 'NOT_INVOICED'
        
        -- Check if all GRN items are fully invoiced
        WHEN (
            SELECT COUNT(*) 
            FROM grn_items gi 
            WHERE gi.grn_id = g.id 
            AND COALESCE(gi.quantity_invoiced, 0) >= gi.quantity_received
        ) = (
            SELECT COUNT(*) 
            FROM grn_items gi 
            WHERE gi.grn_id = g.id
        ) THEN 'FULLY_INVOICED'
        
        -- Check if at least one item has some invoicing
        WHEN EXISTS (
            SELECT 1 
            FROM grn_items gi 
            WHERE gi.grn_id = g.id 
            AND COALESCE(gi.quantity_invoiced, 0) > 0
        ) THEN 'PARTIALLY_INVOICED'
        
        -- Default to NOT_INVOICED
        ELSE 'NOT_INVOICED'
    END
);
```

---

### Step 4: Verify Migration
```sql
-- Check status distribution
SELECT 
    invoice_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM grn), 2) as percentage
FROM grn
GROUP BY invoice_status
ORDER BY count DESC;
```

**Expected Output**:
```
+---------------------+-------+------------+
| invoice_status      | count | percentage |
+---------------------+-------+------------+
| NOT_INVOICED        |   150 |      75.00 |
| PARTIALLY_INVOICED  |    30 |      15.00 |
| FULLY_INVOICED      |    20 |      10.00 |
+---------------------+-------+------------+
```

---

### Step 5: Validate Data Integrity
```sql
-- Check for any NULL values (should be none)
SELECT COUNT(*) as null_count
FROM grn
WHERE invoice_status IS NULL;
```

**Expected Result**: `null_count = 0`

```sql
-- Check for invalid status values
SELECT invoice_status, COUNT(*) as count
FROM grn
WHERE invoice_status NOT IN ('NOT_INVOICED', 'PARTIALLY_INVOICED', 'FULLY_INVOICED')
GROUP BY invoice_status;
```

**Expected Result**: Empty result set

---

### Step 6: Cross-Check with Invoice Data
```sql
-- Verify NOT_INVOICED status matches reality
SELECT 
    g.id,
    g.grn_number,
    g.invoice_status,
    COUNT(vi.id) as invoice_count,
    COALESCE(SUM(gi.quantity_invoiced), 0) as total_invoiced
FROM grn g
LEFT JOIN vendor_invoice vi ON g.id = vi.grn_id
LEFT JOIN grn_items gi ON g.id = gi.grn_id
WHERE g.invoice_status = 'NOT_INVOICED'
GROUP BY g.id, g.grn_number, g.invoice_status
HAVING invoice_count > 0 OR total_invoiced > 0;
```

**Expected Result**: Empty result set (no mismatches)

---

### Step 7: Drop Old Column
```sql
-- Remove the old invoice_created column
ALTER TABLE grn 
DROP COLUMN invoice_created;
```

---

### Step 8: Verify Final Schema
```sql
-- Check column exists with correct properties
DESCRIBE grn;
```

**Expected Output** (should include):
```
+-------------------------+--------------+------+-----+---------------+
| Field                   | Type         | Null | Key | Default       |
+-------------------------+--------------+------+-----+---------------+
| ...                     | ...          | ...  | ... | ...           |
| invoice_status          | varchar(50)  | NO   |     | NOT_INVOICED  |
| ...                     | ...          | ...  | ... | ...           |
+-------------------------+--------------+------+-----+---------------+
```

---

## Rollback Procedure

If you need to rollback the migration:

```sql
-- Step 1: Restore old column
ALTER TABLE grn 
ADD COLUMN invoice_created TINYINT(1) NOT NULL DEFAULT 0
AFTER direct_consume_project_id;

-- Step 2: Restore data
UPDATE grn 
SET invoice_created = CASE 
    WHEN invoice_status = 'FULLY_INVOICED' THEN 1
    ELSE 0
END;

-- Step 3: Drop new column
ALTER TABLE grn 
DROP COLUMN invoice_status;

-- Step 4: Verify
SELECT invoice_created, COUNT(*) FROM grn GROUP BY invoice_created;
```

---

## Testing Checklist

After migration, verify:

- [ ] All GRNs have valid `invoice_status` values
- [ ] No NULL values in `invoice_status` column
- [ ] Status distribution looks reasonable
- [ ] GRN API with `invoiceStatus` filter works correctly
- [ ] Invoice creation updates GRN status correctly
- [ ] Invoice update updates GRN status correctly
- [ ] Application starts without errors
- [ ] No references to old `invoiceCreated` field remain

---

## Sample Test Queries

### Test 1: NOT_INVOICED GRNs
```sql
-- Should return GRNs with no invoices
SELECT g.id, g.grn_number, g.invoice_status
FROM grn g
WHERE g.invoice_status = 'NOT_INVOICED'
LIMIT 5;
```

### Test 2: PARTIALLY_INVOICED GRNs
```sql
-- Should return GRNs with partial invoices
SELECT 
    g.id,
    g.grn_number,
    g.invoice_status,
    gi.quantity_received,
    gi.quantity_invoiced,
    ROUND((gi.quantity_invoiced / gi.quantity_received) * 100, 2) as invoiced_percentage
FROM grn g
JOIN grn_items gi ON g.id = gi.grn_id
WHERE g.invoice_status = 'PARTIALLY_INVOICED'
LIMIT 5;
```

### Test 3: FULLY_INVOICED GRNs
```sql
-- Should return GRNs where all items fully invoiced
SELECT 
    g.id,
    g.grn_number,
    g.invoice_status,
    COUNT(gi.id) as total_items,
    SUM(CASE WHEN gi.quantity_invoiced >= gi.quantity_received THEN 1 ELSE 0 END) as fully_invoiced_items
FROM grn g
JOIN grn_items gi ON g.id = gi.grn_id
WHERE g.invoice_status = 'FULLY_INVOICED'
GROUP BY g.id, g.grn_number, g.invoice_status
LIMIT 5;
```

---

## Common Issues and Solutions

### Issue 1: Migration Takes Too Long
**Solution**: Run migration during off-peak hours or in batches
```sql
-- Batch update (adjust batch size as needed)
UPDATE grn 
SET invoice_status = (your calculation here)
WHERE id BETWEEN 1 AND 1000;

UPDATE grn 
SET invoice_status = (your calculation here)
WHERE id BETWEEN 1001 AND 2000;
-- etc.
```

### Issue 2: Status Mismatch After Migration
**Solution**: Run the recalculation via application
```sql
-- Identify mismatched GRNs
SELECT g.id, g.invoice_status
FROM grn g
WHERE g.invoice_status != (your expected calculation);
```

Then use the application API to trigger recalculation or run update manually.

### Issue 3: NULL Values Appear
**Solution**: Set default value
```sql
UPDATE grn 
SET invoice_status = 'NOT_INVOICED'
WHERE invoice_status IS NULL;
```

---

## Post-Migration Application Steps

After database migration:

1. **Restart Application**: Ensure all code changes are deployed
2. **Clear Cache**: Clear any application or database caches
3. **Test APIs**: Verify all GRN and invoice APIs work correctly
4. **Monitor Logs**: Watch for any errors related to invoice status
5. **Run Integration Tests**: Execute full test suite if available

---

## Migration Checklist

- [ ] Database backed up
- [ ] New column added
- [ ] Data migrated
- [ ] Migration verified
- [ ] Old column dropped
- [ ] Rollback procedure tested (in staging/dev)
- [ ] Application restarted
- [ ] APIs tested
- [ ] Frontend updated (if needed)
- [ ] Documentation updated

---

## Estimated Downtime

- **Small DB** (< 10,000 GRNs): ~2-5 minutes
- **Medium DB** (10,000-100,000 GRNs): ~10-20 minutes  
- **Large DB** (> 100,000 GRNs): ~30-60 minutes

**Recommendation**: Schedule during maintenance window

---

## Support

If you encounter issues during migration:

1. Check application logs for errors
2. Verify SQL syntax for your specific database version
3. Test in staging environment first
4. Keep database backup handy for rollback

---

## End of Migration Guide
