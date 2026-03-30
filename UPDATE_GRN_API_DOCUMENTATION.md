# Update GRN API Documentation

## Overview
This document provides comprehensive information about the **Update GRN API** that has been added to the Real Estate Management System (REMS) backend.

## API Endpoint
**POST** `/api/grn/update/{grnId}`

### Method: `POST`
### Path Parameter: 
- `grnId` (Long) - Required: The ID of the GRN to be updated

### Authentication
- Requires JWT token in the Authorization header
- User information is extracted from the JWT token

## Functionality

The Update GRN API allows modification of an existing Goods Received Note (GRN) with comprehensive validations:

### Key Features:
1. **Complete GRN Update**: Updates GRN header information and all associated items
2. **Quantity Validation**: Ensures updated quantities don't exceed Purchase Order limits
3. **PO Status Update**: Automatically updates Purchase Order status based on received quantities
4. **Warehouse Integration**: Supports warehouse stock management if receipt type is WAREHOUSE_STOCK
5. **Transaction Safety**: All operations are wrapped in database transactions with rollback on errors

### Validation Rules:
- GRN must exist and not be in CLOSED or CANCELLED status
- Purchase Order must exist and not be CLOSED or CANCELLED
- Cannot change the associated Purchase Order ID
- All items must have valid PO Item IDs
- Quantity received cannot exceed remaining pending quantity for each item
- Reverts previous quantities before applying new ones to ensure accurate calculations

### Business Logic:
1. **Revert Previous Quantities**: Removes previously received quantities from PO items
2. **Validate New Quantities**: Checks new quantities against available pending quantities
3. **Update Items**: Deletes existing GRN items and creates new ones
4. **Update PO Status**: Recalculates PO status (PENDING/PARTIAL/CLOSED) based on received quantities
5. **Warehouse Integration**: Processes stock updates if receipt type is WAREHOUSE_STOCK

## Request Body Structure

```json
{
    "poId": 1,
    "receivedDate": "2024-02-24T10:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 1,
    "directConsumeProjectId": null,
    "grnItemsList": [
        {
            "poItemId": 1,
            "quantityReceived": 50.0
        },
        {
            "poItemId": 2,
            "quantityReceived": 25.0
        }
    ]
}
```

### Field Descriptions:

#### Header Fields:
- **poId** (Long, Required): Purchase Order ID - must match existing GRN's PO ID
- **receivedDate** (LocalDateTime, Optional): Date when goods were received. If not provided, keeps existing date
- **receiptType** (Enum, Optional): Type of receipt - "WAREHOUSE_STOCK" or "DIRECT_CONSUME"
- **warehouseId** (Long, Optional): Required if receiptType is "WAREHOUSE_STOCK"
- **directConsumeProjectId** (Long, Optional): Required if receiptType is "DIRECT_CONSUME"

#### GRN Items:
- **grnItemsList** (Array, Required): List of items being received
  - **poItemId** (Long, Required): Purchase Order Item ID
  - **quantityReceived** (Double, Required): Quantity being received for this item

## CURL Request Examples

### 1. Basic Update GRN (Warehouse Stock)

```bash
curl -X POST "http://localhost:8080/api/grn/update/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "poId": 1,
    "receivedDate": "2024-02-24T14:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 1,
    "grnItemsList": [
        {
            "poItemId": 1,
            "quantityReceived": 100.0
        },
        {
            "poItemId": 2,
            "quantityReceived": 50.0
        }
    ]
}'
```

### 2. Update GRN with Direct Consume

```bash
curl -X POST "http://localhost:8080/api/grn/update/2" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "poId": 5,
    "receivedDate": "2024-02-24T16:00:00",
    "receiptType": "DIRECT_CONSUME",
    "directConsumeProjectId": 3,
    "grnItemsList": [
        {
            "poItemId": 10,
            "quantityReceived": 75.0
        },
        {
            "poItemId": 11,
            "quantityReceived": 25.0
        },
        {
            "poItemId": 12,
            "quantityReceived": 150.0
        }
    ]
}'
```

### 3. Update GRN with Multiple Items

```bash
curl -X POST "http://localhost:8080/api/grn/update/3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "poId": 7,
    "receivedDate": "2024-02-24T09:15:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 2,
    "grnItemsList": [
        {
            "poItemId": 15,
            "quantityReceived": 200.0
        },
        {
            "poItemId": 16,
            "quantityReceived": 300.0
        },
        {
            "poItemId": 17,
            "quantityReceived": 125.0
        },
        {
            "poItemId": 18,
            "quantityReceived": 80.0
        }
    ]
}'
```

### 4. Postman Collection Format

For Postman users, here's the request configuration:

**Method:** POST
**URL:** `{{baseURL}}/api/grn/update/1`
**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{authToken}}
```
**Body (raw JSON):**
```json
{
    "poId": 1,
    "receivedDate": "2024-02-24T14:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 1,
    "grnItemsList": [
        {
            "poItemId": 1,
            "quantityReceived": 100.0
        },
        {
            "poItemId": 2,
            "quantityReceived": 50.0
        }
    ]
}
```

## Response Format

### Success Response (HTTP 200):
```json
{
    "data": null,
    "responseMessage": "GRN updated successfully",
    "responseCode": "0000"
}
```

### Error Responses:

#### Validation Error (HTTP 200):
```json
{
    "data": null,
    "responseMessage": "GRN not found",
    "responseCode": "9998"
}
```

#### Business Logic Error (HTTP 200):
```json
{
    "data": null,
    "responseMessage": "Cannot update closed or cancelled GRN",
    "responseCode": "9998"
}
```

#### Quantity Validation Error (HTTP 200):
```json
{
    "data": null,
    "responseMessage": "GRN quantity (150.0) exceeds pending quantity (100.0) for PO Item: 1",
    "responseCode": "9998"
}
```

#### System Error (HTTP 200):
```json
{
    "data": null,
    "responseMessage": "Database connection failed",
    "responseCode": "9999"
}
```

## Response Codes
- **0000**: Success
- **9998**: Invalid Parameter / Business Logic Error
- **9999**: System Failure

## Integration Notes

### Service Layer Integration:
The update functionality integrates with:
1. **WarehouseIntegrationService**: For stock management when receiptType is WAREHOUSE_STOCK
2. **PO Management**: Updates Purchase Order status and item received quantities
3. **Transaction Management**: Ensures data consistency with rollback capabilities

### Database Operations:
1. Updates GRN header information
2. Deletes and recreates GRN items (handles additions/deletions/modifications)
3. Updates Purchase Order item received quantities
4. Updates Purchase Order status
5. Processes warehouse stock entries (if applicable)

### Error Handling:
- All database operations are transactional
- Automatic rollback on any validation or system error
- Comprehensive error messages for different failure scenarios

## Testing Recommendations

### Test Scenarios:
1. **Happy Path**: Update GRN with valid data
2. **Validation Tests**: 
   - Invalid GRN ID
   - Closed/Cancelled GRN
   - Invalid PO ID
   - Quantity exceeding limits
3. **Edge Cases**:
   - Updating with same quantities
   - Adding new items
   - Removing existing items
   - Changing receipt type
4. **Integration Tests**: Verify warehouse stock updates and PO status changes

### Sample Test Data:
Use the CURL examples above with different scenarios to validate the functionality.

## Security Considerations
- JWT authentication required
- User context extracted from token
- All database operations are audited with user information
- Input validation prevents SQL injection and other security risks

---

*This API follows the existing REMS backend patterns and integrates seamlessly with the current Purchase Management and Warehouse Management modules.*
