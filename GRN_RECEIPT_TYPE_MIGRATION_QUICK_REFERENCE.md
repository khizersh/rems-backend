# GRN Receipt Type Migration - Quick Reference

## 🚨 Problem

**API Error:** `http://localhost:8081/api/grn/getByStatusAndDateRange`

```json
{
    "data": "No enum constant com.rem.backend.enums.ReceiptType.WAREHOUSE_STOCK",
    "responseMessage": "System Failure!",
    "responseCode": "9999"
}
```

---

## 🔍 Root Cause

Database contains **old enum values** but Java code uses **new enum values**:

| Location | Old Values | New Values |
|----------|-----------|------------|
| **Database** | `WAREHOUSE_STOCK`, `DIRECT_CONSUME` | Should be `STOCK`, `DIRECT` |
| **Java Enum** | ❌ Not defined | ✅ `STOCK`, `DIRECT` |

When JPA tries to map database records to Java enums, it fails because `WAREHOUSE_STOCK` doesn't exist in the current enum definition.

---

## ✅ Solution: Run Database Migration

### Option 1: Quick Fix (Direct SQL)

```sql
UPDATE grn SET receipt_type = 'STOCK' WHERE receipt_type = 'WAREHOUSE_STOCK';
UPDATE grn SET receipt_type = 'DIRECT' WHERE receipt_type = 'DIRECT_CONSUME';
```

### Option 2: Safe Migration (Use Migration Script)

Use the provided SQL script:
- **File:** `grn_receipt_type_migration_transactional.sql`
- Review results before committing
- Includes rollback option

---

## 📝 Migration Steps

### 1. Backup Database
```bash
mysqldump -u username -p database_name > backup_grn_migration.sql
```

### 2. Run Migration Script

**For MySQL:**
```bash
mysql -u username -p database_name < grn_receipt_type_migration_transactional.sql
```

**Or manually in MySQL client:**
```sql
START TRANSACTION;
UPDATE grn SET receipt_type = 'STOCK' WHERE receipt_type = 'WAREHOUSE_STOCK';
UPDATE grn SET receipt_type = 'DIRECT' WHERE receipt_type = 'DIRECT_CONSUME';
-- Verify results
SELECT receipt_type, COUNT(*) FROM grn GROUP BY receipt_type;
-- If OK:
COMMIT;
```

### 3. Verify Migration

```sql
-- Should only show STOCK and DIRECT
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;
```

### 4. Test API

```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "orgId": 1,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Expected:** Should return GRN list without errors ✅

---

## 🔄 Enum Value Mapping

```
WAREHOUSE_STOCK  →  STOCK
DIRECT_CONSUME   →  DIRECT
```

---

## 📂 Related Files

| File | Purpose |
|------|---------|
| `grn_receipt_type_migration.sql` | Simple migration script |
| `grn_receipt_type_migration_transactional.sql` | Safe transactional migration |
| `GRN_RECEIPT_TYPE_DATABASE_MIGRATION.md` | Detailed migration guide |
| `GRN_RECEIPT_TYPE_FLOW_CHANGES.md` | Business flow documentation |

---

## ⚠️ Important Notes

1. **This is a one-time migration** - Required after enum name change
2. **No code changes needed** - Only database update required
3. **Affects all existing GRN records** - Review before running
4. **Test on staging first** - Before running in production

---

## 🔍 Where ReceiptType is Used

The `ReceiptType` enum is used in:

1. **GRN Entity** (`grn` table)
   - Column: `receipt_type`
   - Values stored as strings: 'STOCK' or 'DIRECT'

2. **GRN Service**
   - Line ~167: Stock processing check
   - Line ~342: Stock processing check
   - Line ~722: Stock reversal check

3. **GRN Filter API**
   - Parameter: `receiptType`
   - Filters GRNs by type

4. **Warehouse Integration Service**
   - Processes stock only for `ReceiptType.STOCK`

---

## 🐛 Troubleshooting

### Error persists after migration?

1. **Clear application cache/restart**
   ```bash
   # Restart Spring Boot application
   ```

2. **Verify database values**
   ```sql
   SELECT DISTINCT receipt_type FROM grn;
   ```
   Should only show: `STOCK`, `DIRECT`

3. **Check for null values**
   ```sql
   SELECT * FROM grn WHERE receipt_type IS NULL;
   ```

---

## 📞 Support

If migration fails or errors persist:

1. ✅ Check database backup exists
2. ✅ Verify migration SQL executed successfully
3. ✅ Confirm no old values remain in database
4. ✅ Restart Spring Boot application
5. ✅ Test API with simple request (no filters)

---

**Status:** Ready to execute
**Priority:** High - Blocking GRN APIs
**Time Required:** ~5 minutes
