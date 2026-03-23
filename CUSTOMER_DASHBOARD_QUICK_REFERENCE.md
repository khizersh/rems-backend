# Customer Dashboard Quick Reference

## 🚀 Quick Start

### 1. Test the API

```bash
# Step 1: Login (replace credentials)
curl --location 'http://localhost:8080/api/user/login' \
--header 'Content-Type: application/json' \
--data '{"username": "customer_user", "password": "password123"}'

# Step 2: Copy the token from response and use it

# Get Summary
curl --location 'http://localhost:8080/api/customer/dashboard/summary' \
--header 'Authorization: Bearer YOUR_TOKEN'

# Get Payment Chart
curl --location 'http://localhost:8080/api/customer/dashboard/payment-chart' \
--header 'Authorization: Bearer YOUR_TOKEN'

# Get Payment Modes
curl --location 'http://localhost:8080/api/customer/dashboard/payment-modes' \
--header 'Authorization: Bearer YOUR_TOKEN'

# Get Recent Payments (last 5)
curl --location 'http://localhost:8080/api/customer/dashboard/recent-payments?limit=5' \
--header 'Authorization: Bearer YOUR_TOKEN'

# Get All Accounts
curl --location 'http://localhost:8080/api/customer/dashboard/accounts' \
--header 'Authorization: Bearer YOUR_TOKEN'
```

## 📁 Files Created

### New Files
```
src/main/java/com/rem/backend/
├── controller/CustomerDashboardController.java
├── service/CustomerDashboardService.java
└── dto/customer/dashboard/
    ├── CustomerSummaryDTO.java
    ├── PaymentChartDTO.java
    ├── PaymentModeDistributionDTO.java
    ├── RecentPaymentDTO.java
    └── AccountStatusDTO.java
```

### Modified Files
```
src/main/java/com/rem/backend/repository/
├── CustomerAccountRepo.java (added 5 methods)
├── CustomerPaymentRepo.java (added 3 methods)
└── CustomerPaymentDetailRepo.java (added 2 methods)
```

## 🔑 Key Points

1. **No Customer ID Required** - All endpoints derive customer from JWT token
2. **Secure by Design** - Username extracted from token → User → Customer
3. **Zero Breaking Changes** - Only adds new code, doesn't modify existing endpoints
4. **Performance Optimized** - Aggregate queries run in database
5. **Null Safe** - All queries use COALESCE for zero values

## 🎯 API Endpoints Summary

| Endpoint | Returns |
|----------|---------|
| `GET /api/customer/dashboard/summary` | KPIs: bookings, total, paid, remaining |
| `GET /api/customer/dashboard/payment-chart` | Monthly payment trends |
| `GET /api/customer/dashboard/payment-modes` | Payment type breakdown |
| `GET /api/customer/dashboard/recent-payments?limit=N` | Last N payments |
| `GET /api/customer/dashboard/accounts` | All customer units/properties |

## 📊 Response Format

All endpoints return:
```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": { ... }
}
```

## 🐛 Common Issues

### Issue: "User not found"
**Solution**: Token is invalid or expired. Login again to get new token.

### Issue: "Customer profile not found for this user"
**Solution**: User exists but has no customer record. Create customer profile linking to user.

### Issue: Empty data returned
**Solution**: Customer has no bookings/payments yet. Create test data in database.

## 📦 Import to Postman

Import the collection file:
```
Customer_Dashboard_API.postman_collection.json
```

Steps:
1. Open Postman
2. Click Import
3. Select `Customer_Dashboard_API.postman_collection.json`
4. Run "Login" request first to save token
5. Token auto-populates in other requests

## 🔐 Security Flow

```
1. User logs in with username/password
2. Backend returns JWT token containing username
3. Frontend stores token
4. Each API call includes: Authorization: Bearer <token>
5. Backend extracts username from token
6. Backend finds User by username
7. Backend finds Customer by userId
8. All data filtered by customerId
```

## 💾 Database Requirements

### Required Data
- User record with `username`
- Customer record with `userId` linked to user
- Customer accounts (bookings)
- Customer payments (optional, but needed for full dashboard)
- Customer payment details (optional)

### Sample Query to Verify
```sql
-- Check if user has customer profile
SELECT u.username, c.customer_id, c.name
FROM users u
LEFT JOIN customer c ON c.user_id = u.id
WHERE u.username = 'your_customer_username';

-- Check customer accounts
SELECT ca.id, ca.total_amount, ca.total_paid_amount, ca.total_balance_amount
FROM customer_account ca
WHERE ca.customer_id = YOUR_CUSTOMER_ID
AND ca.is_active = 1;
```

## 🎨 Frontend Integration

### React Example
```javascript
import { useState, useEffect } from 'react';

const CustomerDashboard = () => {
  const [summary, setSummary] = useState(null);
  const token = localStorage.getItem('authToken');

  useEffect(() => {
    fetch('/api/customer/dashboard/summary', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => res.json())
    .then(data => setSummary(data.data));
  }, []);

  return (
    <div>
      <h1>Welcome, {summary?.customerName}</h1>
      <div className="kpi-cards">
        <div>Bookings: {summary?.totalBookings}</div>
        <div>Total: ${summary?.totalAmountPayable}</div>
        <div>Paid: ${summary?.totalAmountPaid}</div>
        <div>Remaining: ${summary?.totalRemainingAmount}</div>
      </div>
    </div>
  );
};
```

### Vue Example
```javascript
<template>
  <div>
    <h1>Welcome, {{ summary.customerName }}</h1>
    <div class="kpi-cards">
      <div>Bookings: {{ summary.totalBookings }}</div>
      <div>Total: ${{ summary.totalAmountPayable }}</div>
      <div>Paid: ${{ summary.totalAmountPaid }}</div>
      <div>Remaining: ${{ summary.totalRemainingAmount }}</div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return { summary: {} };
  },
  async mounted() {
    const token = localStorage.getItem('authToken');
    const res = await fetch('/api/customer/dashboard/summary', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await res.json();
    this.summary = data.data;
  }
};
</script>
```

## 📈 Performance Tips

1. **Cache Dashboard Data**: Cache summary data for 5-10 minutes on frontend
2. **Lazy Load Charts**: Load chart data after initial page render
3. **Paginate Recent Payments**: Use limit=5 for dashboard, show "View More" link
4. **Optimize Images**: If showing property images, use thumbnails
5. **Background Refresh**: Refresh data in background every few minutes

## ✅ Testing Checklist

- [ ] Can login and get JWT token
- [ ] Summary endpoint returns customer data
- [ ] Payment chart shows monthly data
- [ ] Payment modes show distribution
- [ ] Recent payments return correct limit
- [ ] Accounts show all customer units
- [ ] Invalid token returns error
- [ ] User without customer profile returns error
- [ ] All amounts sum correctly
- [ ] Performance is acceptable (< 500ms per endpoint)

## 📚 Documentation Files

1. **CUSTOMER_DASHBOARD_API_DOCUMENTATION.md** - Complete API reference
2. **CUSTOMER_DASHBOARD_IMPLEMENTATION_SUMMARY.md** - Implementation overview
3. **Customer_Dashboard_API.postman_collection.json** - Postman collection
4. **CUSTOMER_DASHBOARD_QUICK_REFERENCE.md** - This file

## 🎓 Learning Resources

### Understanding JWT
- Token contains username as subject
- Token is signed, can't be tampered
- Token expires after 10 hours (configurable)
- No need to query database on every request for basic auth

### Understanding Aggregate Queries
- `SUM()` - Adds up all values
- `COUNT()` - Counts rows
- `GROUP BY` - Groups results by field
- `COALESCE(value, 0)` - Returns 0 if value is NULL

## 🛠️ Troubleshooting

### Application won't start
**Check**: Make sure all required dependencies are in pom.xml
**Fix**: Run `mvn clean install`

### Endpoints return 404
**Check**: Controller is in correct package
**Fix**: Verify `@RestController` and `@RequestMapping` annotations

### Queries return null
**Check**: Customer has active accounts
**Fix**: Create test data or verify is_active = 1

### Token not working
**Check**: Token format in Authorization header
**Fix**: Must be `Authorization: Bearer <token>` (note the space)

## 🚀 Next Steps

1. Test all endpoints with real data
2. Integrate with frontend application
3. Add error logging and monitoring
4. Implement caching if needed
5. Add unit tests for service layer
6. Load test with multiple concurrent users
7. Add payment schedule integration (Phase 2)

---

**Quick Help**: For detailed documentation, see `CUSTOMER_DASHBOARD_API_DOCUMENTATION.md`
