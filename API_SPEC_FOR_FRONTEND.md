# Customer Dashboard API Specification

## Quick Reference for Frontend Integration

---

## 🔐 Authentication

All endpoints require JWT token in header:
```
Authorization: Bearer <token>
```

Get token via login:
```javascript
POST /api/user/login
Body: { "username": "user", "password": "pass" }
Response: { "responseCode": "0000", "data": { "token": "..." } }
```

---

## 📊 API Endpoints

### 1. GET `/api/customer/dashboard/summary`

**Purpose**: Get customer overview and KPIs

**Request**: No parameters needed (uses JWT token)

**Response**:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "customerName": "John Doe",
    "nationalId": "12345-6789012-3",
    "contactNo": "+92-300-1234567",
    "email": "john@example.com",
    "totalBookings": 2,
    "totalUnitsBooked": 2,
    "totalAmountPayable": 5000000.00,
    "totalAmountPaid": 2000000.00,
    "totalRemainingAmount": 3000000.00,
    "overdueAmount": 0.0
  }
}
```

**React Hook**:
```javascript
const { data, loading, error } = useQuery(['summary'], () => 
  api.get('/customer/dashboard/summary').then(r => r.data.data)
);
```

---

### 2. GET `/api/customer/dashboard/payment-chart`

**Purpose**: Monthly payment trends for charts

**Request**: No parameters

**Response**:
```json
{
  "responseCode": "0000",
  "data": [
    {
      "month": 1,
      "year": 2024,
      "monthName": "Jan",
      "totalPaidAmount": 150000.00,
      "totalDueAmount": 0.0,
      "cumulativeRemaining": 0.0
    }
  ]
}
```

**Chart.js Usage**:
```javascript
const chartData = {
  labels: data.map(d => `${d.monthName} ${d.year}`),
  datasets: [{
    label: 'Payments',
    data: data.map(d => d.totalPaidAmount)
  }]
};
```

---

### 3. GET `/api/customer/dashboard/payment-modes`

**Purpose**: Payment method distribution (for pie chart)

**Request**: No parameters

**Response**:
```json
{
  "responseCode": "0000",
  "data": [
    {
      "paymentMode": "BANK_TRANSFER",
      "totalAmount": 1500000.00,
      "transactionCount": 15
    },
    {
      "paymentMode": "CASH",
      "totalAmount": 300000.00,
      "transactionCount": 5
    }
  ]
}
```

**Pie Chart Data**:
```javascript
const pieData = {
  labels: data.map(d => d.paymentMode),
  datasets: [{
    data: data.map(d => d.totalAmount)
  }]
};
```

---

### 4. GET `/api/customer/dashboard/recent-payments?limit={n}`

**Purpose**: Recent payment transactions

**Parameters**:
- `limit` (optional, default: 10) - Number of payments to return

**Request**: `?limit=5`

**Response**:
```json
{
  "responseCode": "0000",
  "data": [
    {
      "paymentId": 123,
      "accountId": 45,
      "projectName": "Green Valley",
      "unitSerial": "A-101",
      "totalPaymentAmount": 150000.00,
      "receivedAmount": 150000.00,
      "paidDate": "2024-01-15T10:30:00",
      "paymentStatus": "PAID",
      "paymentDetails": [
        {
          "paymentType": "BANK_TRANSFER",
          "amount": 100000.00,
          "chequeNo": null,
          "chequeDate": null
        }
      ]
    }
  ]
}
```

**Table Rendering**:
```jsx
{payments.map(p => (
  <tr key={p.paymentId}>
    <td>{formatDate(p.paidDate)}</td>
    <td>{p.projectName}</td>
    <td>{p.unitSerial}</td>
    <td>${p.receivedAmount}</td>
    <td>{p.paymentStatus}</td>
  </tr>
))}
```

---

### 5. GET `/api/customer/dashboard/accounts`

**Purpose**: All customer properties/units with status

**Request**: No parameters

**Response**:
```json
{
  "responseCode": "0000",
  "data": [
    {
      "accountId": 45,
      "projectName": "Green Valley",
      "unitSerial": "A-101",
      "unitType": "APARTMENT",
      "totalAmount": 2500000.00,
      "totalPaidAmount": 1000000.00,
      "totalBalanceAmount": 1500000.00,
      "status": "ACTIVE",
      "durationInMonths": 36
    }
  ]
}
```

**Card Display**:
```jsx
{accounts.map(acc => (
  <Card key={acc.accountId}>
    <h3>{acc.projectName} - {acc.unitSerial}</h3>
    <Badge status={acc.status} />
    <div>Paid: ${acc.totalPaidAmount}</div>
    <div>Remaining: ${acc.totalBalanceAmount}</div>
    <ProgressBar 
      percent={(acc.totalPaidAmount/acc.totalAmount)*100} 
    />
  </Card>
))}
```

---

## 🎨 UI Component Mapping

### Dashboard Layout
```
┌─────────────────────────────────────────┐
│  Welcome, [customerName]                │
│  [contactNo] | [email]                  │
├─────────────────────────────────────────┤
│  [Bookings] [Total] [Paid] [Remaining]  │ ← /summary
├──────────────────┬──────────────────────┤
│  Payment Chart   │  Payment Modes Pie   │ ← /payment-chart, /payment-modes
├──────────────────┴──────────────────────┤
│  Recent Payments Table                  │ ← /recent-payments?limit=5
├─────────────────────────────────────────┤
│  My Properties Cards                    │ ← /accounts
└─────────────────────────────────────────┘
```

---

## 📦 Quick Setup (3 Steps)

### Step 1: API Config
```javascript
// api/config.js
import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

api.interceptors.request.use(config => {
  config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`;
  return config;
});
```

### Step 2: Service
```javascript
// services/dashboard.js
import { api } from '../api/config';

export const getDashboardSummary = () =>
  api.get('/customer/dashboard/summary').then(r => r.data.data);

export const getPaymentChart = () =>
  api.get('/customer/dashboard/payment-chart').then(r => r.data.data);

export const getPaymentModes = () =>
  api.get('/customer/dashboard/payment-modes').then(r => r.data.data);

export const getRecentPayments = (limit = 10) =>
  api.get(`/customer/dashboard/recent-payments?limit=${limit}`).then(r => r.data.data);

export const getAccounts = () =>
  api.get('/customer/dashboard/accounts').then(r => r.data.data);
```

### Step 3: Component
```javascript
// pages/Dashboard.jsx
import { useEffect, useState } from 'react';
import * as dashboardService from '../services/dashboard';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    dashboardService.getDashboardSummary()
      .then(setSummary)
      .catch(console.error);
  }, []);

  if (!summary) return <div>Loading...</div>;

  return (
    <div>
      <h1>Welcome, {summary.customerName}</h1>
      {/* Render summary cards, charts, etc. */}
    </div>
  );
}
```

---

## 🔄 Response Format

**All responses follow this structure:**
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": { /* endpoint-specific data */ }
}
```

**Response Codes**:
- `0000` - Success
- `0001` - No data found
- `0002` - Invalid parameter
- `0003` - Invalid credentials
- `9999` - System failure

---

## ⚠️ Error Handling

```javascript
try {
  const data = await getDashboardSummary();
  setData(data);
} catch (error) {
  if (error.response?.status === 401) {
    // Token expired - redirect to login
    window.location.href = '/login';
  } else {
    // Show error message
    setError(error.message);
  }
}
```

---

## 📱 Sample API Calls

### Axios
```javascript
const summary = await axios.get('/customer/dashboard/summary', {
  headers: { Authorization: `Bearer ${token}` }
});
```

### Fetch
```javascript
const response = await fetch('/api/customer/dashboard/summary', {
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const { data } = await response.json();
```

### React Query
```javascript
const { data, isLoading } = useQuery('summary', () =>
  api.get('/customer/dashboard/summary').then(r => r.data.data)
);
```

---

## 🎯 Data Types (TypeScript)

```typescript
interface CustomerSummary {
  customerName: string;
  nationalId: string;
  contactNo: string;
  email: string;
  totalBookings: number;
  totalUnitsBooked: number;
  totalAmountPayable: number;
  totalAmountPaid: number;
  totalRemainingAmount: number;
  overdueAmount: number;
}

interface PaymentChartData {
  month: number;
  year: number;
  monthName: string;
  totalPaidAmount: number;
  totalDueAmount: number;
  cumulativeRemaining: number;
}

interface PaymentModeDistribution {
  paymentMode: string;
  totalAmount: number;
  transactionCount: number;
}

interface RecentPayment {
  paymentId: number;
  accountId: number;
  projectName: string;
  unitSerial: string;
  totalPaymentAmount: number;
  receivedAmount: number;
  paidDate: string;
  paymentStatus: string;
  paymentDetails: PaymentDetail[];
}

interface AccountStatus {
  accountId: number;
  projectName: string;
  unitSerial: string;
  unitType: string;
  totalAmount: number;
  totalPaidAmount: number;
  totalBalanceAmount: number;
  status: string;
  durationInMonths: number;
}
```

---

## ✅ Integration Checklist

- [ ] Setup axios/fetch with base URL
- [ ] Configure auth interceptor for JWT token
- [ ] Create dashboard service with 5 API methods
- [ ] Implement error handling (401/403 redirect)
- [ ] Create summary cards component
- [ ] Add payment chart (Chart.js/Recharts)
- [ ] Add payment mode pie chart
- [ ] Create recent payments table
- [ ] Create accounts/properties cards
- [ ] Add loading states
- [ ] Add error states
- [ ] Test with real data
- [ ] Make responsive for mobile

---

## 🚀 Complete Example

```javascript
// Complete dashboard in 50 lines
import { useEffect, useState } from 'react';
import { api } from './api/config';

export default function CustomerDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/customer/dashboard/summary'),
      api.get('/customer/dashboard/payment-chart'),
      api.get('/customer/dashboard/payment-modes'),
      api.get('/customer/dashboard/recent-payments?limit=5'),
      api.get('/customer/dashboard/accounts'),
    ])
    .then(([summary, chart, modes, payments, accounts]) => {
      setData({
        summary: summary.data.data,
        chart: chart.data.data,
        modes: modes.data.data,
        payments: payments.data.data,
        accounts: accounts.data.data,
      });
      setLoading(false);
    })
    .catch(console.error);
  }, []);

  if (loading) return <div>Loading...</div>;

  return (
    <div className="dashboard">
      <h1>Welcome, {data.summary.customerName}</h1>
      
      <SummaryCards data={data.summary} />
      <PaymentChart data={data.chart} />
      <PaymentModesPie data={data.modes} />
      <RecentPaymentsTable data={data.payments} />
      <AccountsGrid data={data.accounts} />
    </div>
  );
}
```

---

## 📞 Support

For complete implementation guide see:
- `FRONTEND_INTEGRATION_GUIDE.md` - Full React implementation
- `CUSTOMER_DASHBOARD_API_DOCUMENTATION.md` - Detailed API docs

---

**Version**: 1.0  
**Last Updated**: February 17, 2026  
**Status**: Production Ready ✅
