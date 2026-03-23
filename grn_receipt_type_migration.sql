-- =====================================================
-- GRN Receipt Type Migration Script
-- =====================================================
-- Purpose: Update old enum values to new enum values
-- Old: WAREHOUSE_STOCK, DIRECT_CONSUME
-- New: STOCK, DIRECT
-- =====================================================

-- Step 1: Check current data before migration
SELECT 'Before Migration - Current Receipt Type Distribution:' as info;
SELECT receipt_type, COUNT(*) as count
FROM grn
GROUP BY receipt_type;

-- Step 2: Update WAREHOUSE_STOCK to STOCK
UPDATE grn
SET receipt_type = 'STOCK'
WHERE receipt_type = 'WAREHOUSE_STOCK';

-- Step 3: Update DIRECT_CONSUME to DIRECT
UPDATE grn
SET receipt_type = 'DIRECT'
WHERE receipt_type = 'DIRECT_CONSUME';

-- Step 4: Verify migration
SELECT 'After Migration - Updated Receipt Type Distribution:' as info;
SELECT receipt_type, COUNT(*) as count
FROM grn
GROUP BY receipt_type;

-- Step 5: Check for any remaining old values (should return 0 rows)
SELECT 'Verification - Old values remaining (should be empty):' as info;
SELECT *
FROM grn
WHERE receipt_type IN ('WAREHOUSE_STOCK', 'DIRECT_CONSUME');

-- Expected Result: Only STOCK and DIRECT should be present
-- =====================================================
-- Migration Complete
-- =====================================================
