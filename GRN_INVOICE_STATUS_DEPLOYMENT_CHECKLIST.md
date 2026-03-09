# GRN Invoice Status Enhancement - Deployment Checklist

## Pre-Deployment Checklist

### Code Review
- [ ] All new files reviewed and approved
  - [ ] GrnInvoiceStatus.java (new enum)
  - [ ] Grn.java (field changed)
  - [ ] GrnService.java (calculation method added)
  - [ ] VendorInvoiceService.java (integration updated)
  - [ ] GrnFilterRequest.java (DTO updated)
  - [ ] GrnRepo.java (query updated)
  - [ ] GrnController.java (parameter updated)

### Testing
- [ ] Unit tests passed (if applicable)
- [ ] Integration tests passed
- [ ] Manual testing completed
- [ ] All test scenarios validated (see TESTING_GUIDE.md)
- [ ] Performance testing done
- [ ] Edge cases tested

### Documentation
- [ ] Implementation summary reviewed
- [ ] API comparison guide reviewed
- [ ] Database migration guide reviewed
- [ ] Testing guide reviewed
- [ ] Frontend team notified of API changes

---

## Deployment Steps

### Step 1: Backup
- [ ] Database backed up
- [ ] Application code backed up
- [ ] Configuration files backed up

**Command**:
```bash
mysqldump -u username -p database_name > backup_pre_grn_status_$(date +%Y%m%d_%H%M%S).sql
```

---

### Step 2: Database Migration

#### Option A: Automated Migration (Liquibase/Flyway)
- [ ] Create migration script
- [ ] Test in staging
- [ ] Execute in production

#### Option B: Manual Migration
- [ ] Connect to database
- [ ] Execute SQL script (see below)
- [ ] Verify migration

**SQL Script**:
```sql
-- Step 1: Add new column
ALTER TABLE grn 
ADD COLUMN invoice_status VARCHAR(50) NOT NULL DEFAULT 'NOT_INVOICED'
AFTER direct_consume_project_id;

-- Step 2: Migrate data (accurate calculation)
UPDATE grn g
SET invoice_status = (
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM vendor_invoice vi WHERE vi.grn_id = g.id)
        THEN 'NOT_INVOICED'
        
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
        
        WHEN EXISTS (
            SELECT 1 
            FROM grn_items gi 
            WHERE gi.grn_id = g.id 
            AND COALESCE(gi.quantity_invoiced, 0) > 0
        ) THEN 'PARTIALLY_INVOICED'
        
        ELSE 'NOT_INVOICED'
    END
);

-- Step 3: Verify migration
SELECT 
    invoice_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM grn), 2) as percentage
FROM grn
GROUP BY invoice_status;

-- Step 4: Drop old column (ONLY after verification)
ALTER TABLE grn 
DROP COLUMN invoice_created;
```

**Verification Queries**:
```sql
-- Check for NULL values (should be 0)
SELECT COUNT(*) FROM grn WHERE invoice_status IS NULL;

-- Check for invalid values (should be 0)
SELECT COUNT(*) FROM grn 
WHERE invoice_status NOT IN ('NOT_INVOICED', 'PARTIALLY_INVOICED', 'FULLY_INVOICED');

-- Verify data integrity
SELECT 
    g.id,
    g.invoice_status,
    COUNT(vi.id) as invoice_count,
    SUM(gi.quantity_invoiced) as total_invoiced,
    SUM(gi.quantity_received) as total_received
FROM grn g
LEFT JOIN vendor_invoice vi ON g.id = vi.grn_id
LEFT JOIN grn_items gi ON g.id = gi.grn_id
GROUP BY g.id, g.invoice_status
LIMIT 10;
```

---

### Step 3: Deploy Application Code

#### Stop Application
```bash
# For systemd service
sudo systemctl stop rems-backend

# For Docker
docker stop rems-backend-container

# For manual
pkill -f "rems-backend"
```

#### Deploy New Code
```bash
# Pull latest code
git pull origin main

# Build application
mvn clean package -DskipTests

# Or copy JAR
cp target/rem-0.1.jar /opt/rems-backend/
```

#### Start Application
```bash
# For systemd service
sudo systemctl start rems-backend

# For Docker
docker start rems-backend-container

# For manual
java -jar /opt/rems-backend/rem-0.1.jar &
```

---

### Step 4: Post-Deployment Verification

#### Health Check
- [ ] Application started successfully
- [ ] No startup errors in logs
- [ ] Health endpoint responding

**Command**:
```bash
curl http://localhost:8081/actuator/health
```

#### API Verification
- [ ] Login API works
- [ ] GRN list API works with new filter
- [ ] Invoice creation works and updates status
- [ ] Status calculation working correctly

**Test Commands**:
```bash
# Get NOT_INVOICED GRNs
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer TOKEN' \
--data '{"orgId": 1, "invoiceStatus": "NOT_INVOICED", "page": 0, "size": 5}'

# Get PARTIALLY_INVOICED GRNs
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer TOKEN' \
--data '{"orgId": 1, "invoiceStatus": "PARTIALLY_INVOICED", "page": 0, "size": 5}'

# Get FULLY_INVOICED GRNs
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer TOKEN' \
--data '{"orgId": 1, "invoiceStatus": "FULLY_INVOICED", "page": 0, "size": 5}'
```

#### Database Verification
```sql
-- Check status distribution after deployment
SELECT 
    invoice_status,
    COUNT(*) as count
FROM grn
GROUP BY invoice_status;

-- Check recent GRNs
SELECT 
    id,
    grn_number,
    invoice_status,
    created_date
FROM grn
ORDER BY created_date DESC
LIMIT 10;
```

---

### Step 5: Monitoring

#### Log Monitoring
```bash
# Tail application logs
tail -f /var/log/rems-backend/application.log

# Search for errors
grep -i "error\|exception" /var/log/rems-backend/application.log | tail -20
```

**Watch for**:
- [ ] No compilation errors
- [ ] No NullPointerExceptions
- [ ] No database connection errors
- [ ] Invoice status calculation working

#### Performance Monitoring
- [ ] Response times normal
- [ ] Database query performance acceptable
- [ ] No memory leaks
- [ ] CPU usage normal

---

## Rollback Procedure

### If Issues Detected

#### Step 1: Stop Application
```bash
sudo systemctl stop rems-backend
```

#### Step 2: Restore Database
```bash
# Restore from backup
mysql -u username -p database_name < backup_pre_grn_status_TIMESTAMP.sql
```

#### Step 3: Restore Previous Code
```bash
# Checkout previous version
git checkout <previous_commit_hash>

# Rebuild
mvn clean package -DskipTests

# Or restore old JAR
cp /backup/rem-0.1.jar.old /opt/rems-backend/rem-0.1.jar
```

#### Step 4: Restart Application
```bash
sudo systemctl start rems-backend
```

#### Step 5: Verify Rollback
```bash
# Check old API works
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer TOKEN' \
--data '{"orgId": 1, "invoiceCreated": true, "page": 0, "size": 5}'
```

---

## Post-Deployment Tasks

### Immediate (Within 1 Hour)
- [ ] Monitor logs for errors
- [ ] Verify key workflows
- [ ] Check database consistency
- [ ] Notify stakeholders of successful deployment

### Short Term (Within 24 Hours)
- [ ] Monitor user feedback
- [ ] Check error rates
- [ ] Verify reporting accuracy
- [ ] Update API documentation in production

### Long Term (Within 1 Week)
- [ ] Review performance metrics
- [ ] Analyze usage patterns
- [ ] Gather user feedback
- [ ] Plan any optimizations

---

## Communication Plan

### Before Deployment
- [ ] Notify frontend team 48 hours in advance
- [ ] Send email to stakeholders about API changes
- [ ] Update API documentation portal
- [ ] Schedule deployment window

### During Deployment
- [ ] Post maintenance notice if downtime expected
- [ ] Keep stakeholders informed of progress
- [ ] Document any issues encountered

### After Deployment
- [ ] Send success notification
- [ ] Share updated API documentation links
- [ ] Provide support for API consumers
- [ ] Document lessons learned

---

## Support Plan

### Support Contacts
- **Backend Team**: backend-support@company.com
- **Database Team**: dba@company.com
- **DevOps Team**: devops@company.com

### Issue Escalation
1. **Level 1**: Check logs and documentation
2. **Level 2**: Contact backend team
3. **Level 3**: Emergency rollback if critical

### Common Issues & Solutions

#### Issue 1: "Cannot resolve symbol 'grn'" compilation error
**Solution**: IDE cache issue, restart IDE or rebuild project

#### Issue 2: NULL invoice_status values
**Solution**: Re-run migration script

#### Issue 3: Status not updating after invoice
**Solution**: Verify `calculateAndUpdateGrnInvoiceStatus()` is being called

#### Issue 4: Performance degradation
**Solution**: Add index on `invoice_status` column
```sql
CREATE INDEX idx_grn_invoice_status ON grn(invoice_status);
```

---

## Success Criteria

Deployment considered successful when:

- [x] Application starts without errors
- [x] All APIs respond correctly
- [x] New filter parameters work
- [x] Status calculation accurate
- [x] No data corruption
- [x] Performance acceptable
- [x] No critical bugs reported
- [x] Users can perform normal workflows

---

## Timeline

| Task | Duration | Responsible |
|------|----------|-------------|
| Pre-deployment testing | 2-4 hours | QA Team |
| Database backup | 15 mins | DBA Team |
| Database migration | 30-60 mins | DBA Team |
| Code deployment | 15 mins | DevOps Team |
| Post-deployment testing | 1-2 hours | QA Team |
| Monitoring | 24 hours | Support Team |

**Total Estimated Downtime**: 45-75 minutes

---

## Sign-Off

### Pre-Deployment
- [ ] Code reviewed by: __________________ Date: __________
- [ ] Testing approved by: ________________ Date: __________
- [ ] DBA approved migration: _____________ Date: __________

### Post-Deployment
- [ ] Deployment successful: ______________ Date: __________
- [ ] Verification passed: ________________ Date: __________
- [ ] Production ready: __________________ Date: __________

---

## Additional Resources

- **Implementation Summary**: `GRN_INVOICE_STATUS_ENHANCEMENT_SUMMARY.md`
- **Database Migration**: `GRN_INVOICE_STATUS_DATABASE_MIGRATION.md`
- **Testing Guide**: `GRN_INVOICE_STATUS_TESTING_GUIDE.md`
- **API Comparison**: `GRN_INVOICE_STATUS_API_COMPARISON.md`

---

## Notes

_Use this space to document any deployment-specific notes or observations_

---

## End of Deployment Checklist
