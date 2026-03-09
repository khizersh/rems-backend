# Postman-Ready CURL Commands - Update Vendor Invoice

## Variables Setup (For Postman)
Set these variables in Postman environment:
- `baseUrl` = `http://localhost:8081`
- `token` = Your JWT token from login

---

## 1. Get UNPAID Invoices (Before Update)

**Purpose**: Find invoices that can be updated

**URL**: `{{baseUrl}}/api/vendorInvoice/1/getByStatus/UNPAID`

**Method**: POST

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body** (raw JSON):
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

---

## 2. Get Invoice Details by ID

**Purpose**: Check current invoice data before update

**URL**: `{{baseUrl}}/api/vendorInvoice/getById/1`

**Method**: GET

**Headers**:
```
Authorization: Bearer {{token}}
```

**Response Example**:
```json
{
  "data": {
    "id": 1,
    "invoiceNumber": "INV-20260301-001",
    "status": "UNPAID",
    "totalAmount": 50000.00,
    "paidAmount": 0.00,
    "pendingAmount": 50000.00,
    "invoiceDate": "2026-03-01",
    "dueDate": "2026-03-31",
    "grnId": 1,
    "vendorId": 3,
    "projectId": 20000,
    "invoiceItemList": [
      {
        "id": 1,
        "grnItemId": 1,
        "quantity": 100.0,
        "rate": 500.0,
        "amount": 50000.0,
        "itemName": "Cement"
      }
    ]
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 3. Update Vendor Invoice ⭐ (MAIN API)

**Purpose**: Update invoice (only if status is UNPAID)

**URL**: `{{baseUrl}}/api/vendorInvoice/update/1`

**Method**: PUT

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body** (raw JSON) - Full Update:
```json
{
  "invoiceNumber": "INV-20260301-001-UPDATED",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-15",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}
```

**Body** (raw JSON) - Minimal Update (Keep Existing Fields):
```json
{
  "totalAmount": 60000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}
```

**Body** (raw JSON) - Multiple Items:
```json
{
  "invoiceNumber": "INV-20260301-002",
  "totalAmount": 95000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-30",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 100.0,
      "rate": 500.0
    },
    {
      "grnItemId": 2,
      "quantity": 50.0,
      "rate": 300.0
    },
    {
      "grnItemId": 3,
      "quantity": 200.0,
      "rate": 200.0
    }
  ]
}
```

**Success Response**:
```json
{
  "data": null,
  "responseMessage": "Invoice updated successfully",
  "responseCode": "0000"
}
```

**Error Response** (Not UNPAID):
```json
{
  "data": null,
  "responseMessage": "Only UNPAID invoices can be updated. Current status: PAID",
  "responseCode": "9998"
}
```

**Error Response** (Quantity Exceeds):
```json
{
  "data": null,
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 1",
  "responseCode": "9998"
}
```

---

## 4. Verify Update

**Purpose**: Confirm the update was successful

**URL**: `{{baseUrl}}/api/vendorInvoice/getById/1`

**Method**: GET

**Headers**:
```
Authorization: Bearer {{token}}
```

---

## 5. Get GRN Details (For Available Quantities)

**Purpose**: Check available quantities before updating

**URL**: `{{baseUrl}}/api/grn/getById/1`

**Method**: GET

**Headers**:
```
Authorization: Bearer {{token}}
```

**Use This To**:
- See GRN items and their quantities
- Check how much is already invoiced
- Calculate remaining quantities

---

## Complete Test Sequence in Postman

### Collection Structure:
```
📁 Vendor Invoice APIs
  📄 1. Login (Get Token)
  📄 2. Get UNPAID Invoices
  📄 3. Get Invoice by ID
  📄 4. Get GRN Details (Optional)
  📄 5. Update Invoice ⭐
  📄 6. Verify Update
  📄 7. Try Update PAID Invoice (Should Fail)
```

---

## Postman Collection JSON

Save this as `VendorInvoice_Update.postman_collection.json`:

```json
{
  "info": {
    "name": "Vendor Invoice - Update API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8081"
    },
    {
      "key": "token",
      "value": "YOUR_JWT_TOKEN"
    },
    {
      "key": "invoiceId",
      "value": "1"
    }
  ],
  "item": [
    {
      "name": "Get UNPAID Invoices",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"page\": 0,\n  \"size\": 10,\n  \"sortBy\": \"createdDate\",\n  \"sortDir\": \"desc\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/vendorInvoice/1/getByStatus/UNPAID",
          "host": ["{{baseUrl}}"],
          "path": ["api", "vendorInvoice", "1", "getByStatus", "UNPAID"]
        }
      }
    },
    {
      "name": "Get Invoice By ID",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/vendorInvoice/getById/{{invoiceId}}",
          "host": ["{{baseUrl}}"],
          "path": ["api", "vendorInvoice", "getById", "{{invoiceId}}"]
        }
      }
    },
    {
      "name": "Update Invoice",
      "request": {
        "method": "PUT",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"invoiceNumber\": \"INV-20260301-001-UPDATED\",\n  \"totalAmount\": 60000.00,\n  \"invoiceDate\": \"2026-03-01\",\n  \"dueDate\": \"2026-04-15\",\n  \"invoiceItemList\": [\n    {\n      \"grnItemId\": 1,\n      \"quantity\": 120.0,\n      \"rate\": 500.0\n    }\n  ]\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/vendorInvoice/update/{{invoiceId}}",
          "host": ["{{baseUrl}}"],
          "path": ["api", "vendorInvoice", "update", "{{invoiceId}}"]
        }
      }
    },
    {
      "name": "Verify Update",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/vendorInvoice/getById/{{invoiceId}}",
          "host": ["{{baseUrl}}"],
          "path": ["api", "vendorInvoice", "getById", "{{invoiceId}}"]
        }
      }
    }
  ]
}
```

---

## Quick Copy-Paste CURLs

### Update with orgId=1, invoiceId=1:
```bash
curl --location --request PUT 'http://localhost:8081/api/vendorInvoice/update/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYW1tYWR2YiIsImlhdCI6MTc3MjI3OTE3MCwiZXhwIjoxNzcyMzE1MTcwfQ.C6KpoAlglki04S31cp-wilGQSkSL5Sgf9CzOnbIvapo' \
--data '{
  "invoiceNumber": "INV-20260301-001-UPDATED",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-15",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}'
```

### Get UNPAID Invoices:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/1/getByStatus/UNPAID' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYW1tYWR2YiIsImlhdCI6MTc3MjI3OTE3MCwiZXhwIjoxNzcyMzE1MTcwfQ.C6KpoAlglki04S31cp-wilGQSkSL5Sgf9CzOnbIvapo' \
--data '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```

### Get Invoice Details:
```bash
curl --location 'http://localhost:8081/api/vendorInvoice/getById/1' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYW1tYWR2YiIsImlhdCI6MTc3MjI3OTE3MCwiZXhwIjoxNzcyMzE1MTcwfQ.C6KpoAlglki04S31cp-wilGQSkSL5Sgf9CzOnbIvapo'
```

---

## Tips for Postman

1. **Save Token**: After login, save JWT token to environment variable `{{token}}`
2. **Dynamic IDs**: Use collection variables for `invoiceId`, `orgId`, etc.
3. **Tests Tab**: Add assertions to verify status codes
4. **Pre-request Script**: Auto-refresh expired tokens
5. **Collection Runner**: Run entire test sequence

---

## Request Body Reference

### Minimal Required Fields:
```json
{
  "totalAmount": 60000.00,
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}
```

### All Fields:
```json
{
  "invoiceNumber": "INV-20260301-001",
  "totalAmount": 60000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-04-15",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 120.0,
      "rate": 500.0
    }
  ]
}
```

---

## Important Notes

⚠️ **Only UNPAID invoices can be updated**  
⚠️ Replace `YOUR_JWT_TOKEN` with actual token from login  
⚠️ Invoice items are completely replaced on update  
⚠️ Quantities validated against GRN received quantities  
⚠️ All operations are transactional  

---

## End of Document
