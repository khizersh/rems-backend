# Vendor Invoice API Documentation
## Real Estate ERP System - Purchase Management Module

### Overview
The Vendor Invoice Controller provides REST APIs for managing vendor invoices in the Real Estate ERP system. These APIs handle invoice creation, retrieval, filtering, and vendor payment tracking operations.

### Base URL
```
/api/vendorInvoice/
```

### Authentication
All endpoints require JWT authentication. The logged-in user is extracted from the JWT token and used for audit trails.

---

## API Endpoints

### 1. Create Invoice
**Endpoint:** `POST /api/vendorInvoice/create`

**Description:** Creates a new vendor invoice against a GRN (Goods Receipt Note)

**Request Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "invoiceNumber": "INV-2024-001",
  "orgId": 1,
  "projectId": 5,
  "vendorId": 10,
  "poId": 25,
  "grnId": 30,
  "totalAmount": 50000.00,
  "paidAmount": 0.00,
  "pendingAmount": 50000.00,
  "status": "UNPAID",
  "invoiceDate": "2024-01-15",
  "dueDate": "2024-02-15",
  "invoiceItemList": [
    {
      "grnItemId": 101,
      "quantity": 100.00,
      "rate": 250.00,
      "amount": 25000.00
    },
    {
      "grnItemId": 102,
      "quantity": 50.00,
      "rate": 500.00,
      "amount": 25000.00
    }
  ]
}
```

**Response:**
```json
{
  "data": {
    "id": 45,
    "invoiceNumber": "INV-2024-001",
    "orgId": 1,
    "projectId": 5,
    "vendorId": 10,
    "poId": 25,
    "grnId": 30,
    "totalAmount": 50000.00,
    "paidAmount": 0.00,
    "pendingAmount": 50000.00,
    "status": "UNPAID",
    "invoiceDate": "2024-01-15",
    "dueDate": "2024-02-15",
    "createdBy": "user123",
    "updatedBy": "user123",
    "createdDate": "2024-01-15T10:30:00",
    "updatedDate": "2024-01-15T10:30:00"
  },
  "responseMessage": "Invoice created successfully",
  "responseCode": "0000"
}
```

**CURL Command:**
```bash
curl -X POST "http://localhost:8080/api/vendorInvoice/create" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "invoiceNumber": "INV-2024-001",
  "orgId": 1,
  "projectId": 5,
  "vendorId": 10,
  "poId": 25,
  "grnId": 30,
  "totalAmount": 50000.00,
  "paidAmount": 0.00,
  "pendingAmount": 50000.00,
  "status": "UNPAID",
  "invoiceDate": "2024-01-15",
  "dueDate": "2024-02-15",
  "invoiceItemList": [
    {
      "grnItemId": 101,
      "quantity": 100.00,
      "rate": 250.00,
      "amount": 25000.00
    }
  ]
}'
```

---

### 2. Get Invoice by ID
**Endpoint:** `GET /api/vendorInvoice/getById/{invoiceId}`

**Description:** Retrieves a specific invoice by its ID

**Path Parameters:**
- `invoiceId` (Long): The invoice ID

**Response:**
```json
{
  "data": {
    "id": 45,
    "invoiceNumber": "INV-2024-001",
    "orgId": 1,
    "projectId": 5,
    "vendorId": 10,
    "poId": 25,
    "grnId": 30,
    "totalAmount": 50000.00,
    "paidAmount": 15000.00,
    "pendingAmount": 35000.00,
    "status": "PARTIAL",
    "invoiceDate": "2024-01-15",
    "dueDate": "2024-02-15",
    "createdBy": "user123",
    "updatedBy": "user456",
    "createdDate": "2024-01-15T10:30:00",
    "updatedDate": "2024-01-20T14:25:00",
    "invoiceItemList": [
      {
        "id": 201,
        "invoiceId": 45,
        "grnItemId": 101,
        "quantity": 100.00,
        "rate": 250.00,
        "amount": 25000.00,
        "createdBy": "user123",
        "updatedBy": "user123",
        "createdDate": "2024-01-15T10:30:00",
        "updatedDate": "2024-01-15T10:30:00"
      }
    ]
  },
  "responseMessage": "Invoice retrieved successfully",
  "responseCode": "0000"
}
```

**CURL Command:**
```bash
curl -X GET "http://localhost:8080/api/vendorInvoice/getById/45" \
-H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 3. Get All Invoices by Organization
**Endpoint:** `POST /api/vendorInvoice/{organizationId}/getAll`

**Description:** Retrieves all invoices for a specific organization with pagination

**Path Parameters:**
- `organizationId` (Long): The organization ID

**Request Body:**
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc",
  "filteredName": ""
}
```

**Response:**
```json
{
  "data": {
    "content": [
      {
        "id": 45,
        "invoiceNumber": "INV-2024-001",
        "orgId": 1,
        "projectId": 5,
        "vendorId": 10,
        "poId": 25,
        "grnId": 30,
        "totalAmount": 50000.00,
        "paidAmount": 15000.00,
        "pendingAmount": 35000.00,
        "status": "PARTIAL",
        "invoiceDate": "2024-01-15",
        "dueDate": "2024-02-15",
        "createdBy": "user123",
        "updatedBy": "user456",
        "createdDate": "2024-01-15T10:30:00",
        "updatedDate": "2024-01-20T14:25:00"
      }
    ],
    "pageable": {
      "sort": {
        "sorted": true,
        "unsorted": false
      },
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 25,
    "totalPages": 3,
    "last": false,
    "first": true,
    "numberOfElements": 10
  },
  "responseMessage": "Invoices retrieved successfully",
  "responseCode": "0000"
}
```

**CURL Command:**
```bash
curl -X POST "http://localhost:8080/api/vendorInvoice/1/getAll" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc",
  "filteredName": ""
}'
```

---

### 4. Get Invoices by Vendor
**Endpoint:** `POST /api/vendorInvoice/getByVendor/{vendorId}`

**Description:** Retrieves all invoices for a specific vendor with pagination

**Path Parameters:**
- `vendorId` (Long): The vendor ID

**Request Body:**
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "invoiceDate",
  "sortDir": "desc",
  "filteredName": ""
}
```

**Response:** Same structure as "Get All Invoices by Organization"

**CURL Command:**
```bash
curl -X POST "http://localhost:8080/api/vendorInvoice/getByVendor/10" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "page": 0,
  "size": 10,
  "sortBy": "invoiceDate",
  "sortDir": "desc",
  "filteredName": ""
}'
```

---

### 5. Get Invoices by Status
**Endpoint:** `POST /api/vendorInvoice/{organizationId}/getByStatus/{status}`

**Description:** Retrieves invoices filtered by status for a specific organization

**Path Parameters:**
- `organizationId` (Long): The organization ID
- `status` (String): Invoice status (UNPAID, PARTIAL, PAID)

**Request Body:**
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "dueDate",
  "sortDir": "asc",
  "filteredName": ""
}
```

**Response:** Same structure as "Get All Invoices by Organization"

**CURL Command:**
```bash
curl -X POST "http://localhost:8080/api/vendorInvoice/1/getByStatus/UNPAID" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "page": 0,
  "size": 10,
  "sortBy": "dueDate",
  "sortDir": "asc",
  "filteredName": ""
}'
```

---

### 6. Get Pending Amount by Vendor
**Endpoint:** `GET /api/vendorInvoice/getPendingAmount/{vendorId}`

**Description:** Retrieves the total pending payment amount for a specific vendor

**Path Parameters:**
- `vendorId` (Long): The vendor ID

**Response:**
```json
{
  "data": {
    "vendorId": 10,
    "vendorName": "ABC Construction Co.",
    "totalInvoiceAmount": 250000.00,
    "totalPaidAmount": 150000.00,
    "totalPendingAmount": 100000.00,
    "invoiceCount": 5,
    "unpaidInvoices": 2,
    "partialPaidInvoices": 2,
    "fullyPaidInvoices": 1
  },
  "responseMessage": "Pending amount retrieved successfully",
  "responseCode": "0000"
}
```

**CURL Command:**
```bash
curl -X GET "http://localhost:8080/api/vendorInvoice/getPendingAmount/10" \
-H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### VendorInvoice Entity
```typescript
interface VendorInvoice {
  id: number;
  invoiceNumber: string;
  orgId: number;
  projectId: number;
  vendorId: number;
  poId: number;
  grnId: number;
  totalAmount: number;
  paidAmount: number;
  pendingAmount: number;
  status: 'UNPAID' | 'PARTIAL' | 'PAID';
  invoiceDate: string; // YYYY-MM-DD format
  dueDate: string; // YYYY-MM-DD format
  createdBy: string;
  updatedBy: string;
  createdDate: string; // ISO DateTime
  updatedDate: string; // ISO DateTime
  invoiceItemList?: VendorInvoiceItem[];
}
```

### VendorInvoiceItem Entity
```typescript
interface VendorInvoiceItem {
  id?: number;
  invoiceId?: number;
  grnItemId: number;
  quantity: number;
  rate: number;
  amount: number;
  createdBy?: string;
  updatedBy?: string;
  createdDate?: string;
  updatedDate?: string;
}
```

### CommonPaginationRequest
```typescript
interface CommonPaginationRequest {
  id?: number; // Used for filtering (organizationId, vendorId, etc.)
  page: number; // Default: 0
  size: number; // Default: 10
  sortBy: string; // Field name to sort by
  sortDir: string; // 'asc' or 'desc'
  filteredName: string; // Search filter
}
```

---

## Error Handling

### Common Error Responses
```json
{
  "data": null,
  "responseMessage": "Error message description",
  "responseCode": "9999"
}
```

### HTTP Status Codes
- `200 OK`: Success
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Invalid or missing JWT token
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: System failure

---

## Frontend Integration Guidelines

### 1. Authentication Setup
```typescript
// Set up axios interceptor for JWT token
axios.defaults.headers.common['Authorization'] = `Bearer ${getJWTToken()}`;
```

### 2. API Service Example
```typescript
class VendorInvoiceService {
  private baseURL = '/api/vendorInvoice';

  async createInvoice(invoiceData: VendorInvoice): Promise<ApiResponse<VendorInvoice>> {
    const response = await axios.post(`${this.baseURL}/create`, invoiceData);
    return response.data;
  }

  async getInvoiceById(invoiceId: number): Promise<ApiResponse<VendorInvoice>> {
    const response = await axios.get(`${this.baseURL}/getById/${invoiceId}`);
    return response.data;
  }

  async getInvoicesByOrganization(
    orgId: number, 
    pagination: CommonPaginationRequest
  ): Promise<ApiResponse<PagedResponse<VendorInvoice>>> {
    const response = await axios.post(`${this.baseURL}/${orgId}/getAll`, pagination);
    return response.data;
  }

  async getInvoicesByVendor(
    vendorId: number, 
    pagination: CommonPaginationRequest
  ): Promise<ApiResponse<PagedResponse<VendorInvoice>>> {
    const response = await axios.post(`${this.baseURL}/getByVendor/${vendorId}`, pagination);
    return response.data;
  }

  async getInvoicesByStatus(
    orgId: number, 
    status: InvoiceStatus, 
    pagination: CommonPaginationRequest
  ): Promise<ApiResponse<PagedResponse<VendorInvoice>>> {
    const response = await axios.post(`${this.baseURL}/${orgId}/getByStatus/${status}`, pagination);
    return response.data;
  }

  async getPendingAmountByVendor(vendorId: number): Promise<ApiResponse<VendorPendingAmount>> {
    const response = await axios.get(`${this.baseURL}/getPendingAmount/${vendorId}`);
    return response.data;
  }
}
```

### 3. React Component Usage Examples

#### Invoice List Component
```typescript
const InvoiceList: React.FC = () => {
  const [invoices, setInvoices] = useState<VendorInvoice[]>([]);
  const [pagination, setPagination] = useState<CommonPaginationRequest>({
    page: 0,
    size: 10,
    sortBy: 'createdDate',
    sortDir: 'desc',
    filteredName: ''
  });

  useEffect(() => {
    loadInvoices();
  }, [pagination]);

  const loadInvoices = async () => {
    try {
      const response = await vendorInvoiceService.getInvoicesByOrganization(orgId, pagination);
      setInvoices(response.data.content);
    } catch (error) {
      console.error('Error loading invoices:', error);
    }
  };

  return (
    <div>
      {/* Invoice table/grid component */}
    </div>
  );
};
```

#### Invoice Form Component
```typescript
const InvoiceForm: React.FC = () => {
  const [invoice, setInvoice] = useState<VendorInvoice>({
    invoiceNumber: '',
    orgId: 0,
    projectId: 0,
    vendorId: 0,
    poId: 0,
    grnId: 0,
    totalAmount: 0,
    paidAmount: 0,
    pendingAmount: 0,
    status: 'UNPAID',
    invoiceDate: '',
    dueDate: '',
    invoiceItemList: []
  });

  const handleSubmit = async () => {
    try {
      const response = await vendorInvoiceService.createInvoice(invoice);
      console.log('Invoice created:', response.data);
    } catch (error) {
      console.error('Error creating invoice:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
    </form>
  );
};
```

### 4. State Management (Redux/Zustand)
```typescript
interface InvoiceStore {
  invoices: VendorInvoice[];
  loading: boolean;
  error: string | null;
  pagination: CommonPaginationRequest;
  
  fetchInvoices: (orgId: number) => Promise<void>;
  createInvoice: (invoice: VendorInvoice) => Promise<void>;
  updatePagination: (pagination: CommonPaginationRequest) => void;
}
```

---

## Notes for Frontend Development

1. **Pagination**: All list endpoints use POST method with pagination request body
2. **Date Format**: Use YYYY-MM-DD format for date fields (invoiceDate, dueDate)
3. **Status Enum**: Use uppercase strings for invoice status (UNPAID, PARTIAL, PAID)
4. **Error Handling**: Check responseCode "0000" for success, "9999" for errors
5. **Authentication**: JWT token is required for all endpoints
6. **Sorting**: Supported sort fields include createdDate, invoiceDate, dueDate, totalAmount
7. **Search**: Use filteredName in pagination request for text-based filtering

This documentation provides complete integration guidance for the Vendor Invoice API endpoints in your React frontend application.
