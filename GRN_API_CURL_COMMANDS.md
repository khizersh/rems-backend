# CURL Commands for GRN Filter API

## API Endpoint: getGrnsByStatusAndDateRange

**URL:** `POST http://localhost:8080/api/grn/getByStatusAndDateRange`

---

## 🔐 Prerequisites

First, login to get your JWT token:

```bash
curl --location 'http://localhost:8080/api/user/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "your_username",
  "password": "your_password"
}'
```

Copy the token from the response and use it in the commands below by replacing `YOUR_JWT_TOKEN`.

---

## 📋 CURL Commands

### 1. Get All GRNs (No Filters - Pagination Only)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": null,
  "vendorId": null,
  "status": null,
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

**Alternative (omit null fields):**
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 2. Filter by Purchase Order ID Only

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 3. Filter by Vendor ID Only

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "vendorId": 456,
  "page": 0,
  "size": 20,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}'
```

---

### 4. Filter by Status Only (RECEIVED)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "RECEIVED",
  "page": 0,
  "size": 15,
  "sortBy": "receivedDate",
  "sortDir": "desc"
}'
```

---

### 5. Filter by Status Only (PARTIAL)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "PARTIAL",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 6. Filter by Status Only (CANCELLED)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "CANCELLED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 7. Filter by Date Range Only

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 0,
  "size": 50,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 8. Filter by Start Date Only (from date onwards)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "startDate": "2024-02-01T00:00:00",
  "page": 0,
  "size": 25,
  "sortBy": "createdDate",
  "sortDir": "asc"
}'
```

---

### 9. Filter by End Date Only (up to date)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 30,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 10. Filter by PO ID and Status

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "status": "RECEIVED",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 11. Filter by Vendor ID and Status

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "vendorId": 456,
  "status": "RECEIVED",
  "page": 0,
  "size": 20,
  "sortBy": "receivedDate",
  "sortDir": "asc"
}'
```

---

### 12. Filter by Status and Date Range

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-29T23:59:59",
  "page": 0,
  "size": 20,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 13. Filter by PO ID, Vendor ID, and Status

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "page": 0,
  "size": 10,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}'
```

---

### 14. Filter by All Parameters (Complete Example)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 50,
  "sortBy": "receivedDate",
  "sortDir": "desc"
}'
```

---

### 15. Get Second Page with Custom Page Size

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "RECEIVED",
  "page": 1,
  "size": 25,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 16. Sort by GRN Number (Ascending)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "status": "RECEIVED",
  "page": 0,
  "size": 10,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}'
```

---

### 17. Filter GRNs for Current Month

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "startDate": "2024-02-01T00:00:00",
  "endDate": "2024-02-29T23:59:59",
  "page": 0,
  "size": 100,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 18. Filter GRNs for Last 7 Days

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "startDate": "2024-02-11T00:00:00",
  "endDate": "2024-02-18T23:59:59",
  "page": 0,
  "size": 50,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 19. Get Large Result Set (100 records per page)

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "page": 0,
  "size": 100,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

---

### 20. Filter Partial GRNs for Specific Vendor in Date Range

```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "vendorId": 456,
  "status": "PARTIAL",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-06-30T23:59:59",
  "page": 0,
  "size": 20,
  "sortBy": "receivedDate",
  "sortDir": "desc"
}'
```

---

## 📊 Expected Response Format

All successful requests return:

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
            "quantityInvoiced": 0.0,
            "createdBy": "admin",
            "updatedBy": "admin",
            "createdDate": "2024-02-18T10:00:00",
            "updatedDate": "2024-02-18T10:00:00"
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

## 🎯 Quick Reference

### Filter Options (All Optional)
- **poId** - Purchase Order ID (Long)
- **vendorId** - Vendor ID (Long)
- **status** - GRN Status: `RECEIVED`, `PARTIAL`, `CANCELLED`
- **startDate** - Start date (ISO 8601: `YYYY-MM-DDTHH:mm:ss`)
- **endDate** - End date (ISO 8601: `YYYY-MM-DDTHH:mm:ss`)

### Pagination Options
- **page** - Page number (0-based, default: 0)
- **size** - Records per page (default: 10)
- **sortBy** - Field to sort by (default: "createdDate")
- **sortDir** - Sort direction: `asc` or `desc` (default: "desc")

### Common Sort Fields
- `createdDate` - Sort by creation date
- `receivedDate` - Sort by received date
- `grnNumber` - Sort by GRN number
- `status` - Sort by status

---

## 💡 Tips

1. **All filters are optional** - You can use any combination or none at all
2. **Date format** - Use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
3. **End date** - Automatically adjusted to 23:59:59 of the specified day
4. **Null values** - Can be explicitly set to `null` or omitted from the request
5. **Multiple filters** - Use AND logic (all must match)
6. **Windows users** - Replace `\` with `^` for line continuation in Command Prompt
7. **Save token** - Store JWT token in environment variable for reuse

---

## 🔄 Using with Environment Variable (Recommended)

**Set token variable:**
```bash
# Linux/Mac
export JWT_TOKEN="your_actual_jwt_token_here"

# Windows Command Prompt
set JWT_TOKEN=your_actual_jwt_token_here

# Windows PowerShell
$env:JWT_TOKEN="your_actual_jwt_token_here"
```

**Use in CURL:**
```bash
# Linux/Mac
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $JWT_TOKEN" \
--data '{
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}'

# Windows Command Prompt
curl --location "http://localhost:8080/api/grn/getByStatusAndDateRange" ^
--header "Content-Type: application/json" ^
--header "Authorization: Bearer %JWT_TOKEN%" ^
--data "{\"status\": \"RECEIVED\", \"page\": 0, \"size\": 10}"

# Windows PowerShell
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' `
--header 'Content-Type: application/json' `
--header "Authorization: Bearer $env:JWT_TOKEN" `
--data '{
  "status": "RECEIVED",
  "page": 0,
  "size": 10
}'
```

---

## ✅ Testing Checklist

- [ ] Login and get JWT token
- [ ] Test with no filters (get all GRNs)
- [ ] Test with single filter (poId, vendorId, status, date)
- [ ] Test with multiple filters combined
- [ ] Test pagination (different pages and sizes)
- [ ] Test sorting (different fields and directions)
- [ ] Test date ranges (start only, end only, both)
- [ ] Verify response structure
- [ ] Check totalElements and pagination metadata

---

**Last Updated:** February 18, 2026  
**API Version:** 2.0 (Conditional Filters)
