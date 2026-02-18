# GRN Filter API Documentation

## API: Get GRNs with Conditional Filters

### Endpoint
```
POST /api/grn/getByStatusAndDateRange
```

### Description
Fetches GRNs (Goods Receipt Notes) with flexible conditional filtering. All filter parameters are optional:
- **poId** - Filter by Purchase Order ID
- **vendorId** - Filter by Vendor ID  
- **status** - Filter by GRN Status (RECEIVED, PARTIAL, CANCELLED)
- **startDate** - Filter from this date onwards
- **endDate** - Filter up to this date

**If all filters are null**, the API returns all GRNs with pagination only.

### Authentication
Requires JWT token in Authorization header.

### Request Body
```json
{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

### Request Parameters

| Field | Type | Required | Description | Default |
|-------|------|----------|-------------|---------|
| poId | Long | No | Purchase Order ID filter | null |
| vendorId | Long | No | Vendor ID filter | null |
| status | GrnStatus | No | Status of GRN (RECEIVED, PARTIAL, CANCELLED) | null |
| startDate | LocalDateTime | No | Start date for filtering (ISO 8601 format) | null |
| endDate | LocalDateTime | No | End date for filtering (ISO 8601 format) | null |
| page | int | No | Page number (0-based) | 0 |
| size | int | No | Number of records per page | 10 |
| sortBy | String | No | Field to sort by | "createdDate" |
| sortDir | String | No | Sort direction ("asc" or "desc") | "desc" |

### GrnStatus Enum Values
- `RECEIVED` - GRN fully received
- `PARTIAL` - GRN partially received
- `CANCELLED` - GRN cancelled

### Response Structure
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "content": [
      {
        "id": 1,
        "grnNumber": "GRN-20240217-001",
        "orgId": 100,
        "projectId": 50,
        "vendorId": 25,
        "poId": 10,
        "status": "RECEIVED",
        "receivedDate": "2024-02-17T10:30:00",
        "receiptType": "WAREHOUSE_STOCK",
        "warehouseId": 5,
        "directConsumeProjectId": null,
        "createdBy": "admin",
        "updatedBy": "admin",
        "createdDate": "2024-02-17T10:00:00",
        "updatedDate": "2024-02-17T10:00:00",
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
            "createdDate": "2024-02-17T10:00:00",
            "updatedDate": "2024-02-17T10:00:00"
          }
        ]
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Use Cases

#### 1. Get All GRNs (No Filters - Pagination Only)
Fetches all GRNs without any filtering.

**Request:**
```json
{
  "poId": null,
  "vendorId": null,
  "status": null,
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 20,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

#### 2. Get GRNs by Purchase Order ID
Fetches all GRNs for a specific Purchase Order.

**Request:**
```json
{
  "poId": 123,
  "vendorId": null,
  "status": null,
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

#### 3. Get GRNs by Vendor ID
Fetches all GRNs for a specific Vendor.

**Request:**
```json
{
  "poId": null,
  "vendorId": 456,
  "status": null,
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 10,
  "sortBy": "receivedDate",
  "sortDir": "asc"
}
```

#### 4. Get GRNs by Status Only
Fetches all GRNs with specific status.

**Request:**
```json
{
  "poId": null,
  "vendorId": null,
  "status": "RECEIVED",
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 20,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

#### 5. Get GRNs by Status with Date Range
Fetches GRNs with specified status created within a date range.

**Request:**
```json
{
  "poId": null,
  "vendorId": null,
  "status": "PARTIAL",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "asc"
}
```

#### 6. Get GRNs with Multiple Filters
Fetches GRNs matching multiple criteria.

**Request:**
```json
{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "page": 0,
  "size": 10,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}
```

#### 7. Get GRNs by Date Range Only
Fetches all GRNs created within a specific date range.

**Request:**
```json
{
  "poId": null,
  "vendorId": null,
  "status": null,
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 0,
  "size": 50,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

#### 8. Get Cancelled GRNs for a Specific Vendor
Combines vendor and status filters.

**Request:**
```json
{
  "poId": null,
  "vendorId": 789,
  "status": "CANCELLED",
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 15,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}
```

### CURL Examples

#### Example 1: Get All GRNs (No Filters)
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

#### Example 2: Get GRNs by Purchase Order ID
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

#### Example 3: Get GRNs by Vendor and Status
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "vendorId": 456,
  "status": "RECEIVED",
  "page": 0,
  "size": 20
}'
```

#### Example 4: Get GRNs with Date Range
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-28T23:59:59",
  "page": 0,
  "size": 20,
  "sortBy": "receivedDate",
  "sortDir": "desc"
}'
```

#### Example 5: Get GRNs with All Filters
```bash
curl --location 'http://localhost:8080/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--data '{
  "poId": 123,
  "vendorId": 456,
  "status": "RECEIVED",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 0,
  "size": 50,
  "sortBy": "grnNumber",
  "sortDir": "asc"
}'
```

### Postman Request

**Method:** POST  
**URL:** `http://localhost:8080/api/grn/getByStatusAndDateRange`  
**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer YOUR_JWT_TOKEN`

**Body (raw JSON):**
```json
{
  "status": "RECEIVED",
  "startDate": null,
  "endDate": null,
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

### Error Responses

#### System Error
```json
{
  "responseCode": "9999",
  "responseMessage": "System Failure!",
  "data": "Error details..."
}
```

#### Unauthorized
```json
{
  "responseCode": "0003",
  "responseMessage": "Invalid Credentials!",
  "data": null
}
```

**Note:** Since all parameters are optional, there are no "Invalid Parameter" errors for missing filters.

### Features

✅ **All Filters Optional**: Every filter parameter can be null  
✅ **No Filters = All Records**: If all filters are null, returns all GRNs with pagination  
✅ **Multiple Filter Combinations**: Mix and match any filters  
✅ **Purchase Order Filter**: Filter by specific PO ID  
✅ **Vendor Filter**: Filter by specific Vendor ID  
✅ **Status Filter**: Filter by GRN status (RECEIVED, PARTIAL, CANCELLED)  
✅ **Date Range Filter**: Filter by creation date range  
✅ **Flexible Date Filtering**: Use start date only, end date only, or both  
✅ **Pagination Support**: Efficient handling of large datasets  
✅ **Sorting Options**: Sort by any field in ascending or descending order  
✅ **GRN Items Included**: Each GRN includes its items list  
✅ **Auto End of Day**: End date automatically adjusted to 23:59:59  

### Implementation Details

**Repository Layer:**
- Uses JPQL query with all optional parameters
- Null-safe conditional WHERE clauses
- Filters applied only when parameter is not null
- Optimized pagination with Spring Data

**Service Layer:**
- No validation required (all parameters optional)
- Adjusts end date to end of day (23:59:59)
- Fetches GRN items for each GRN
- Returns paginated response with metadata

**Controller Layer:**
- POST endpoint for complex filtering
- Pageable configuration from request
- JWT authentication required

### Database Query Logic

The query applies filters based on what's provided:
1. **poId** (optional): If provided, filters by Purchase Order ID
2. **vendorId** (optional): If provided, filters by Vendor ID
3. **Status** (optional): If provided, filters by GRN status
4. **Start Date** (optional): If provided, filters createdDate >= startDate
5. **End Date** (optional): If provided, filters createdDate <= endDate (adjusted to end of day)
6. **Pagination**: Always applied - page number, size, sort field, and direction

**Filter Combinations:**
- Zero filters → Returns all GRNs
- One filter → Returns GRNs matching that filter
- Multiple filters → Returns GRNs matching ALL provided filters (AND logic)

### Notes

- All filter fields are optional - send null or omit them to ignore that filter
- If all filters are null, API returns all GRNs with pagination
- Date fields should be in ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
- If only startDate is provided, fetches from that date onwards
- If only endDate is provided, fetches up to that date
- End date is automatically adjusted to 23:59:59 to include the entire day
- Multiple filters use AND logic (all provided filters must match)
- Results are ordered by createdDate DESC by default (can be changed via sortBy/sortDir)

### Related APIs

- `POST /api/grn/create` - Create new GRN
- `GET /api/grn/getById/{grnId}` - Get GRN by ID
- `POST /api/grn/getByPoId/{poId}` - Get GRNs by Purchase Order ID
- `POST /api/grn/getGroupedByPoId` - Get GRNs grouped by PO

---

**API Version:** 1.0  
**Last Updated:** February 17, 2026  
**Status:** ✅ Complete and Ready to Use
