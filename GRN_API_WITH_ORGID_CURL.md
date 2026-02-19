# GRN Filter API - CURL Commands with Organization ID

## Updated API with Organization ID Filter (REQUIRED)

The `getGrnsByStatusAndDateRange` API now **requires** Organization ID as a mandatory filter with AND condition.

---

## 📋 All Available Filters

| Filter | Type | Required | Description |
|--------|------|----------|-------------|
| **orgId** | Long | **YES** | Organization ID filter (MANDATORY) |
| poId | Long | No | Purchase Order ID filter |
| vendorId | Long | No | Vendor ID filter |
| status | GrnStatus | No | GRN Status (RECEIVED, PARTIAL, CANCELLED) |
| startDate | LocalDate | No | Start date (YYYY-MM-DD) |
| endDate | LocalDate | No | End date (YYYY-MM-DD) |

---

## 🔐 Get JWT Token First

```bash
curl --location 'http://localhost:8080/api/user/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "your_username",
  "password": "your_password"
}'
```

---

## 📝 CURL Examples

### 1. Filter by Organization ID Only
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "page": 0,
  "size": 10
}'
```

### 2. Filter by Organization ID and Status
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "status": "RECEIVED",
  "page": 0,
  "size": 20
}'
```

### 3. Filter by Organization ID and Date Range
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "page": 0,
  "size": 50
}'
```

### 4. Filter by Organization ID, PO ID, and Vendor ID
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "poId": 123,
  "vendorId": 456,
  "page": 0,
  "size": 10
}'
```

### 5. All Filters Combined
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "page": 0,
  "size": 20,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### 6. Get All GRNs (No Filters)
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "page": 0,
  "size": 10
}'
```

### 7. Organization with Status and Date Range
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "status": "RECEIVED",
  "startDate": "2024-02-01",
  "endDate": "2024-02-29",
  "page": 0,
  "size": 15,
  "sortBy": "receivedDate",
  "sortDir": "desc"
}'
```

### 8. Organization with Vendor Filter (Last Month)
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "vendorId": 456,
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "page": 0,
  "size": 25
}'
```

### 9. Organization with PO Filter
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "poId": 123,
  "page": 0,
  "size": 10,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}'
```

### 10. Organization with Multiple Statuses (PARTIAL)
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "orgId": 100,
  "status": "PARTIAL",
  "page": 0,
  "size": 20
}'
```

---

## 📊 Complete Request Body Example

```json
{
  "orgId": 100,
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "page": 0,
  "size": 20,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

---

## 🔍 Filter Logic

Organization ID is **mandatory** and combined with other optional filters using **AND** condition:
```
WHERE 
  g.orgId = :orgId                                      -- MANDATORY (always applied)
  AND (:poId IS NULL OR g.poId = :poId)                -- Optional
  AND (:vendorId IS NULL OR g.vendorId = :vendorId)    -- Optional
  AND (:status IS NULL OR g.status = :status)          -- Optional
  AND (:startDate IS NULL OR DATE(g.createdDate) >= :startDate)  -- Optional
  AND (:endDate IS NULL OR DATE(g.createdDate) <= :endDate)      -- Optional
```

**Organization ID is always required. Other filters are only applied if the parameter is NOT null.**

---

## ✅ Response Structure

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
        "grnItemsList": [...]
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

## 💡 Usage Tips

1. **Organization Filter** - **REQUIRED** - Must provide `orgId` in every request
2. **Combine Filters** - All other filters work together with AND logic
3. **Null Values** - Omit optional filters you don't need (they'll be ignored)
4. **Date Format** - Use `YYYY-MM-DD` format for dates
5. **Pagination** - Always specify page and size for large datasets
6. **Sorting** - Customize sortBy and sortDir as needed
7. **Validation** - API will return error if orgId is missing or null

---

## 🎯 Common Use Cases

### By Organization
Get all GRNs for a specific organization:
```json
{ "orgId": 100 }
```

### By Organization and Vendor
Get all GRNs for a vendor within an organization:
```json
{ "orgId": 100, "vendorId": 456 }
```

### By Organization, Status, and Date
Get received GRNs for an organization in a date range:
```json
{
  "orgId": 100,
  "status": "RECEIVED",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31"
}
```

---

**Updated:** February 18, 2026  
**Version:** 2.2 (Added Organization ID Filter)
