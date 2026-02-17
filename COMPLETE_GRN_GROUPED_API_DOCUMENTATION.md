# GRN Grouped by PO ID API - Complete Implementation

## API Implementation Complete ✅

The GRN grouped by PO ID API has been successfully implemented with the following components:

### 1. DTO Created
- `GrnByPoGroupedResponseDTO.java` - Response DTO for grouped data

### 2. Repository Method Added
- `findGrnGroupedByPoId()` in `GrnRepo.java` - Native query to group GRNs by PO ID

### 3. Service Method Implemented
- `getGrnGroupedByPoId()` in `GrnService.java` - Business logic with pagination

### 4. Controller Endpoint Added
- `POST /api/grn/getGroupedByPoId` in `GrnController.java`

## CURL Commands for Testing

### 1. Basic Request (First Page)
```bash
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "lastGrnDate",
    "sortDir": "desc"
  }'
```

### 2. Sort by GRN Count (Ascending)
```bash
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 15,
    "sortBy": "totalGrnCount",
    "sortDir": "asc"
  }'
```

### 3. Sort by PO Number
```bash
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 20,
    "sortBy": "poNumber",
    "sortDir": "asc"
  }'
```

### 4. Second Page Request
```bash
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 1,
    "size": 10,
    "sortBy": "lastGrnDate",
    "sortDir": "desc"
  }'
```

### 5. Large Page Size Request
```bash
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 50,
    "sortBy": "poDate",
    "sortDir": "desc"
  }'
```

## Postman Collection

### Request Configuration
- **Method**: POST
- **URL**: `{{base_url}}/api/grn/getGroupedByPoId`
- **Headers**:
  ```
  Content-Type: application/json
  Authorization: Bearer {{jwt_token}}
  ```

### Request Body (Raw JSON)
```json
{
  "id": {{org_id}},
  "page": 0,
  "size": 10,
  "sortBy": "lastGrnDate",
  "sortDir": "desc"
}
```

### Environment Variables
```
base_url: http://localhost:8080
jwt_token: your_actual_jwt_token_here
org_id: 1
```

## Request Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| id | Long | Yes | - | Organization ID |
| page | Integer | No | 0 | Page number (0-based) |
| size | Integer | No | 10 | Number of records per page |
| sortBy | String | No | "lastGrnDate" | Field to sort by |
| sortDir | String | No | "desc" | Sort direction (asc/desc) |

### Available Sort Fields
- `lastGrnDate` - Most recent GRN date
- `firstGrnDate` - First GRN date
- `totalGrnCount` - Number of GRNs per PO
- `poNumber` - Purchase Order number
- `poDate` - Purchase Order date
- `vendorName` - Vendor name
- `projectName` - Project name

## Sample Response
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "poId": 101,
        "poNumber": "PO-20260216-001",
        "vendorId": 5,
        "vendorName": "ABC Construction Materials",
        "projectId": 12,
        "projectName": "Residential Complex Phase 1",
        "poDate": "2026-02-10T10:30:00",
        "totalGrnCount": 3,
        "lastGrnDate": "2026-02-15T14:20:00",
        "firstGrnDate": "2026-02-12T09:15:00"
      },
      {
        "poId": 102,
        "poNumber": "PO-20260216-002", 
        "vendorId": 8,
        "vendorName": "XYZ Hardware Supplies",
        "projectId": null,
        "projectName": null,
        "poDate": "2026-02-11T11:45:00",
        "totalGrnCount": 1,
        "lastGrnDate": "2026-02-14T16:30:00",
        "firstGrnDate": "2026-02-14T16:30:00"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Response Fields Description

### Main Response Structure
- `success`: Boolean indicating API success
- `message`: Response message
- `data`: Contains the actual data and pagination info

### Content Array Fields
- `poId`: Purchase Order unique identifier
- `poNumber`: Purchase Order number/reference
- `vendorId`: Vendor unique identifier (can be null)
- `vendorName`: Vendor name (can be null)
- `projectId`: Project unique identifier (can be null)
- `projectName`: Project name (can be null)
- `poDate`: Purchase Order creation date
- `totalGrnCount`: Total number of GRNs received against this PO
- `lastGrnDate`: Date and time of the most recent GRN
- `firstGrnDate`: Date and time of the first GRN

### Pagination Fields
- `totalElements`: Total number of grouped PO records
- `totalPages`: Total number of pages
- `currentPage`: Current page number (0-based)
- `pageSize`: Number of records per page
- `hasNext`: Boolean indicating if next page exists
- `hasPrevious`: Boolean indicating if previous page exists

## Key Features

1. **Grouping**: Groups all GRNs by Purchase Order ID
2. **Counting**: Shows total count of GRNs per PO
3. **Date Tracking**: Shows first and last GRN dates
4. **Organization Filtering**: Filters by organization ID
5. **Pagination**: Full pagination support
6. **Sorting**: Multiple sort options
7. **Vendor Information**: Includes vendor details
8. **Project Information**: Includes project details (if applicable)

## Error Responses

### Missing Organization ID
```json
{
  "success": false,
  "message": "Organization ID is required"
}
```

### Invalid Parameters
```json
{
  "success": false,
  "message": "Invalid parameter provided"
}
```

### System Error
```json
{
  "success": false,
  "message": "An error occurred while processing the request"
}
```

## Implementation Notes

1. **Authentication**: Requires valid JWT token
2. **Performance**: Uses native SQL query for optimal performance
3. **Null Handling**: Handles null values for vendor and project information
4. **Transactional**: Read-only operation, safe for concurrent access
5. **Pagination**: Implements standard pagination pattern used across the application

## Usage Examples

### Get first 20 POs with most GRNs
```json
{
  "id": 1,
  "page": 0,
  "size": 20,
  "sortBy": "totalGrnCount",
  "sortDir": "desc"
}
```

### Get recent PO activities
```json
{
  "id": 1,
  "page": 0,
  "size": 10,
  "sortBy": "lastGrnDate", 
  "sortDir": "desc"
}
```

### Get alphabetically sorted POs
```json
{
  "id": 1,
  "page": 0,
  "size": 15,
  "sortBy": "poNumber",
  "sortDir": "asc"
}
```
