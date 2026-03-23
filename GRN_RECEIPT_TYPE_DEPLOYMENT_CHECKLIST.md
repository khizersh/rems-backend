# GRN Receipt Type Migration - Deployment Checklist

## Pre-Deployment Checklist

### Environment Verification
- [ ] Identify target environment (Development/UAT/Production)
- [ ] Verify database connection credentials
- [ ] Confirm database backup capability
- [ ] Check current `ReceiptType` values in database

### Backup Strategy
- [ ] Create full database backup
- [ ] Verify backup file integrity
- [ ] Store backup in safe location
- [ ] Document backup file name and location

### Testing Environment
- [ ] Test migration on development database first
- [ ] Verify all GRN APIs work after migration
- [ ] Test invoice creation flow
- [ ] Test vendor payment flow

---

## Migration Execution Checklist

### Pre-Migration Steps

1. **Check Current Data**
```sql
-- Count records with old values
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;
```
- [ ] Document current counts
- [ ] Identify records with old values

2. **Identify Affected Records**
```sql
-- Find records that will be updated
SELECT COUNT(*) FROM grn WHERE receipt_type = 'WAREHOUSE_STOCK';
SELECT COUNT(*) FROM grn WHERE receipt_type = 'DIRECT_CONSUME';
```
- [ ] Record count: WAREHOUSE_STOCK → ____
- [ ] Record count: DIRECT_CONSUME → ____

### Migration Steps

3. **Run Migration Script**
- [ ] Use transactional script: `grn_receipt_type_migration_transactional.sql`
- [ ] Execute in database client (MySQL Workbench, phpMyAdmin, etc.)
- [ ] Review results before committing

4. **Verify Migration**
```sql
-- Verify new values
SELECT receipt_type, COUNT(*) as count 
FROM grn 
GROUP BY receipt_type;

-- Check for old values (should be empty)
SELECT * FROM grn 
WHERE receipt_type IN ('WAREHOUSE_STOCK', 'DIRECT_CONSUME');
```
- [ ] Only STOCK and DIRECT values exist
- [ ] No old values remain
- [ ] Record counts match expected totals

5. **Commit Transaction**
- [ ] Review all changes
- [ ] Execute COMMIT
- [ ] Verify commit successful

### Post-Migration Steps

6. **Application Restart**
- [ ] Stop Spring Boot application
- [ ] Clear application cache (if any)
- [ ] Start Spring Boot application
- [ ] Verify application startup logs

7. **API Testing**
- [ ] Test GRN list API
- [ ] Test GRN filter API
- [ ] Test GRN create API (both STOCK and DIRECT types)
- [ ] Test GRN update API
- [ ] Test invoice creation
- [ ] Test vendor payment flow

---

## Test Cases

### TC-001: GRN List API
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
- [ ] ✅ Returns 200 OK
- [ ] ✅ No enum constant errors
- [ ] ✅ Data contains STOCK/DIRECT values

### TC-002: Filter by Receipt Type - STOCK
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "orgId": 1,
  "receiptType": "STOCK",
  "page": 0,
  "size": 10
}'
```
- [ ] ✅ Returns only STOCK type GRNs
- [ ] ✅ warehouseId populated
- [ ] ✅ warehouseName displayed

### TC-003: Filter by Receipt Type - DIRECT
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "orgId": 1,
  "receiptType": "DIRECT",
  "page": 0,
  "size": 10
}'
```
- [ ] ✅ Returns only DIRECT type GRNs
- [ ] ✅ directProjectId populated
- [ ] ✅ directProjectName displayed

### TC-004: Create GRN - STOCK Type
```bash
curl --location 'http://localhost:8081/api/grn/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "poId": 1,
  "receiptType": "STOCK",
  "warehouseId": 1,
  "receivedDate": "2026-03-22T10:00:00",
  "grnItemsList": [{"poItemId": 1, "quantityReceived": 10}]
}'
```
- [ ] ✅ GRN created successfully
- [ ] ✅ Stock entry created in warehouse
- [ ] ✅ Receipt type saved as STOCK

### TC-005: Create GRN - DIRECT Type
```bash
curl --location 'http://localhost:8081/api/grn/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "poId": 1,
  "receiptType": "DIRECT",
  "directProjectId": 1,
  "receivedDate": "2026-03-22T10:00:00",
  "grnItemsList": [{"poItemId": 1, "quantityReceived": 10}]
}'
```
- [ ] ✅ GRN created successfully
- [ ] ✅ No stock entry created
- [ ] ✅ Receipt type saved as DIRECT

### TC-006: Invoice & Payment Flow
- [ ] ✅ Create invoice against STOCK GRN
- [ ] ✅ Create invoice against DIRECT GRN
- [ ] ✅ Make payment for DIRECT GRN invoice
- [ ] ✅ Project construction amount increases

---

## Rollback Plan

### If Migration Fails

1. **Stop Application**
- [ ] Stop Spring Boot application

2. **Rollback Database**
```sql
ROLLBACK;
```
Or restore from backup:
```bash
mysql -u username -p database_name < backup_before_grn_migration.sql
```

3. **Temporary Code Fix** (If needed)
Add old enum values back temporarily:
```java
public enum ReceiptType {
    STOCK,
    DIRECT,
    @Deprecated WAREHOUSE_STOCK,
    @Deprecated DIRECT_CONSUME
}
```

4. **Plan Retry**
- [ ] Identify root cause
- [ ] Fix issue
- [ ] Schedule retry

---

## Sign-Off

### Development Environment
- [ ] Migration completed successfully
- [ ] All test cases passed
- [ ] Signed off by: _____________ Date: _______

### UAT Environment
- [ ] Migration completed successfully
- [ ] All test cases passed
- [ ] Signed off by: _____________ Date: _______

### Production Environment
- [ ] Migration completed successfully
- [ ] All test cases passed
- [ ] Signed off by: _____________ Date: _______

---

## Notes & Issues

### Migration Execution Notes
```
Date: _________
Time: _________
Database: _________
Records Updated: _________
Duration: _________
Issues Encountered: 
_________________________________
_________________________________
```

### Post-Migration Observations
```
API Response Time: _________
Errors Logged: _________
Performance Impact: _________
User Feedback: 
_________________________________
_________________________________
```

---

## Contact Information

**Database Administrator:** _________________
**Backend Developer:** _________________
**DevOps Engineer:** _________________

---

**Migration Status:** 
- [ ] Pending
- [ ] In Progress
- [ ] Completed
- [ ] Failed
- [ ] Rolled Back

**Final Sign-Off:** _____________ Date: _______
