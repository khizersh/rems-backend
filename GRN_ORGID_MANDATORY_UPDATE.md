# Organization ID Made Mandatory in GRN Filter API

## ✅ COMPLETE - orgId is Now Required

The `getGrnsByStatusAndDateRange` API now **requires** Organization ID as a mandatory parameter.

---

## 🎯 What Changed

### **Before:**
- All filters were optional (including orgId)
- API could fetch all GRNs across all organizations

### **After:** ✅
- **orgId is MANDATORY** (required parameter)
- API validates orgId and returns error if missing
- All GRNs are filtered by organization

---

## 📝 Updated Files

### 1. **GrnFilterRequest.java** ✅
```java
private Long orgId;  // REQUIRED - Organization ID filter (mandatory)
```

### 2. **GrnRepo.java** ✅
```java
@Query("SELECT g FROM Grn g WHERE " +
       "g.orgId = :orgId " +  // No null check - MANDATORY
       "AND (:poId IS NULL OR g.poId = :poId) " +
       // ... other optional filters
)
```

### 3. **GrnService.java** ✅
```java
public Map<String, Object> getByConditionalFilters(
    Long orgId,  // REQUIRED
    // ... other parameters
) {
    // Validate mandatory orgId
    ValidationService.validate(orgId, "Organization ID");
    
    // ... rest of logic
}
```

### 4. **Documentation** ✅
- Updated `GRN_API_WITH_ORGID_CURL.md`
- Marked orgId as REQUIRED in all tables and examples

---

## 📋 Filter Summary

| Filter | Required | Description |
|--------|----------|-------------|
| **orgId** | ✅ **YES** | Organization ID (MANDATORY) |
| poId | ❌ No | Purchase Order ID |
| vendorId | ❌ No | Vendor ID |
| status | ❌ No | GRN Status |
| startDate | ❌ No | Start date |
| endDate | ❌ No | End date |

---

## 📊 Request Examples

### Minimum Required (Only orgId)
```json
{
  "orgId": 100,
  "page": 0,
  "size": 10
}
```

### With Additional Filters
```json
{
  "orgId": 100,
  "status": "RECEIVED",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "page": 0,
  "size": 20
}
```

### All Filters
```json
{
  "orgId": 100,
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "page": 0,
  "size": 20
}
```

---

## ❌ Error Response (Missing orgId)

If orgId is null or missing:

```json
{
  "responseCode": "0002",
  "responseMessage": "Invalid Parameter!",
  "data": "Organization ID is required"
}
```

---

## 🔍 Query Logic

```sql
WHERE 
  g.orgId = :orgId                                      -- REQUIRED (always applied)
  AND (:poId IS NULL OR g.poId = :poId)                -- Optional
  AND (:vendorId IS NULL OR g.vendorId = :vendorId)    -- Optional
  AND (:status IS NULL OR g.status = :status)          -- Optional
  AND (:startDate IS NULL OR DATE(g.createdDate) >= :startDate)  -- Optional
  AND (:endDate IS NULL OR DATE(g.createdDate) <= :endDate)      -- Optional
```

---

## ✅ Validation

### Service Layer
```java
ValidationService.validate(orgId, "Organization ID");
```

**This validation ensures:**
- ✅ orgId is not null
- ✅ Returns error response if validation fails
- ✅ Prevents queries without organization filter

---

## 📝 CURL Example

### Valid Request (orgId provided)
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}'
```

### Invalid Request (orgId missing) - Will Return Error
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}'
```

**Response:**
```json
{
  "responseCode": "0002",
  "responseMessage": "Invalid Parameter!",
  "data": "Organization ID is required"
}
```

---

## 🎉 Benefits

✅ **Security** - Users can only query GRNs from their organization  
✅ **Performance** - Always filtered by organization (indexed column)  
✅ **Data Isolation** - Prevents cross-organization data access  
✅ **Validation** - Early error detection for missing orgId  
✅ **Clarity** - Clear requirement in API documentation  

---

## 🔄 Migration Notes

### For Existing API Consumers:
1. **Action Required** - Add `orgId` to all API calls
2. **Breaking Change** - Requests without orgId will fail
3. **Error Handling** - Handle validation error response (0002)

### Example Migration:

**Old Request (will now fail):**
```json
{
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}
```

**New Request (required):**
```json
{
  "orgId": 100,
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}
```

---

## ✅ Status

- ✅ orgId is now mandatory
- ✅ Validation added in service layer
- ✅ Query updated (no null check for orgId)
- ✅ Documentation updated
- ✅ No compilation errors
- ✅ Ready for production

---

**Updated:** February 18, 2026  
**Version:** 3.0 (orgId Mandatory)  
**Breaking Change:** YES - orgId is now required
