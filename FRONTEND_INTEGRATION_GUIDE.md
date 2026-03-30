# Frontend Integration Guide - GRN Multi-Invoice Support

## Overview
This document provides complete integration details for the **GRN Invoice Status Enhancement** feature that enables multiple invoices per GRN with accurate partial invoicing tracking.

**Target Audience**: Frontend Developers, React Developers  
**Date**: March 1, 2026  
**Version**: 1.0

---

## 🎯 What Changed

### Old System (Boolean Flag)
- ❌ Only knew if GRN had "any invoice" (true/false)
- ❌ Could not track partial invoicing
- ❌ Could not support multiple invoices properly

### New System (Enum Status)
- ✅ Three accurate states: NOT_INVOICED, PARTIALLY_INVOICED, FULLY_INVOICED
- ✅ Tracks exact invoicing progress per item
- ✅ Supports unlimited invoices per GRN
- ✅ Prevents over-invoicing automatically

---

## 🔄 Breaking Changes

### API Request Parameter Change

#### ❌ OLD (No longer works)
```javascript
// Filter GRNs - OLD API
const request = {
  orgId: 1,
  invoiceCreated: true,  // ❌ REMOVED - Boolean
  page: 0,
  size: 10
};
```

#### ✅ NEW (Required)
```javascript
// Filter GRNs - NEW API
const request = {
  orgId: 1,
  invoiceStatus: "PARTIALLY_INVOICED",  // ✅ NEW - Enum
  page: 0,
  size: 10,
  sortBy: "createdDate",
  sortDir: "desc"
};
```

### API Response Field Change

#### ❌ OLD Response
```javascript
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "invoiceCreated": true,  // ❌ REMOVED
    "grnItemsList": [...]
  }
}
```

#### ✅ NEW Response
```javascript
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ NEW
    "grnItemsList": [
      {
        "id": 1,
        "itemName": "Cement",
        "quantityReceived": 100.0,
        "quantityInvoiced": 50.0  // Track invoiced vs received
      }
    ]
  }
}
```

---

## 📋 New Invoice Status Enum

### TypeScript/JavaScript Type Definition
```typescript
// Add this to your types file
export type GrnInvoiceStatus = 
  | "NOT_INVOICED"        // No invoices created yet
  | "PARTIALLY_INVOICED"  // Some items invoiced, some pending
  | "FULLY_INVOICED";     // All items fully invoiced

// GRN Interface
export interface Grn {
  id: number;
  grnNumber: string;
  status: "RECEIVED" | "CANCELLED";  // Physical receipt status
  invoiceStatus: GrnInvoiceStatus;    // NEW: Invoice billing status
  orgId: number;
  projectId: number;
  vendorId: number;
  poId: number;
  receivedDate: string;
  createdDate: string;
  updatedDate: string;
  // Transient fields
  projectName?: string;
  vendorName?: string;
  poNumber?: string;
  grnItemsList?: GrnItem[];
}

// GRN Item Interface
export interface GrnItem {
  id: number;
  grnId: number;
  itemId: number;
  itemName?: string;
  quantityReceived: number;
  quantityInvoiced: number;  // NEW: Track invoiced quantity
  createdDate: string;
  updatedDate: string;
}

// Filter Request
export interface GrnFilterRequest {
  orgId: number;                           // Required
  poId?: number | null;
  vendorId?: number | null;
  status?: "RECEIVED" | "CANCELLED" | null;
  startDate?: string | null;
  endDate?: string | null;
  invoiceStatus?: GrnInvoiceStatus | null; // NEW: Replaced invoiceCreated
  page: number;
  size: number;
  sortBy: string;
  sortDir: "asc" | "desc";
}
```

---

## 🔌 API Endpoints

### Base URL
```
http://localhost:8081/api
```

### Authentication
All endpoints require JWT token:
```javascript
headers: {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
}
```

---

## 📡 API 1: Get GRNs with Filters

### Endpoint
```
POST /api/grn/getByStatusAndDateRange
```

### Request Body
```typescript
interface GetGrnsRequest {
  orgId: number;                           // REQUIRED
  poId?: number | null;                    // Optional
  vendorId?: number | null;                // Optional
  status?: "RECEIVED" | "CANCELLED" | null;
  startDate?: string | null;               // Format: "YYYY-MM-DD"
  endDate?: string | null;                 // Format: "YYYY-MM-DD"
  invoiceStatus?: GrnInvoiceStatus | null; // NEW: Filter by invoice status
  page: number;
  size: number;
  sortBy: string;
  sortDir: "asc" | "desc";
}
```

### Response
```typescript
interface GetGrnsResponse {
  data: {
    content: Grn[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
  responseMessage: string;
  responseCode: string;
}
```

### React/Axios Example - Get NOT_INVOICED GRNs
```javascript
import axios from 'axios';

const getNotInvoicedGRNs = async (orgId, page = 0, size = 10) => {
  try {
    const response = await axios.post(
      'http://localhost:8081/api/grn/getByStatusAndDateRange',
      {
        orgId: orgId,
        poId: null,
        vendorId: null,
        status: null,
        startDate: null,
        endDate: null,
        invoiceStatus: "NOT_INVOICED",  // ✅ NEW: Get uninvoiced GRNs
        page: page,
        size: size,
        sortBy: "createdDate",
        sortDir: "desc"
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching GRNs:', error);
    throw error;
  }
};
```

### React/Axios Example - Get PARTIALLY_INVOICED GRNs
```javascript
const getPartiallyInvoicedGRNs = async (orgId, page = 0, size = 10) => {
  try {
    const response = await axios.post(
      'http://localhost:8081/api/grn/getByStatusAndDateRange',
      {
        orgId: orgId,
        invoiceStatus: "PARTIALLY_INVOICED",  // ✅ Get partial GRNs
        page: page,
        size: size,
        sortBy: "createdDate",
        sortDir: "desc"
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching partially invoiced GRNs:', error);
    throw error;
  }
};
```

### React/Axios Example - Get FULLY_INVOICED GRNs
```javascript
const getFullyInvoicedGRNs = async (orgId, page = 0, size = 10) => {
  try {
    const response = await axios.post(
      'http://localhost:8081/api/grn/getByStatusAndDateRange',
      {
        orgId: orgId,
        invoiceStatus: "FULLY_INVOICED",  // ✅ Get fully invoiced GRNs
        page: page,
        size: size,
        sortBy: "createdDate",
        sortDir: "desc"
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching fully invoiced GRNs:', error);
    throw error;
  }
};
```

### React/Axios Example - Get ALL GRNs (No Filter)
```javascript
const getAllGRNs = async (orgId, page = 0, size = 10) => {
  try {
    const response = await axios.post(
      'http://localhost:8081/api/grn/getByStatusAndDateRange',
      {
        orgId: orgId,
        invoiceStatus: null,  // ✅ null = no filter, get all
        page: page,
        size: size,
        sortBy: "createdDate",
        sortDir: "desc"
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching GRNs:', error);
    throw error;
  }
};
```

---

## 📡 API 2: Get GRN by ID

### Endpoint
```
GET /api/grn/getById/{grnId}
```

### Request
```javascript
const getGRNById = async (grnId) => {
  try {
    const response = await axios.get(
      `http://localhost:8081/api/grn/getById/${grnId}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching GRN:', error);
    throw error;
  }
};
```

### Response
```javascript
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260301-001",
    "status": "RECEIVED",
    "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ NEW
    "orgId": 1,
    "projectId": 20000,
    "vendorId": 3,
    "poId": 7,
    "receivedDate": "2026-03-01T10:30:00",
    "projectName": "Tower A Construction",
    "vendorName": "ABC Suppliers",
    "poNumber": "PO-20260201-001",
    "grnItemsList": [
      {
        "id": 1,
        "grnId": 1,
        "itemId": 15,
        "itemName": "Cement Bags 50kg",
        "quantityReceived": 100.0,
        "quantityInvoiced": 50.0  // ✅ NEW: Track invoiced
      },
      {
        "id": 2,
        "grnId": 1,
        "itemId": 16,
        "itemName": "Steel Rods 12mm",
        "quantityReceived": 50.0,
        "quantityInvoiced": 0.0   // ✅ Not yet invoiced
      }
    ]
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 📡 API 3: Create Vendor Invoice

### Endpoint
```
POST /api/vendorInvoice/create
```

### Request Body
```typescript
interface CreateInvoiceRequest {
  grnId: number;
  invoiceNumber?: string;     // Optional, auto-generated if not provided
  totalAmount: number;
  invoiceDate?: string;       // Optional, defaults to today
  dueDate?: string;
  invoiceItemList: InvoiceItem[];
}

interface InvoiceItem {
  grnItemId: number;          // ID from grn_items table
  quantity: number;           // Must not exceed available quantity
  rate: number;
}
```

### React/Axios Example
```javascript
const createInvoice = async (invoiceData) => {
  try {
    const response = await axios.post(
      'http://localhost:8081/api/vendorInvoice/create',
      {
        grnId: invoiceData.grnId,
        invoiceNumber: invoiceData.invoiceNumber, // Optional
        totalAmount: invoiceData.totalAmount,
        invoiceDate: invoiceData.invoiceDate || "2026-03-01",
        dueDate: invoiceData.dueDate,
        invoiceItemList: invoiceData.items.map(item => ({
          grnItemId: item.grnItemId,
          quantity: item.quantity,
          rate: item.rate
        }))
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    // ✅ Backend automatically updates GRN.invoiceStatus
    return response.data;
  } catch (error) {
    console.error('Error creating invoice:', error);
    throw error;
  }
};

// Usage Example - Create partial invoice
const invoiceData = {
  grnId: 1,
  totalAmount: 25000.00,
  invoiceDate: "2026-03-01",
  dueDate: "2026-03-31",
  items: [
    {
      grnItemId: 1,    // First item
      quantity: 50.0,   // Invoice 50 out of 100
      rate: 500.0
    }
    // Note: Not invoicing second item yet
  ]
};

await createInvoice(invoiceData);
// After this, GRN status will automatically become PARTIALLY_INVOICED
```

### Success Response
```javascript
{
  "data": null,
  "responseMessage": "Invoice created successfully",
  "responseCode": "0000"
}
```

### Error Response (Over-Invoicing)
```javascript
{
  "data": null,
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 1",
  "responseCode": "9998"
}
```

---

## 📡 API 4: Update Vendor Invoice (UNPAID Only)

### Endpoint
```
PUT /api/vendorInvoice/update/{invoiceId}
```

### Request Body
```typescript
interface UpdateInvoiceRequest {
  invoiceNumber?: string;
  totalAmount: number;
  invoiceDate?: string;
  dueDate?: string;
  invoiceItemList: InvoiceItem[];
}
```

### React/Axios Example
```javascript
const updateInvoice = async (invoiceId, invoiceData) => {
  try {
    const response = await axios.put(
      `http://localhost:8081/api/vendorInvoice/update/${invoiceId}`,
      {
        totalAmount: invoiceData.totalAmount,
        invoiceDate: invoiceData.invoiceDate,
        dueDate: invoiceData.dueDate,
        invoiceItemList: invoiceData.items.map(item => ({
          grnItemId: item.grnItemId,
          quantity: item.quantity,
          rate: item.rate
        }))
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    // ✅ Backend automatically recalculates GRN.invoiceStatus
    return response.data;
  } catch (error) {
    console.error('Error updating invoice:', error);
    throw error;
  }
};
```

**Important**: Only invoices with status `UNPAID` can be updated. Once payment is recorded, invoice becomes read-only.

---

## 📡 API 5: Get Invoice by ID

### Endpoint
```
GET /api/vendorInvoice/getById/{invoiceId}
```

### React/Axios Example
```javascript
const getInvoiceById = async (invoiceId) => {
  try {
    const response = await axios.get(
      `http://localhost:8081/api/vendorInvoice/getById/${invoiceId}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    console.error('Error fetching invoice:', error);
    throw error;
  }
};
```

### Response
```javascript
{
  "data": {
    "id": 1,
    "invoiceNumber": "INV-20260301-001",
    "status": "UNPAID",
    "totalAmount": 25000.00,
    "paidAmount": 0.00,
    "pendingAmount": 25000.00,
    "grnId": 1,
    "invoiceDate": "2026-03-01",
    "dueDate": "2026-03-31",
    "grnNumber": "GRN-20260301-001",
    "projectName": "Tower A",
    "vendorName": "ABC Suppliers",
    "poNumber": "PO-20260201-001",
    "invoiceItemList": [
      {
        "id": 1,
        "grnItemId": 1,
        "itemName": "Cement Bags 50kg",
        "quantity": 50.0,
        "rate": 500.0,
        "amount": 25000.0
      }
    ]
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 🎨 UI/UX Recommendations

### 1. GRN List Page - Status Badge

Show visual indicators for invoice status:

```jsx
import React from 'react';

const GrnInvoiceStatusBadge = ({ status }) => {
  const getStatusConfig = (status) => {
    switch (status) {
      case 'NOT_INVOICED':
        return {
          label: 'Not Invoiced',
          color: 'bg-gray-200 text-gray-800',
          icon: '⏳'
        };
      case 'PARTIALLY_INVOICED':
        return {
          label: 'Partially Invoiced',
          color: 'bg-yellow-200 text-yellow-800',
          icon: '⚠️'
        };
      case 'FULLY_INVOICED':
        return {
          label: 'Fully Invoiced',
          color: 'bg-green-200 text-green-800',
          icon: '✅'
        };
      default:
        return {
          label: 'Unknown',
          color: 'bg-gray-200 text-gray-800',
          icon: '❓'
        };
    }
  };

  const config = getStatusConfig(status);

  return (
    <span className={`px-3 py-1 rounded-full text-sm font-medium ${config.color}`}>
      {config.icon} {config.label}
    </span>
  );
};

export default GrnInvoiceStatusBadge;
```

### 2. GRN Details Page - Invoice Progress

Show invoicing progress per item:

```jsx
import React from 'react';

const GrnItemInvoiceProgress = ({ grnItems }) => {
  const calculateProgress = (quantityInvoiced, quantityReceived) => {
    if (quantityReceived === 0) return 0;
    return (quantityInvoiced / quantityReceived) * 100;
  };

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">Invoice Progress</h3>
      {grnItems.map((item) => {
        const progress = calculateProgress(item.quantityInvoiced, item.quantityReceived);
        
        return (
          <div key={item.id} className="border rounded-lg p-4">
            <div className="flex justify-between mb-2">
              <span className="font-medium">{item.itemName}</span>
              <span className="text-sm text-gray-600">
                {item.quantityInvoiced} / {item.quantityReceived} invoiced
              </span>
            </div>
            
            {/* Progress Bar */}
            <div className="w-full bg-gray-200 rounded-full h-2.5">
              <div
                className={`h-2.5 rounded-full ${
                  progress === 100 ? 'bg-green-500' :
                  progress > 0 ? 'bg-yellow-500' :
                  'bg-gray-400'
                }`}
                style={{ width: `${progress}%` }}
              />
            </div>
            
            <div className="flex justify-between mt-1 text-xs text-gray-500">
              <span>{progress.toFixed(1)}% invoiced</span>
              <span>
                {item.quantityReceived - item.quantityInvoiced} remaining
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default GrnItemInvoiceProgress;
```

### 3. Create Invoice Form - Available Quantity Validation

```jsx
import React, { useState, useEffect } from 'react';

const CreateInvoiceForm = ({ grn }) => {
  const [invoiceItems, setInvoiceItems] = useState([]);

  // Calculate available quantity for each item
  const getAvailableQuantity = (grnItem) => {
    return grnItem.quantityReceived - (grnItem.quantityInvoiced || 0);
  };

  const handleQuantityChange = (index, newQuantity, grnItem) => {
    const available = getAvailableQuantity(grnItem);
    
    if (newQuantity > available) {
      alert(`Cannot invoice more than available quantity (${available})`);
      return;
    }

    const updatedItems = [...invoiceItems];
    updatedItems[index].quantity = newQuantity;
    setInvoiceItems(updatedItems);
  };

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">Create Invoice</h3>
      
      {grn.grnItemsList.map((grnItem, index) => {
        const available = getAvailableQuantity(grnItem);
        
        return (
          <div key={grnItem.id} className="border rounded p-4">
            <div className="flex justify-between mb-2">
              <span className="font-medium">{grnItem.itemName}</span>
              <span className="text-sm text-gray-600">
                Available: {available} / {grnItem.quantityReceived}
              </span>
            </div>
            
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Quantity</label>
                <input
                  type="number"
                  min="0"
                  max={available}
                  step="0.01"
                  placeholder={`Max: ${available}`}
                  className="w-full border rounded px-3 py-2"
                  onChange={(e) => handleQuantityChange(index, parseFloat(e.target.value), grnItem)}
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Rate</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="Rate"
                  className="w-full border rounded px-3 py-2"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Amount</label>
                <input
                  type="number"
                  disabled
                  className="w-full border rounded px-3 py-2 bg-gray-100"
                />
              </div>
            </div>
            
            {available === 0 && (
              <p className="text-sm text-green-600 mt-2">
                ✅ Fully invoiced
              </p>
            )}
            
            {available > 0 && grnItem.quantityInvoiced > 0 && (
              <p className="text-sm text-yellow-600 mt-2">
                ⚠️ Partially invoiced ({grnItem.quantityInvoiced} already invoiced)
              </p>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default CreateInvoiceForm;
```

### 4. GRN List Filters

```jsx
import React, { useState } from 'react';

const GrnFilters = ({ onFilterChange }) => {
  const [filters, setFilters] = useState({
    invoiceStatus: null,
    startDate: '',
    endDate: '',
    vendorId: null,
    poId: null
  });

  const handleStatusChange = (status) => {
    const newFilters = { ...filters, invoiceStatus: status };
    setFilters(newFilters);
    onFilterChange(newFilters);
  };

  return (
    <div className="bg-white p-4 rounded-lg shadow mb-4">
      <h3 className="font-semibold mb-3">Filter GRNs</h3>
      
      {/* Invoice Status Filter */}
      <div className="mb-4">
        <label className="block text-sm font-medium mb-2">Invoice Status</label>
        <div className="flex gap-2">
          <button
            onClick={() => handleStatusChange(null)}
            className={`px-4 py-2 rounded ${
              filters.invoiceStatus === null
                ? 'bg-blue-500 text-white'
                : 'bg-gray-200 text-gray-700'
            }`}
          >
            All
          </button>
          <button
            onClick={() => handleStatusChange('NOT_INVOICED')}
            className={`px-4 py-2 rounded ${
              filters.invoiceStatus === 'NOT_INVOICED'
                ? 'bg-gray-500 text-white'
                : 'bg-gray-200 text-gray-700'
            }`}
          >
            ⏳ Not Invoiced
          </button>
          <button
            onClick={() => handleStatusChange('PARTIALLY_INVOICED')}
            className={`px-4 py-2 rounded ${
              filters.invoiceStatus === 'PARTIALLY_INVOICED'
                ? 'bg-yellow-500 text-white'
                : 'bg-gray-200 text-gray-700'
            }`}
          >
            ⚠️ Partial
          </button>
          <button
            onClick={() => handleStatusChange('FULLY_INVOICED')}
            className={`px-4 py-2 rounded ${
              filters.invoiceStatus === 'FULLY_INVOICED'
                ? 'bg-green-500 text-white'
                : 'bg-gray-200 text-gray-700'
            }`}
          >
            ✅ Fully Invoiced
          </button>
        </div>
      </div>
      
      {/* Additional filters... */}
    </div>
  );
};

export default GrnFilters;
```

---

## 🔄 Complete React Component Examples

### Example 1: GRN List with Filters

```jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const GrnListPage = () => {
  const [grns, setGrns] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    size: 10
  });
  const [filters, setFilters] = useState({
    invoiceStatus: null,
    startDate: null,
    endDate: null
  });

  const fetchGRNs = async (page = 0) => {
    setLoading(true);
    try {
      const response = await axios.post(
        'http://localhost:8081/api/grn/getByStatusAndDateRange',
        {
          orgId: 1, // Get from context/auth
          invoiceStatus: filters.invoiceStatus,
          startDate: filters.startDate,
          endDate: filters.endDate,
          page: page,
          size: pagination.size,
          sortBy: 'createdDate',
          sortDir: 'desc'
        },
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );

      setGrns(response.data.data.content);
      setPagination({
        currentPage: response.data.data.currentPage,
        totalPages: response.data.data.totalPages,
        totalElements: response.data.data.totalElements,
        size: response.data.data.pageSize
      });
    } catch (error) {
      console.error('Error fetching GRNs:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGRNs(0);
  }, [filters]);

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">GRN List</h1>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow mb-4">
        <div className="flex gap-2">
          <button
            onClick={() => setFilters({ ...filters, invoiceStatus: null })}
            className="px-4 py-2 rounded bg-gray-200"
          >
            All
          </button>
          <button
            onClick={() => setFilters({ ...filters, invoiceStatus: 'NOT_INVOICED' })}
            className="px-4 py-2 rounded bg-gray-200"
          >
            Not Invoiced
          </button>
          <button
            onClick={() => setFilters({ ...filters, invoiceStatus: 'PARTIALLY_INVOICED' })}
            className="px-4 py-2 rounded bg-yellow-200"
          >
            Partial
          </button>
          <button
            onClick={() => setFilters({ ...filters, invoiceStatus: 'FULLY_INVOICED' })}
            className="px-4 py-2 rounded bg-green-200"
          >
            Fully Invoiced
          </button>
        </div>
      </div>

      {/* GRN Table */}
      {loading ? (
        <div>Loading...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left">GRN Number</th>
                <th className="px-6 py-3 text-left">PO Number</th>
                <th className="px-6 py-3 text-left">Vendor</th>
                <th className="px-6 py-3 text-left">Invoice Status</th>
                <th className="px-6 py-3 text-left">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {grns.map((grn) => (
                <tr key={grn.id}>
                  <td className="px-6 py-4">{grn.grnNumber}</td>
                  <td className="px-6 py-4">{grn.poNumber}</td>
                  <td className="px-6 py-4">{grn.vendorName}</td>
                  <td className="px-6 py-4">
                    <GrnInvoiceStatusBadge status={grn.invoiceStatus} />
                  </td>
                  <td className="px-6 py-4">
                    <button className="text-blue-600 hover:underline">
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          <div className="px-6 py-4 flex justify-between items-center border-t">
            <div>
              Showing {pagination.currentPage * pagination.size + 1} to{' '}
              {Math.min((pagination.currentPage + 1) * pagination.size, pagination.totalElements)}{' '}
              of {pagination.totalElements} entries
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => fetchGRNs(pagination.currentPage - 1)}
                disabled={pagination.currentPage === 0}
                className="px-4 py-2 border rounded disabled:opacity-50"
              >
                Previous
              </button>
              <button
                onClick={() => fetchGRNs(pagination.currentPage + 1)}
                disabled={pagination.currentPage >= pagination.totalPages - 1}
                className="px-4 py-2 border rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
```

### Example 2: Create Invoice Form with Validation

```jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const CreateInvoicePage = ({ grnId }) => {
  const [grn, setGrn] = useState(null);
  const [loading, setLoading] = useState(false);
  const [invoiceData, setInvoiceData] = useState({
    invoiceNumber: '',
    invoiceDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    items: []
  });

  useEffect(() => {
    fetchGRNDetails();
  }, [grnId]);

  const fetchGRNDetails = async () => {
    setLoading(true);
    try {
      const response = await axios.get(
        `http://localhost:8081/api/grn/getById/${grnId}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
      
      setGrn(response.data.data);
      
      // Initialize invoice items with available quantities
      const initialItems = response.data.data.grnItemsList.map(item => ({
        grnItemId: item.id,
        itemName: item.itemName,
        quantityReceived: item.quantityReceived,
        quantityInvoiced: item.quantityInvoiced || 0,
        availableQuantity: item.quantityReceived - (item.quantityInvoiced || 0),
        quantity: 0,
        rate: 0,
        amount: 0
      }));
      
      setInvoiceData({ ...invoiceData, items: initialItems });
    } catch (error) {
      console.error('Error fetching GRN:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleItemChange = (index, field, value) => {
    const updatedItems = [...invoiceData.items];
    updatedItems[index][field] = parseFloat(value) || 0;
    
    // Validate quantity
    if (field === 'quantity' && value > updatedItems[index].availableQuantity) {
      alert(`Cannot invoice more than ${updatedItems[index].availableQuantity}`);
      return;
    }
    
    // Calculate amount
    if (field === 'quantity' || field === 'rate') {
      updatedItems[index].amount = updatedItems[index].quantity * updatedItems[index].rate;
    }
    
    setInvoiceData({ ...invoiceData, items: updatedItems });
  };

  const calculateTotalAmount = () => {
    return invoiceData.items.reduce((sum, item) => sum + item.amount, 0);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Filter items with quantity > 0
    const itemsToInvoice = invoiceData.items.filter(item => item.quantity > 0);
    
    if (itemsToInvoice.length === 0) {
      alert('Please add at least one item with quantity');
      return;
    }
    
    setLoading(true);
    try {
      const response = await axios.post(
        'http://localhost:8081/api/vendorInvoice/create',
        {
          grnId: grnId,
          invoiceNumber: invoiceData.invoiceNumber || undefined,
          totalAmount: calculateTotalAmount(),
          invoiceDate: invoiceData.invoiceDate,
          dueDate: invoiceData.dueDate,
          invoiceItemList: itemsToInvoice.map(item => ({
            grnItemId: item.grnItemId,
            quantity: item.quantity,
            rate: item.rate
          }))
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
      
      if (response.data.responseCode === '0000') {
        alert('Invoice created successfully! GRN status updated automatically.');
        // Redirect or refresh
      }
    } catch (error) {
      console.error('Error creating invoice:', error);
      alert(error.response?.data?.responseMessage || 'Error creating invoice');
    } finally {
      setLoading(false);
    }
  };

  if (!grn) return <div>Loading...</div>;

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Create Invoice</h1>
      
      {/* GRN Info */}
      <div className="bg-blue-50 p-4 rounded-lg mb-4">
        <h2 className="font-semibold mb-2">GRN Details</h2>
        <div className="grid grid-cols-3 gap-4 text-sm">
          <div>
            <span className="text-gray-600">GRN Number:</span> {grn.grnNumber}
          </div>
          <div>
            <span className="text-gray-600">PO Number:</span> {grn.poNumber}
          </div>
          <div>
            <span className="text-gray-600">Vendor:</span> {grn.vendorName}
          </div>
          <div>
            <span className="text-gray-600">Invoice Status:</span>{' '}
            <GrnInvoiceStatusBadge status={grn.invoiceStatus} />
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        {/* Invoice Header */}
        <div className="bg-white p-4 rounded-lg shadow mb-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Invoice Number</label>
              <input
                type="text"
                placeholder="Auto-generated if empty"
                value={invoiceData.invoiceNumber}
                onChange={(e) => setInvoiceData({ ...invoiceData, invoiceNumber: e.target.value })}
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Invoice Date</label>
              <input
                type="date"
                value={invoiceData.invoiceDate}
                onChange={(e) => setInvoiceData({ ...invoiceData, invoiceDate: e.target.value })}
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Due Date</label>
              <input
                type="date"
                value={invoiceData.dueDate}
                onChange={(e) => setInvoiceData({ ...invoiceData, dueDate: e.target.value })}
                className="w-full border rounded px-3 py-2"
              />
            </div>
          </div>
        </div>

        {/* Invoice Items */}
        <div className="bg-white p-4 rounded-lg shadow mb-4">
          <h3 className="font-semibold mb-4">Invoice Items</h3>
          {invoiceData.items.map((item, index) => (
            <div key={item.grnItemId} className="border rounded p-4 mb-4">
              <div className="flex justify-between mb-2">
                <span className="font-medium">{item.itemName}</span>
                <span className="text-sm text-gray-600">
                  Available: {item.availableQuantity} / {item.quantityReceived}
                  {item.quantityInvoiced > 0 && (
                    <span className="text-yellow-600 ml-2">
                      ({item.quantityInvoiced} already invoiced)
                    </span>
                  )}
                </span>
              </div>
              
              <div className="grid grid-cols-4 gap-4">
                <div>
                  <label className="block text-sm mb-1">Quantity</label>
                  <input
                    type="number"
                    min="0"
                    max={item.availableQuantity}
                    step="0.01"
                    value={item.quantity}
                    onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
                    className="w-full border rounded px-3 py-2"
                    disabled={item.availableQuantity === 0}
                  />
                </div>
                <div>
                  <label className="block text-sm mb-1">Rate</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={item.rate}
                    onChange={(e) => handleItemChange(index, 'rate', e.target.value)}
                    className="w-full border rounded px-3 py-2"
                    disabled={item.availableQuantity === 0}
                  />
                </div>
                <div>
                  <label className="block text-sm mb-1">Amount</label>
                  <input
                    type="number"
                    value={item.amount}
                    disabled
                    className="w-full border rounded px-3 py-2 bg-gray-100"
                  />
                </div>
                <div className="flex items-end">
                  {item.availableQuantity === 0 ? (
                    <span className="text-green-600 text-sm">✅ Fully Invoiced</span>
                  ) : (
                    <span className="text-yellow-600 text-sm">⏳ Available</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Total */}
        <div className="bg-white p-4 rounded-lg shadow mb-4">
          <div className="flex justify-end">
            <div className="text-right">
              <div className="text-gray-600">Total Amount:</div>
              <div className="text-2xl font-bold">
                ₹ {calculateTotalAmount().toFixed(2)}
              </div>
            </div>
          </div>
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-4">
          <button
            type="button"
            className="px-6 py-2 border rounded"
            onClick={() => window.history.back()}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading || calculateTotalAmount() === 0}
            className="px-6 py-2 bg-blue-500 text-white rounded disabled:opacity-50"
          >
            {loading ? 'Creating...' : 'Create Invoice'}
          </button>
        </div>
      </form>
    </div>
  );
};
```

---

## 🚨 Error Handling

### Common Error Scenarios

#### Error 1: Over-Invoicing Attempt
```javascript
{
  "responseMessage": "Invoice quantity (150.0) exceeds pending quantity (100.0) for GRN Item: 1",
  "responseCode": "9998"
}
```

**Handle in UI**:
```javascript
try {
  await createInvoice(data);
} catch (error) {
  if (error.response?.data?.responseCode === '9998') {
    alert('Cannot invoice more than available quantity. Please check the quantities.');
  }
}
```

#### Error 2: Update PAID/PARTIAL Invoice
```javascript
{
  "responseMessage": "Only UNPAID invoices can be updated. Current status: PAID",
  "responseCode": "9998"
}
```

**Handle in UI**:
```javascript
// Disable edit button if invoice is not UNPAID
<button
  disabled={invoice.status !== 'UNPAID'}
  className={invoice.status !== 'UNPAID' ? 'opacity-50 cursor-not-allowed' : ''}
>
  {invoice.status !== 'UNPAID' ? 'Cannot Edit (Already Paid)' : 'Edit Invoice'}
</button>
```

---

## ✅ Migration Checklist for Frontend

- [ ] Update GRN filter components to use `invoiceStatus` instead of `invoiceCreated`
- [ ] Update TypeScript/JavaScript types to include `GrnInvoiceStatus` enum
- [ ] Add three new filter buttons: NOT_INVOICED, PARTIALLY_INVOICED, FULLY_INVOICED
- [ ] Update GRN list to show new status badge
- [ ] Add invoice progress indicators on GRN details page
- [ ] Update invoice creation form to show available quantities
- [ ] Add validation for over-invoicing in UI
- [ ] Update invoice edit form to disable for PAID/PARTIAL invoices
- [ ] Test all GRN and invoice workflows
- [ ] Update user documentation/help text

---

## 📊 Dashboard/Analytics Ideas

### 1. Invoice Status Summary Cards
```jsx
const InvoiceSummaryCards = ({ stats }) => (
  <div className="grid grid-cols-4 gap-4">
    <div className="bg-white p-4 rounded shadow">
      <div className="text-gray-600 text-sm">Total GRNs</div>
      <div className="text-2xl font-bold">{stats.total}</div>
    </div>
    <div className="bg-gray-100 p-4 rounded shadow">
      <div className="text-gray-600 text-sm">Not Invoiced</div>
      <div className="text-2xl font-bold text-gray-700">{stats.notInvoiced}</div>
    </div>
    <div className="bg-yellow-100 p-4 rounded shadow">
      <div className="text-yellow-700 text-sm">Partially Invoiced</div>
      <div className="text-2xl font-bold text-yellow-700">{stats.partial}</div>
    </div>
    <div className="bg-green-100 p-4 rounded shadow">
      <div className="text-green-700 text-sm">Fully Invoiced</div>
      <div className="text-2xl font-bold text-green-700">{stats.fullyInvoiced}</div>
    </div>
  </div>
);
```

### 2. Pending Invoice Chart
Track pending invoice value by vendor/project using Chart.js or similar library.

---

## 🔗 Related APIs (Unchanged)

These existing APIs work as before:
- `POST /api/grn/create` - Create GRN
- `GET /api/po/getById/{id}` - Get PO details
- `GET /api/vendorInvoice/getByVendor/{vendorId}` - Get invoices by vendor

---

## 📞 Support & Questions

For backend-related questions or issues:
- Check API response codes
- Review error messages in `responseMessage` field
- Consult backend team if needed

---

## 📝 Summary

### Key Changes for Frontend
1. **Replace** `invoiceCreated` (Boolean) with `invoiceStatus` (Enum)
2. **Add** three filter options instead of two
3. **Show** invoice progress per GRN item
4. **Validate** quantities against available amounts
5. **Handle** new error scenarios

### Benefits
- ✅ Better user experience with clear status indicators
- ✅ Accurate invoicing progress tracking
- ✅ Prevention of over-invoicing at UI level
- ✅ Support for multiple invoices per GRN

---

## End of Frontend Integration Guide

**Document Version**: 1.0  
**Last Updated**: March 1, 2026  
**Backend Version**: Compatible with GRN Invoice Status Enhancement v1.0
