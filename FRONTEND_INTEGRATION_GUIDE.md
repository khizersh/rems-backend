# Customer Dashboard API - Frontend Integration Guide

## 📋 Overview

This guide provides everything needed to integrate the Customer Dashboard APIs into a React frontend application. All APIs are JWT-authenticated and automatically resolve customer data from the logged-in user's token.

---

## 🔐 Authentication Setup

### 1. API Configuration

```javascript
// src/api/config.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

// Create axios instance with base configuration
export const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to all requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 2. Authentication Service

```javascript
// src/services/authService.js
import api from '../api/config';

export const authService = {
  /**
   * Login user and store JWT token
   * @param {string} username - Username
   * @param {string} password - Password
   * @returns {Promise<{token: string, user: object}>}
   */
  login: async (username, password) => {
    const response = await api.post('/user/login', { username, password });
    
    if (response.data.responseCode === '0000') {
      const { token, user } = response.data.data;
      localStorage.setItem('authToken', token);
      localStorage.setItem('user', JSON.stringify(user));
      return { token, user };
    }
    
    throw new Error(response.data.responseMessage || 'Login failed');
  },

  /**
   * Logout user
   */
  logout: () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
  },

  /**
   * Check if user is authenticated
   * @returns {boolean}
   */
  isAuthenticated: () => {
    return !!localStorage.getItem('authToken');
  },

  /**
   * Get current user
   * @returns {object|null}
   */
  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },
};
```

---

## 📊 Customer Dashboard Service

### Complete Service Implementation

```javascript
// src/services/customerDashboardService.js
import api from '../api/config';

/**
 * Customer Dashboard API Service
 * All methods automatically use JWT token from localStorage
 * No customer ID needed - derived from authenticated user
 */
export const customerDashboardService = {
  
  /**
   * Get customer summary with KPIs
   * @returns {Promise<CustomerSummary>}
   * 
   * Response Structure:
   * {
   *   customerName: string,
   *   nationalId: string,
   *   contactNo: string,
   *   email: string,
   *   totalBookings: number,
   *   totalUnitsBooked: number,
   *   totalAmountPayable: number,
   *   totalAmountPaid: number,
   *   totalRemainingAmount: number,
   *   overdueAmount: number
   * }
   */
  getSummary: async () => {
    const response = await api.get('/customer/dashboard/summary');
    
    if (response.data.responseCode === '0000') {
      return response.data.data;
    }
    
    throw new Error(response.data.responseMessage || 'Failed to fetch summary');
  },

  /**
   * Get monthly payment chart data
   * @returns {Promise<Array<PaymentChartData>>}
   * 
   * Response Structure (Array):
   * [{
   *   month: number,           // 1-12
   *   year: number,            // e.g., 2024
   *   monthName: string,       // e.g., "Jan"
   *   totalPaidAmount: number,
   *   totalDueAmount: number,
   *   cumulativeRemaining: number
   * }]
   */
  getPaymentChart: async () => {
    const response = await api.get('/customer/dashboard/payment-chart');
    
    if (response.data.responseCode === '0000') {
      return response.data.data;
    }
    
    throw new Error(response.data.responseMessage || 'Failed to fetch payment chart');
  },

  /**
   * Get payment mode distribution
   * @returns {Promise<Array<PaymentModeDistribution>>}
   * 
   * Response Structure (Array):
   * [{
   *   paymentMode: string,      // e.g., "CASH", "BANK_TRANSFER", "CHEQUE"
   *   totalAmount: number,
   *   transactionCount: number
   * }]
   */
  getPaymentModes: async () => {
    const response = await api.get('/customer/dashboard/payment-modes');
    
    if (response.data.responseCode === '0000') {
      return response.data.data;
    }
    
    throw new Error(response.data.responseMessage || 'Failed to fetch payment modes');
  },

  /**
   * Get recent payment transactions
   * @param {number} limit - Number of recent payments (default: 10)
   * @returns {Promise<Array<RecentPayment>>}
   * 
   * Response Structure (Array):
   * [{
   *   paymentId: number,
   *   accountId: number,
   *   projectName: string,
   *   unitSerial: string,
   *   totalPaymentAmount: number,
   *   receivedAmount: number,
   *   paidDate: string,         // ISO date string
   *   paymentStatus: string,    // e.g., "PAID", "UNPAID"
   *   paymentDetails: [{
   *     paymentType: string,
   *     amount: number,
   *     chequeNo: string | null,
   *     chequeDate: string | null
   *   }]
   * }]
   */
  getRecentPayments: async (limit = 10) => {
    const response = await api.get(`/customer/dashboard/recent-payments?limit=${limit}`);
    
    if (response.data.responseCode === '0000') {
      return response.data.data;
    }
    
    throw new Error(response.data.responseMessage || 'Failed to fetch recent payments');
  },

  /**
   * Get all customer accounts with status
   * @returns {Promise<Array<AccountStatus>>}
   * 
   * Response Structure (Array):
   * [{
   *   accountId: number,
   *   projectName: string,
   *   unitSerial: string,
   *   unitType: string,         // e.g., "APARTMENT", "SHOP"
   *   totalAmount: number,
   *   totalPaidAmount: number,
   *   totalBalanceAmount: number,
   *   status: string,           // e.g., "ACTIVE", "CLOSED"
   *   durationInMonths: number
   * }]
   */
  getAccounts: async () => {
    const response = await api.get('/customer/dashboard/accounts');
    
    if (response.data.responseCode === '0000') {
      return response.data.data;
    }
    
    throw new Error(response.data.responseMessage || 'Failed to fetch accounts');
  },
};

export default customerDashboardService;
```

---

## 🎨 React Components

### 1. Customer Dashboard Main Component

```jsx
// src/pages/CustomerDashboard.jsx
import React, { useState, useEffect } from 'react';
import { customerDashboardService } from '../services/customerDashboardService';
import SummaryCards from '../components/dashboard/SummaryCards';
import PaymentChart from '../components/dashboard/PaymentChart';
import PaymentModeChart from '../components/dashboard/PaymentModeChart';
import RecentPayments from '../components/dashboard/RecentPayments';
import AccountsList from '../components/dashboard/AccountsList';
import './CustomerDashboard.css';

const CustomerDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [summary, setSummary] = useState(null);
  const [paymentChart, setPaymentChart] = useState([]);
  const [paymentModes, setPaymentModes] = useState([]);
  const [recentPayments, setRecentPayments] = useState([]);
  const [accounts, setAccounts] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load all dashboard data in parallel
      const [
        summaryData,
        chartData,
        modesData,
        paymentsData,
        accountsData
      ] = await Promise.all([
        customerDashboardService.getSummary(),
        customerDashboardService.getPaymentChart(),
        customerDashboardService.getPaymentModes(),
        customerDashboardService.getRecentPayments(5),
        customerDashboardService.getAccounts(),
      ]);

      setSummary(summaryData);
      setPaymentChart(chartData);
      setPaymentModes(modesData);
      setRecentPayments(paymentsData);
      setAccounts(accountsData);
    } catch (err) {
      setError(err.message);
      console.error('Dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-error">
        <p>Error: {error}</p>
        <button onClick={loadDashboardData}>Retry</button>
      </div>
    );
  }

  return (
    <div className="customer-dashboard">
      <div className="dashboard-header">
        <h1>Welcome, {summary?.customerName}</h1>
        <p className="customer-info">
          {summary?.contactNo} | {summary?.email}
        </p>
      </div>

      {/* KPI Summary Cards */}
      <SummaryCards summary={summary} />

      {/* Charts Section */}
      <div className="charts-section">
        <div className="chart-container">
          <h2>Payment Trends</h2>
          <PaymentChart data={paymentChart} />
        </div>
        
        <div className="chart-container">
          <h2>Payment Methods</h2>
          <PaymentModeChart data={paymentModes} />
        </div>
      </div>

      {/* Recent Payments Table */}
      <div className="section">
        <h2>Recent Payments</h2>
        <RecentPayments payments={recentPayments} />
      </div>

      {/* Accounts/Properties List */}
      <div className="section">
        <h2>My Properties</h2>
        <AccountsList accounts={accounts} />
      </div>
    </div>
  );
};

export default CustomerDashboard;
```

### 2. Summary Cards Component

```jsx
// src/components/dashboard/SummaryCards.jsx
import React from 'react';
import { FaHome, FaDollarSign, FaMoneyBillWave, FaExclamationTriangle } from 'react-icons/fa';
import './SummaryCards.css';

const SummaryCards = ({ summary }) => {
  if (!summary) return null;

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const cards = [
    {
      title: 'Total Bookings',
      value: summary.totalBookings,
      icon: <FaHome />,
      color: 'blue',
    },
    {
      title: 'Total Amount',
      value: formatCurrency(summary.totalAmountPayable),
      icon: <FaDollarSign />,
      color: 'purple',
    },
    {
      title: 'Amount Paid',
      value: formatCurrency(summary.totalAmountPaid),
      icon: <FaMoneyBillWave />,
      color: 'green',
    },
    {
      title: 'Remaining',
      value: formatCurrency(summary.totalRemainingAmount),
      icon: <FaExclamationTriangle />,
      color: summary.totalRemainingAmount > 0 ? 'orange' : 'green',
    },
  ];

  return (
    <div className="summary-cards">
      {cards.map((card, index) => (
        <div key={index} className={`summary-card ${card.color}`}>
          <div className="card-icon">{card.icon}</div>
          <div className="card-content">
            <h3>{card.title}</h3>
            <p className="card-value">{card.value}</p>
          </div>
        </div>
      ))}
    </div>
  );
};

export default SummaryCards;
```

### 3. Payment Chart Component (using Chart.js)

```jsx
// src/components/dashboard/PaymentChart.jsx
import React from 'react';
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

const PaymentChart = ({ data }) => {
  if (!data || data.length === 0) {
    return <p>No payment data available</p>;
  }

  const chartData = {
    labels: data.map(item => `${item.monthName} ${item.year}`),
    datasets: [
      {
        label: 'Amount Paid',
        data: data.map(item => item.totalPaidAmount),
        backgroundColor: 'rgba(75, 192, 192, 0.6)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: false,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function(value) {
            return '$' + value.toLocaleString();
          },
        },
      },
    },
  };

  return (
    <div style={{ height: '300px' }}>
      <Bar data={chartData} options={options} />
    </div>
  );
};

export default PaymentChart;
```

### 4. Payment Mode Chart Component (Pie Chart)

```jsx
// src/components/dashboard/PaymentModeChart.jsx
import React from 'react';
import { Pie } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';

ChartJS.register(ArcElement, Tooltip, Legend);

const PaymentModeChart = ({ data }) => {
  if (!data || data.length === 0) {
    return <p>No payment mode data available</p>;
  }

  const chartData = {
    labels: data.map(item => item.paymentMode),
    datasets: [
      {
        label: 'Amount by Payment Mode',
        data: data.map(item => item.totalAmount),
        backgroundColor: [
          'rgba(255, 99, 132, 0.6)',
          'rgba(54, 162, 235, 0.6)',
          'rgba(255, 206, 86, 0.6)',
          'rgba(75, 192, 192, 0.6)',
          'rgba(153, 102, 255, 0.6)',
        ],
        borderColor: [
          'rgba(255, 99, 132, 1)',
          'rgba(54, 162, 235, 1)',
          'rgba(255, 206, 86, 1)',
          'rgba(75, 192, 192, 1)',
          'rgba(153, 102, 255, 1)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = ((value / total) * 100).toFixed(1);
            return `${label}: $${value.toLocaleString()} (${percentage}%)`;
          },
        },
      },
    },
  };

  return (
    <div style={{ height: '300px' }}>
      <Pie data={chartData} options={options} />
    </div>
  );
};

export default PaymentModeChart;
```

### 5. Recent Payments Component

```jsx
// src/components/dashboard/RecentPayments.jsx
import React from 'react';
import { format } from 'date-fns';
import './RecentPayments.css';

const RecentPayments = ({ payments }) => {
  if (!payments || payments.length === 0) {
    return <p>No recent payments</p>;
  }

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return format(new Date(dateString), 'MMM dd, yyyy');
  };

  return (
    <div className="recent-payments">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Project</th>
            <th>Unit</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Payment Methods</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((payment) => (
            <tr key={payment.paymentId}>
              <td>{formatDate(payment.paidDate)}</td>
              <td>{payment.projectName}</td>
              <td>{payment.unitSerial}</td>
              <td>{formatCurrency(payment.receivedAmount)}</td>
              <td>
                <span className={`status-badge ${payment.paymentStatus.toLowerCase()}`}>
                  {payment.paymentStatus}
                </span>
              </td>
              <td>
                {payment.paymentDetails.map((detail, idx) => (
                  <span key={idx} className="payment-method">
                    {detail.paymentType}
                    {idx < payment.paymentDetails.length - 1 && ', '}
                  </span>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RecentPayments;
```

### 6. Accounts List Component

```jsx
// src/components/dashboard/AccountsList.jsx
import React from 'react';
import './AccountsList.css';

const AccountsList = ({ accounts }) => {
  if (!accounts || accounts.length === 0) {
    return <p>No accounts found</p>;
  }

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const getStatusColor = (status) => {
    switch (status.toLowerCase()) {
      case 'active':
        return 'blue';
      case 'closed':
        return 'green';
      case 'overdue':
        return 'red';
      default:
        return 'gray';
    }
  };

  return (
    <div className="accounts-grid">
      {accounts.map((account) => (
        <div key={account.accountId} className="account-card">
          <div className="account-header">
            <h3>{account.projectName}</h3>
            <span className={`status-badge ${getStatusColor(account.status)}`}>
              {account.status}
            </span>
          </div>
          
          <div className="account-details">
            <div className="detail-row">
              <span className="label">Unit:</span>
              <span className="value">{account.unitSerial}</span>
            </div>
            <div className="detail-row">
              <span className="label">Type:</span>
              <span className="value">{account.unitType}</span>
            </div>
            <div className="detail-row">
              <span className="label">Duration:</span>
              <span className="value">{account.durationInMonths} months</span>
            </div>
          </div>

          <div className="account-financials">
            <div className="financial-row">
              <span>Total Amount:</span>
              <strong>{formatCurrency(account.totalAmount)}</strong>
            </div>
            <div className="financial-row paid">
              <span>Paid:</span>
              <strong>{formatCurrency(account.totalPaidAmount)}</strong>
            </div>
            <div className="financial-row remaining">
              <span>Remaining:</span>
              <strong>{formatCurrency(account.totalBalanceAmount)}</strong>
            </div>
          </div>

          {account.totalBalanceAmount > 0 && (
            <div className="progress-bar">
              <div 
                className="progress-fill"
                style={{ 
                  width: `${(account.totalPaidAmount / account.totalAmount) * 100}%` 
                }}
              ></div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default AccountsList;
```

---

## 🎨 Sample CSS Styles

### Dashboard Main Styles

```css
/* src/pages/CustomerDashboard.css */
.customer-dashboard {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
  font-size: 32px;
  font-weight: bold;
  color: #1a202c;
  margin-bottom: 8px;
}

.customer-info {
  color: #718096;
  font-size: 14px;
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.chart-container {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-container h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #2d3748;
}

.section {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.section h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #2d3748;
}

.dashboard-loading,
.dashboard-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
```

### Summary Cards Styles

```css
/* src/components/dashboard/SummaryCards.css */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.summary-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s;
}

.summary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.card-icon {
  font-size: 40px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.summary-card.blue .card-icon {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.summary-card.purple .card-icon {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.summary-card.green .card-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.summary-card.orange .card-icon {
  background: rgba(251, 146, 60, 0.1);
  color: #fb923c;
}

.card-content h3 {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}
```

### Accounts List Styles

```css
/* src/components/dashboard/AccountsList.css */
.accounts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.account-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s;
}

.account-card:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: #3b82f6;
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.account-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.status-badge.green {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-badge.red {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.account-details {
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-row .label {
  color: #6b7280;
  font-size: 14px;
}

.detail-row .value {
  color: #1f2937;
  font-weight: 500;
  font-size: 14px;
}

.account-financials {
  background: white;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.financial-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.financial-row:last-child {
  margin-bottom: 0;
}

.financial-row.paid {
  color: #10b981;
}

.financial-row.remaining {
  color: #f59e0b;
  font-size: 16px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
}

.progress-bar {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #3b82f6);
  transition: width 0.3s ease;
}
```

---

## 🔌 React Hooks (Custom Hooks)

### useDashboard Hook

```javascript
// src/hooks/useDashboard.js
import { useState, useEffect, useCallback } from 'react';
import { customerDashboardService } from '../services/customerDashboardService';

export const useDashboard = () => {
  const [data, setData] = useState({
    summary: null,
    paymentChart: [],
    paymentModes: [],
    recentPayments: [],
    accounts: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadDashboard = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [summary, paymentChart, paymentModes, recentPayments, accounts] = 
        await Promise.all([
          customerDashboardService.getSummary(),
          customerDashboardService.getPaymentChart(),
          customerDashboardService.getPaymentModes(),
          customerDashboardService.getRecentPayments(10),
          customerDashboardService.getAccounts(),
        ]);

      setData({
        summary,
        paymentChart,
        paymentModes,
        recentPayments,
        accounts,
      });
    } catch (err) {
      setError(err.message);
      console.error('Dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  return { data, loading, error, refresh: loadDashboard };
};
```

**Usage:**

```jsx
import { useDashboard } from '../hooks/useDashboard';

const MyDashboard = () => {
  const { data, loading, error, refresh } = useDashboard();

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>Welcome, {data.summary.customerName}</h1>
      {/* Use data.summary, data.paymentChart, etc. */}
    </div>
  );
};
```

---

## 📦 Required Dependencies

### Install Required Packages

```bash
npm install axios react-chartjs-2 chart.js date-fns react-icons
```

or

```bash
yarn add axios react-chartjs-2 chart.js date-fns react-icons
```

### package.json Dependencies

```json
{
  "dependencies": {
    "axios": "^1.6.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-chartjs-2": "^5.2.0",
    "chart.js": "^4.4.0",
    "date-fns": "^2.30.0",
    "react-icons": "^4.12.0"
  }
}
```

---

## 🚀 Quick Integration Steps

### Step 1: Setup API Configuration
```bash
# Create API config
src/api/config.js
```

### Step 2: Create Services
```bash
# Create auth service
src/services/authService.js

# Create dashboard service
src/services/customerDashboardService.js
```

### Step 3: Create Components
```bash
# Main dashboard page
src/pages/CustomerDashboard.jsx

# Dashboard components
src/components/dashboard/SummaryCards.jsx
src/components/dashboard/PaymentChart.jsx
src/components/dashboard/PaymentModeChart.jsx
src/components/dashboard/RecentPayments.jsx
src/components/dashboard/AccountsList.jsx
```

### Step 4: Add Routes
```jsx
// src/App.jsx
import CustomerDashboard from './pages/CustomerDashboard';

function App() {
  return (
    <Routes>
      <Route path="/dashboard" element={<CustomerDashboard />} />
    </Routes>
  );
}
```

---

## 🧪 Testing

### Test API Calls

```javascript
// src/tests/dashboardService.test.js
import { customerDashboardService } from '../services/customerDashboardService';

describe('Customer Dashboard Service', () => {
  beforeAll(() => {
    // Mock token
    localStorage.setItem('authToken', 'test-token');
  });

  test('should fetch summary', async () => {
    const summary = await customerDashboardService.getSummary();
    expect(summary).toHaveProperty('customerName');
    expect(summary).toHaveProperty('totalBookings');
  });

  test('should fetch payment chart', async () => {
    const chart = await customerDashboardService.getPaymentChart();
    expect(Array.isArray(chart)).toBe(true);
  });

  test('should fetch recent payments with limit', async () => {
    const payments = await customerDashboardService.getRecentPayments(5);
    expect(payments.length).toBeLessThanOrEqual(5);
  });
});
```

---

## 📱 Responsive Design

All components are responsive. For mobile optimization:

```css
/* Mobile breakpoints */
@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }

  .charts-section {
    grid-template-columns: 1fr;
  }

  .accounts-grid {
    grid-template-columns: 1fr;
  }

  .recent-payments table {
    font-size: 12px;
  }
}
```

---

## 🔒 Security Best Practices

1. **Token Storage**: Use httpOnly cookies in production instead of localStorage
2. **Token Refresh**: Implement token refresh mechanism
3. **Error Handling**: Don't expose sensitive errors to users
4. **CORS**: Configure CORS properly on backend
5. **HTTPS**: Always use HTTPS in production

---

## 📊 TypeScript Support (Optional)

### Type Definitions

```typescript
// src/types/dashboard.types.ts

export interface CustomerSummary {
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

export interface PaymentChartData {
  month: number;
  year: number;
  monthName: string;
  totalPaidAmount: number;
  totalDueAmount: number;
  cumulativeRemaining: number;
}

export interface PaymentModeDistribution {
  paymentMode: string;
  totalAmount: number;
  transactionCount: number;
}

export interface PaymentDetail {
  paymentType: string;
  amount: number;
  chequeNo: string | null;
  chequeDate: string | null;
}

export interface RecentPayment {
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

export interface AccountStatus {
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

export interface ApiResponse<T> {
  responseCode: string;
  responseMessage: string;
  data: T;
}
```

---

## 🎯 Summary

### What You Get:
✅ Complete React components ready to use  
✅ API service with all 5 endpoints integrated  
✅ Custom hooks for state management  
✅ Chart components with Chart.js  
✅ Responsive CSS styles  
✅ Error handling and loading states  
✅ TypeScript support (optional)  
✅ Authentication flow  
✅ Testing examples  

### Integration Time:
- Basic Setup: ~30 minutes
- Full Dashboard: ~2-3 hours
- With customization: ~1 day

This guide provides everything needed to integrate the Customer Dashboard APIs into any React application!
