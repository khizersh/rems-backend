# Organization ID Added to GRN Filter API

## ✅ COMPLETE - Organization ID Filter Added

The `getGrnsByStatusAndDateRange` API now includes **Organization ID** filter with AND condition.

---

## 🎯 What Was Added

### **New Filter Parameter:**
- **orgId** (Long) - Optional Organization ID filter

---

## 📝 Updated Files

### 1. **GrnFilterRequest.java** ✅
```java
private Long orgId;  // NEW - Organization ID filter
```

### 2. **GrnRepo.java** ✅
```java
@Query("SELECT g FROM Grn g WHERE " +
       "(:orgId IS NULL OR g.orgId = :orgId) " +  // NEW
       "AND (:poId IS NULL OR g.poId = :poId) " +
       "AND (:vendorId IS NULL OR g.vendorId = :vendorId) " +
       "AND (:status IS NULL OR g.status = :status) " +
       "AND (:startDate IS NULL OR DATE(g.createdDate) >= :startDate) " +
       "AND (:endDate IS NULL OR DATE(g.createdDate) <= :endDate)")
Page<Grn> findByConditionalFilters(
    @Param("orgId") Long orgId,  // NEW parameter
    // ... other parameters
);
```

### 3. **GrnService.java** ✅
```java
public Map<String, Object> getByConditionalFilters(
    Long orgId,  // NEW parameter
    Long poId,
    Long vendorId,
    // ... other parameters
)
```

### 4. **GrnController.java** ✅
```java
return grnService.getByConditionalFilters(
    request.getOrgId(),  // NEW parameter
    request.getPoId(),
    request.getVendorId(),
    // ... other parameters
);
```

---

## 📋 All Available Filters (All Optional)

| Filter | Type | Description |
|--------|------|-------------|
| **orgId** | Long | **NEW** - Organization ID filter |
| poId | Long | Purchase Order ID filter |
| vendorId | Long | Vendor ID filter |
| status | GrnStatus | GRN Status filter |
| startDate | LocalDate | Start date filter |
| endDate | LocalDate | End date filter |

---

## 📊 Example Requests

### Filter by Organization Only
```json
{
  "orgId": 100,
  "page": 0,
  "size": 10
}
```

### Filter by Organization and Status
```json
{
  "orgId": 100,
  "status": "RECEIVED",
  "page": 0,
  "size": 20
}
```

### All Filters Combined
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

## 🔍 Filter Logic

All filters use **AND** condition:
```sql
WHERE 
  (:orgId IS NULL OR g.orgId = :orgId)              -- NEW
  AND (:poId IS NULL OR g.poId = :poId)
  AND (:vendorId IS NULL OR g.vendorId = :vendorId)
  AND (:status IS NULL OR g.status = :status)
  AND (:startDate IS NULL OR DATE(g.createdDate) >= :startDate)
  AND (:endDate IS NULL OR DATE(g.createdDate) <= :endDate)
```

---

## 🎉 Summary

✅ **Organization ID filter added**  
✅ **AND condition implemented**  
✅ **Optional parameter (can be null)**  
✅ **No compilation errors**  
✅ **Backward compatible**  
✅ **Ready to use**

---

## 📚 Documentation

New documentation file created:
- **GRN_API_WITH_ORGID_CURL.md** - Complete CURL examples with orgId

---

**Updated:** February 18, 2026  
**Status:** ✅ Complete
