# GRN Receipt Type Flow Changes

## Overview

This document describes the changes made to the GRN (Goods Receipt Note) module to support two distinct material flow types: **STOCK** and **DIRECT**.

---

## Receipt Type Enum Changes

### Old Values (Deprecated)
- `WAREHOUSE_STOCK`
- `DIRECT_CONSUME`

### New Values
- `STOCK` - Material received into warehouse inventory
- `DIRECT` - Material directly consumed by project (no inventory entry)

---

## GRN Entity Field Changes

| Old Field Name | New Field Name | Description |
|----------------|----------------|-------------|
| `directConsumeProjectId` | `directProjectId` | Project ID where material is directly consumed |
| `directConsumeProjectName` | `directProjectName` | Project name (transient field for display) |

---

## Business Flow

### STOCK Type (Material goes to Warehouse)

```
PO → GRN (receiptType=STOCK, warehouseId=X) → Warehouse Stock ↑
                                              ↓
                                        Invoice → Payment
                                              ↓
                                    Organization Account ↓
```

**Workflow:**
1. Create GRN with `receiptType: "STOCK"` and `warehouseId`
2. Material is automatically added to warehouse stock
3. Create invoice against GRN
4. Make payment - organization account is debited
5. Stock remains in warehouse until issued to a project

### DIRECT Type (Material goes directly to Project)

```
PO → GRN (receiptType=DIRECT, directProjectId=Y) → NO Stock Entry
                                                    ↓
                                              Invoice → Payment
                                                    ↓
                              Organization Account ↓ + Project Construction Amount ↑
```

**Workflow:**
1. Create GRN with `receiptType: "DIRECT"` and `directProjectId`
2. **No stock entry** is created (material is directly consumed)
3. Create invoice against GRN
4. Make payment:
   - Organization account is debited
   - **Project construction amount is increased** by the payment amount

---

## API Changes

### Create GRN

**Endpoint:** `POST /api/grn/create`

**Request Body for STOCK type:**
```json
{
  "poId": 123,
  "receiptType": "STOCK",
  "warehouseId": 5,
  "directProjectId": null,
  "receivedDate": "2026-03-20T10:00:00",
  "grnItemsList": [
    {
      "poItemId": 1,
      "quantityReceived": 100
    }
  ]
}
```

**Request Body for DIRECT type:**
```json
{
  "poId": 123,
  "receiptType": "DIRECT",
  "warehouseId": null,
  "directProjectId": 10,
  "receivedDate": "2026-03-20T10:00:00",
  "grnItemsList": [
    {
      "poItemId": 1,
      "quantityReceived": 100
    }
  ]
}
```

### GRN Response

```json
{
  "id": 45,
  "grnNumber": "GRN-20260320-001",
  "poId": 123,
  "receiptType": "DIRECT",
  "warehouseId": null,
  "warehouseName": null,
  "directProjectId": 10,
  "directProjectName": "Ghousia Arcade",
  "status": "RECEIVED",
  "invoiceStatus": "NOT_INVOICED",
  "grnItemsList": [...]
}
```

---

## Validation Rules

### For STOCK Type
- `warehouseId` is **REQUIRED**
- `directProjectId` must be **null** (auto-cleared by backend)
- Warehouse must exist and be active

### For DIRECT Type
- `directProjectId` is **REQUIRED**
- `warehouseId` must be **null** (auto-cleared by backend)
- Project must exist

---

## Frontend Integration Guide

### GRN Creation Form

1. **Receipt Type Selector** (Radio or Dropdown)
   - Option: "Stock to Warehouse" → `receiptType: "STOCK"`
   - Option: "Direct to Project" → `receiptType: "DIRECT"`

2. **Conditional Fields:**
   - If STOCK selected → Show warehouse dropdown, hide project dropdown
   - If DIRECT selected → Show project dropdown, hide warehouse dropdown

3. **Field Mapping:**
   ```javascript
   const grnPayload = {
     poId: selectedPoId,
     receiptType: receiptTypeSelected,  // "STOCK" or "DIRECT"
     warehouseId: receiptTypeSelected === "STOCK" ? selectedWarehouseId : null,
     directProjectId: receiptTypeSelected === "DIRECT" ? selectedProjectId : null,
     receivedDate: receivedDate,
     grnItemsList: items
   };
   ```

### GRN List/View Display

Display fields based on receipt type:
```javascript
// For each GRN in list
if (grn.receiptType === "STOCK") {
  display: `Warehouse: ${grn.warehouseName}`
} else if (grn.receiptType === "DIRECT") {
  display: `Direct to Project: ${grn.directProjectName}`
}
```

---

## Filter API Changes

The GRN filter API now supports filtering by `receiptType`:

**Endpoint:** `POST /api/grn/getByStatusAndDateRange`

```json
{
  "orgId": 1,
  "receiptType": "DIRECT",  // Optional: "STOCK", "DIRECT", or null for all
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

---

## Construction Amount Update Logic

When a vendor payment is made for a GRN with `receiptType: "DIRECT"`:

1. Backend checks if the invoice's GRN has `receiptType === "DIRECT"`
2. If yes, finds the project using `grn.directProjectId`
3. Adds the payment amount to:
   - `project.constructionAmount`
   - `project.totalAmount`

**Note:** This happens automatically - no frontend action required.

---

## Migration Notes

### Database Changes
- Column `direct_consume_project_id` renamed to `direct_project_id` in `grn` table
- Enum values in database updated from `WAREHOUSE_STOCK`/`DIRECT_CONSUME` to `STOCK`/`DIRECT`

### Backward Compatibility
- Old enum values should be migrated during deployment
- Frontend should update all references to use new enum values

---

## Summary

| Feature | STOCK | DIRECT |
|---------|-------|--------|
| Warehouse selection | Required | Not applicable |
| Project selection | Not applicable | Required |
| Stock entry created | Yes | No |
| Construction amount updated on payment | No | Yes |
| Material trackable in inventory | Yes | No |

---

## Curl Examples

### Create GRN with STOCK type
```bash
curl --location 'http://localhost:8081/api/grn/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "poId": 7,
  "receiptType": "STOCK",
  "warehouseId": 1,
  "grnItemsList": [
    {"poItemId": 1, "quantityReceived": 50}
  ]
}'
```

### Create GRN with DIRECT type
```bash
curl --location 'http://localhost:8081/api/grn/create' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "poId": 7,
  "receiptType": "DIRECT",
  "directProjectId": 3,
  "grnItemsList": [
    {"poItemId": 1, "quantityReceived": 50}
  ]
}'
```

### Filter GRNs by Receipt Type
```bash
curl --location 'http://localhost:8081/api/grn/getByStatusAndDateRange' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "orgId": 1,
  "receiptType": "DIRECT",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}'
```
