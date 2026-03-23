# Quick Reference - GRN Multi-Invoice API Changes for Frontend

## 🎯 Executive Summary

**What Changed**: GRN invoice tracking upgraded from Boolean flag to 3-state Enum  
**Impact**: Breaking change - All GRN filter APIs updated  
**Action Required**: Update frontend to use new `invoiceStatus` parameter  
**Benefit**: Support multiple invoices per GRN with accurate partial tracking

---

## ⚡ Quick Changes

### 1. API Request Parameter - BREAKING CHANGE

```diff
// Filter GRNs API
POST /api/grn/getByStatusAndDateRange

// OLD Request Body
{
  "orgId": 1,
- "invoiceCreated": true,     // ❌ REMOVED
  "page": 0,
  "size": 10
}

// NEW Request Body
{
  "orgId": 1,
+ "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ NEW
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

### 2. API Response Field - BREAKING CHANGE

```diff
// GRN Object
{
  "id": 1,
  "grnNumber": "GRN-20260301-001",
- "invoiceCreated": true,        // ❌ REMOVED
+ "invoiceStatus": "PARTIALLY_INVOICED",  // ✅ NEW
  "grnItemsList": [
    {
      "id": 1,
      "quantityReceived": 100.0,
+     "quantityInvoiced": 50.0   // ✅ NEW - Track invoiced amount
    }
  ]
}
```

---

## 📋 New Enum Values

### GrnInvoiceStatus (String Enum)
```typescript
type GrnInvoiceStatus = 
  | "NOT_INVOICED"        // No invoices yet
  | "PARTIALLY_INVOICED"  // Some items invoiced
  | "FULLY_INVOICED";     // All items invoiced
```

### Usage in Filters
```javascript
// Get not invoiced GRNs
{ invoiceStatus: "NOT_INVOICED" }

// Get partially invoiced GRNs
{ invoiceStatus: "PARTIALLY_INVOICED" }

// Get fully invoiced GRNs
{ invoiceStatus: "FULLY_INVOICED" }

// Get all GRNs (no filter)
{ invoiceStatus: null }
```

---

## 🔧 Code Examples

### React Hook - Fetch GRNs by Status
```javascript
import { useState, useEffect } from 'react';
import axios from 'axios';

const useGrns = (orgId, invoiceStatus = null) => {
  const [grns, setGrns] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchGrns = async () => {
      setLoading(true);
      try {
        const response = await axios.post(
          '/api/grn/getByStatusAndDateRange',
          {
            orgId,
            invoiceStatus,  // ✅ NEW parameter
            page: 0,
            size: 10,
            sortBy: 'createdDate',
            sortDir: 'desc'
          },
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        );
        setGrns(response.data.data.content);
      } catch (error) {
        console.error('Error:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchGrns();
  }, [orgId, invoiceStatus]);

  return { grns, loading };
};

// Usage
const { grns, loading } = useGrns(1, "PARTIALLY_INVOICED");
```

### Status Badge Component
```jsx
const StatusBadge = ({ status }) => {
  const styles = {
    NOT_INVOICED: 'bg-gray-200 text-gray-800',
    PARTIALLY_INVOICED: 'bg-yellow-200 text-yellow-800',
    FULLY_INVOICED: 'bg-green-200 text-green-800'
  };

  const labels = {
    NOT_INVOICED: '⏳ Not Invoiced',
    PARTIALLY_INVOICED: '⚠️ Partial',
    FULLY_INVOICED: '✅ Completed'
  };

  return (
    <span className={`px-3 py-1 rounded-full text-sm ${styles[status]}`}>
      {labels[status]}
    </span>
  );
};
```

### Filter Buttons
```jsx
const GrnFilters = ({ onFilterChange, currentFilter }) => (
  <div className="flex gap-2">
    <button
      onClick={() => onFilterChange(null)}
      className={currentFilter === null ? 'bg-blue-500 text-white' : 'bg-gray-200'}
    >
      All
    </button>
    <button
      onClick={() => onFilterChange('NOT_INVOICED')}
      className={currentFilter === 'NOT_INVOICED' ? 'bg-gray-500 text-white' : 'bg-gray-200'}
    >
      ⏳ Not Invoiced
    </button>
    <button
      onClick={() => onFilterChange('PARTIALLY_INVOICED')}
      className={currentFilter === 'PARTIALLY_INVOICED' ? 'bg-yellow-500 text-white' : 'bg-gray-200'}
    >
      ⚠️ Partial
    </button>
    <button
      onClick={() => onFilterChange('FULLY_INVOICED')}
      className={currentFilter === 'FULLY_INVOICED' ? 'bg-green-500 text-white' : 'bg-gray-200'}
    >
      ✅ Completed
    </button>
  </div>
);
```

---

## 📊 GRN Item Progress Display

```jsx
const InvoiceProgress = ({ item }) => {
  const percent = (item.quantityInvoiced / item.quantityReceived) * 100;
  
  return (
    <div className="space-y-2">
      <div className="flex justify-between text-sm">
        <span>{item.itemName}</span>
        <span>{item.quantityInvoiced} / {item.quantityReceived}</span>
      </div>
      
      <div className="w-full bg-gray-200 rounded-full h-2">
        <div
          className={`h-2 rounded-full ${
            percent === 100 ? 'bg-green-500' :
            percent > 0 ? 'bg-yellow-500' :
            'bg-gray-400'
          }`}
          style={{ width: `${percent}%` }}
        />
      </div>
      
      <div className="text-xs text-gray-500">
        {percent.toFixed(0)}% invoiced
      </div>
    </div>
  );
};
```

---

## 🚨 Invoice Creation - Validation

```jsx
const CreateInvoice = ({ grn }) => {
  const getAvailableQty = (item) => {
    return item.quantityReceived - (item.quantityInvoiced || 0);
  };

  const validateQuantity = (item, inputQty) => {
    const available = getAvailableQty(item);
    
    if (inputQty > available) {
      alert(`Max quantity: ${available}. Already invoiced: ${item.quantityInvoiced}`);
      return false;
    }
    
    return true;
  };

  return (
    <div>
      {grn.grnItemsList.map(item => {
        const available = getAvailableQty(item);
        
        return (
          <div key={item.id}>
            <label>{item.itemName}</label>
            <p className="text-sm text-gray-600">
              Available: {available} / {item.quantityReceived}
              {item.quantityInvoiced > 0 && (
                <span className="text-yellow-600">
                  ({item.quantityInvoiced} already invoiced)
                </span>
              )}
            </p>
            <input
              type="number"
              max={available}
              placeholder={`Max: ${available}`}
              onChange={(e) => validateQuantity(item, e.target.value)}
            />
          </div>
        );
      })}
    </div>
  );
};
```

---

## 🔄 Invoice APIs (Unchanged)

These APIs work as before, but they now automatically update GRN status:

### Create Invoice
```javascript
POST /api/vendorInvoice/create
{
  "grnId": 1,
  "totalAmount": 25000.00,
  "invoiceDate": "2026-03-01",
  "dueDate": "2026-03-31",
  "invoiceItemList": [
    {
      "grnItemId": 1,
      "quantity": 50.0,
      "rate": 500.0
    }
  ]
}

// ✅ After creation, GRN.invoiceStatus automatically updated
```

### Update Invoice (UNPAID only)
```javascript
PUT /api/vendorInvoice/update/{invoiceId}
{
  "totalAmount": 30000.00,
  "invoiceItemList": [...]
}

// ✅ After update, GRN.invoiceStatus automatically recalculated
```

---

## ✅ Migration Checklist

### Code Changes
- [ ] Replace `invoiceCreated` with `invoiceStatus` in all API calls
- [ ] Update TypeScript types/interfaces
- [ ] Change filter buttons from 2 to 4 options
- [ ] Add status badge component
- [ ] Add invoice progress indicators
- [ ] Update validation for available quantities

### UI/UX Changes
- [ ] Show 3 status badges instead of 2
- [ ] Display invoice progress per item
- [ ] Show "Available to invoice" quantities
- [ ] Add warning for partially invoiced items
- [ ] Show green checkmark for fully invoiced items

### Testing
- [ ] Test filter by NOT_INVOICED
- [ ] Test filter by PARTIALLY_INVOICED
- [ ] Test filter by FULLY_INVOICED
- [ ] Test filter with null (all GRNs)
- [ ] Test invoice creation updates status
- [ ] Test invoice update recalculates status
- [ ] Test over-invoice validation

---

## 🎨 Color Scheme Recommendations

```javascript
const STATUS_COLORS = {
  NOT_INVOICED: {
    bg: 'bg-gray-100',
    text: 'text-gray-800',
    border: 'border-gray-300',
    icon: '⏳'
  },
  PARTIALLY_INVOICED: {
    bg: 'bg-yellow-100',
    text: 'text-yellow-800',
    border: 'border-yellow-300',
    icon: '⚠️'
  },
  FULLY_INVOICED: {
    bg: 'bg-green-100',
    text: 'text-green-800',
    border: 'border-green-300',
    icon: '✅'
  }
};
```

---

## 🐛 Error Handling

### Over-Invoicing Error
```javascript
try {
  await createInvoice(data);
} catch (error) {
  if (error.response?.data?.responseCode === '9998') {
    // Show user-friendly message
    const message = error.response.data.responseMessage;
    if (message.includes('exceeds pending quantity')) {
      alert('Cannot invoice more than available quantity. Please check your inputs.');
    }
  }
}
```

### Update Paid Invoice Error
```javascript
if (invoice.status !== 'UNPAID') {
  return (
    <div className="bg-yellow-100 p-4 rounded">
      ⚠️ Cannot update invoice. Status: {invoice.status}
    </div>
  );
}
```

---

## 📱 Mobile Responsive Considerations

```jsx
// Stack filters vertically on mobile
<div className="flex flex-col md:flex-row gap-2">
  <button>All</button>
  <button>Not Invoiced</button>
  <button>Partial</button>
  <button>Completed</button>
</div>

// Simplify status badge on mobile
<span className="md:hidden">
  {status === 'FULLY_INVOICED' ? '✅' : 
   status === 'PARTIALLY_INVOICED' ? '⚠️' : '⏳'}
</span>
<span className="hidden md:inline">
  <StatusBadge status={status} />
</span>
```

---

## 🔗 API Endpoints Summary

| Endpoint | Method | Change |
|----------|--------|--------|
| `/api/grn/getByStatusAndDateRange` | POST | ⚠️ Request parameter changed |
| `/api/grn/getById/{id}` | GET | ⚠️ Response field changed |
| `/api/vendorInvoice/create` | POST | ✅ No change (auto-updates GRN) |
| `/api/vendorInvoice/update/{id}` | PUT | ✅ No change (auto-updates GRN) |

---

## 📞 Need Help?

### Common Questions

**Q: Can I still use `invoiceCreated`?**  
A: No, it's been removed. You must use `invoiceStatus`.

**Q: What if I pass `null` for `invoiceStatus`?**  
A: Returns all GRNs regardless of invoice status.

**Q: Does the backend automatically update status?**  
A: Yes! When you create/update invoices, GRN status updates automatically.

**Q: Can one GRN have multiple invoices?**  
A: Yes! That's the main benefit of this update.

---

## 📄 Full Documentation

For complete details, see:
- `FRONTEND_INTEGRATION_GUIDE.md` - Complete React examples
- `GRN_INVOICE_STATUS_API_COMPARISON.md` - Detailed API comparison
- `GRN_INVOICE_STATUS_ENHANCEMENT_SUMMARY.md` - Technical details

---

## ✅ Ready to Implement!

**Estimated Time**: 2-4 hours for full frontend integration  
**Complexity**: Medium (breaking change but well-documented)  
**Testing**: Required in dev/staging before production

---

**Document Version**: 1.0  
**Date**: March 1, 2026  
**Status**: Production Ready
