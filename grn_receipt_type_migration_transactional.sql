-- =====================================================
-- GRN Receipt Type Migration Script (Transactional)
-- =====================================================
-- Purpose: Safely update old enum values to new enum values
-- This script wraps updates in a transaction for rollback safety
-- =====================================================

-- Start Transaction
START TRANSACTION;

-- Step 1: Check current data before migration
SELECT 'Before Migration - Current Receipt Type Distribution:' as info;
SELECT receipt_type, COUNT(*) as count
FROM grn
GROUP BY receipt_type;

-- Step 2: Create a backup column (optional safety measure)
-- ALTER TABLE grn ADD COLUMN receipt_type_backup VARCHAR(50);
-- UPDATE grn SET receipt_type_backup = receipt_type;

-- Step 3: Update WAREHOUSE_STOCK to STOCK
UPDATE grn
SET receipt_type = 'STOCK'
WHERE receipt_type = 'WAREHOUSE_STOCK';

-- Check affected rows
SELECT ROW_COUNT() as 'Rows updated from WAREHOUSE_STOCK to STOCK';

-- Step 4: Update DIRECT_CONSUME to DIRECT
UPDATE grn
SET receipt_type = 'DIRECT'
WHERE receipt_type = 'DIRECT_CONSUME';

-- Check affected rows
SELECT ROW_COUNT() as 'Rows updated from DIRECT_CONSUME to DIRECT';

-- Step 5: Verify migration
SELECT 'After Migration - Updated Receipt Type Distribution:' as info;
SELECT receipt_type, COUNT(*) as count
FROM grn
GROUP BY receipt_type;

-- Step 6: Check for any remaining old values (should return 0 rows)
SELECT 'Verification - Old values remaining (should be empty):' as info;
SELECT COUNT(*) as remaining_old_values
FROM grn
WHERE receipt_type IN ('WAREHOUSE_STOCK', 'DIRECT_CONSUME');

-- =====================================================
-- IMPORTANT: Review the results above
-- If everything looks correct, COMMIT the transaction
-- If there's an issue, ROLLBACK
-- =====================================================

-- Option 1: Commit the changes (uncomment to apply)
-- COMMIT;

-- Option 2: Rollback the changes (uncomment to revert)
-- ROLLBACK;

-- =====================================================
-- After successful COMMIT, optionally remove backup column:
-- ALTER TABLE grn DROP COLUMN receipt_type_backup;
-- =====================================================
