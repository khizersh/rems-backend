# GRN Conditional Filter API - Implementation Summary

## ✅ Implementation Complete

Modified the GRN filter API to support **fully conditional/optional filters** where any or all parameters can be null.

---

## 🎯 What Changed

### **API Endpoint**
```
POST /api/grn/getByStatusAndDateRange
```

### **Key Feature**
**All filters are now optional!** If all values are null, the API returns all GRNs with pagination only.

---

## 📊 Filter Parameters (All Optional)

| Filter | Type | Description |
|--------|------|-------------|
| **poId** | Long | Filter by Purchase Order ID |
| **vendorId** | Long | Filter by Vendor ID |
| **status** | GrnStatus | Filter by status (RECEIVED, PARTIAL, CANCELLED) |
| **startDate** | LocalDateTime | Filter from this date onwards |
| **endDate** | LocalDateTime | Filter up to this date |
| **Pagination** | - | page, size, sortBy, sortDir (always applied) |

---

## 🔧 Modified Files

### 1. **GrnFilterRequest.java** (Updated)
Added new optional fields:
```java
private Long poId;              // NEW
private Long vendorId;          // NEW
private GrnStatus status;       // Now optional (was required)
private LocalDateTime startDate; // Optional
private LocalDateTime endDate;   // Optional
```

### 2. **GrnRepo.java** (Updated)
New query method with conditional filters:
```java
@Query("SELECT g FROM Grn g WHERE " +
       "(:poId IS NULL OR g.poId = :poId) " +
       "AND (:vendorId IS NULL OR g.vendorId = :vendorId) " +
       "AND (:status IS NULL OR g.status = :status) " +
       "AND (:startDate IS NULL OR g.createdDate >= :startDate) " +
       "AND (:endDate IS NULL OR g.createdDate <= :endDate) " +
       "ORDER BY g.createdDate DESC")
Page<Grn> findByConditionalFilters(...)
```

### 3. **GrnService.java** (Updated)
New service method:
```java
public Map<String, Object> getByConditionalFilters(
    Long poId,
    Long vendorId,
    GrnStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Pageable pageable)
```

### 4. **GrnController.java** (Already using new method)
Controller calls the new service method with all parameters.

---

## 💡 Use Cases

### **Case 1: Get All GRNs**
```json
{
  "poId": null,
  "vendorId": null,
  "status": null,
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 10
}
```
**Result:** Returns ALL GRNs with pagination

### **Case 2: Filter by PO Only**
```json
{
  "poId": 123,
  "page": 0,
  "size": 10
}
```
**Result:** Returns all GRNs for PO ID 123

### **Case 3: Filter by Vendor and Status**
```json
{
  "vendorId": 456,
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}
```
**Result:** Returns received GRNs for vendor 456

### **Case 4: Filter by Date Range Only**
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 0,
  "size": 10
}
```
**Result:** Returns all GRNs created in January 2024

### **Case 5: Multiple Filters**
```json
{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 10
}
```
**Result:** Returns GRNs matching ALL filters (AND logic)

---

## 🎨 Filter Logic

### **How It Works**
Each filter is checked:
- **If parameter is NULL** → Filter is ignored
- **If parameter has value** → Filter is applied

**Example Query Behavior:**
```sql
SELECT * FROM grn 
WHERE 
  (poId IS NULL OR grn.po_id = poId)           -- Applied only if poId provided
  AND (vendorId IS NULL OR grn.vendor_id = vendorId)  -- Applied only if vendorId provided
  AND (status IS NULL OR grn.status = status)         -- Applied only if status provided
  AND (startDate IS NULL OR grn.created_date >= startDate)  -- Applied only if startDate provided
  AND (endDate IS NULL OR grn.created_date <= endDate)      -- Applied only if endDate provided
ORDER BY grn.created_date DESC
```

---

## 📋 CURL Examples

### Example 1: Get All GRNs (No Filters)
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "page": 0,
  "size": 10
}'
```

### Example 2: Filter by PO ID
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "page": 0,
  "size": 10
}'
```

### Example 3: Filter by Multiple Parameters
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 20
}'
```

---

## 🔍 Response Structure

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "content": [
      {
        "id": 1,
        "grnNumber": "GRN-20240218-001",
        "orgId": 100,
        "projectId": 50,
        "vendorId": 456,
        "poId": 123,
        "status": "RECEIVED",
        "receivedDate": "2024-02-18T10:30:00",
        "receiptType": "WAREHOUSE_STOCK",
        "warehouseId": 5,
        "directConsumeProjectId": null,
        "createdBy": "admin",
        "updatedBy": "admin",
        "createdDate": "2024-02-18T10:00:00",
        "updatedDate": "2024-02-18T10:00:00",
        "grnItemsList": [
          {
            "id": 1,
            "grnId": 1,
            "poItemId": 20,
            "itemId": 30,
            "quantityReceived": 100.0,
            "quantityInvoiced": 0.0
          }
        ]
      }
    ],
    "totalElements": 150,
    "totalPages": 15,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

## ✅ Benefits

✅ **Maximum Flexibility** - Use any combination of filters  
✅ **No Required Fields** - All filters optional (except pagination)  
✅ **All Records Available** - Can fetch everything if needed  
✅ **Efficient Queries** - Only applies filters that are provided  
✅ **Clean API Design** - Single endpoint for all filtering needs  
✅ **AND Logic** - Multiple filters narrow down results  
✅ **Pagination Always Works** - Even with no filters  

---

## 📚 Documentation

Complete documentation updated:
- **File:** `GRN_FILTER_API_DOCUMENTATION.md`
- **Includes:** 
  - All filter combinations
  - 8 use case examples
  - 5 CURL examples
  - Request/response formats
  - Filter logic explanation
  - Error handling

---

## 🎯 Summary

**Before:**
- Status was required
- Only status + date range filtering

**After:**
- ✅ All filters optional (poId, vendorId, status, dates)
- ✅ No filters = all records with pagination
- ✅ Any combination of filters works
- ✅ Flexible and powerful filtering

---

## 🚀 Status

✅ **Implementation Complete**  
✅ **No Compilation Errors**  
✅ **Documentation Updated**  
✅ **Ready to Use**

The API now supports fully conditional filtering exactly as requested! 🎉

---

**Last Updated:** February 18, 2026  
**Version:** 2.0 (Conditional Filters)
