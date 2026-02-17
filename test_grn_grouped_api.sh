#!/bin/bash

# GRN Grouped by PO ID API - Quick Test Commands
# Replace YOUR_JWT_TOKEN with actual JWT token
# Replace localhost:8080 with your server URL if different

echo "Testing GRN Grouped by PO ID API..."

# Test 1: Basic request
echo -e "\n1. Basic Request (First page, sorted by last GRN date):"
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "lastGrnDate",
    "sortDir": "desc"
  }' | jq .

# Test 2: Sort by GRN count
echo -e "\n2. Sort by GRN Count (Ascending):"
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "totalGrnCount",
    "sortDir": "asc"
  }' | jq .

# Test 3: Sort by PO number
echo -e "\n3. Sort by PO Number (Alphabetical):"
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "poNumber",
    "sortDir": "asc"
  }' | jq .

# Test 4: Large page size
echo -e "\n4. Large Page Size (20 records):"
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 20,
    "sortBy": "lastGrnDate",
    "sortDir": "desc"
  }' | jq .

# Test 5: Second page
echo -e "\n5. Second Page:"
curl -X POST "http://localhost:8080/api/grn/getGroupedByPoId" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "id": 1,
    "page": 1,
    "size": 10,
    "sortBy": "lastGrnDate",
    "sortDir": "desc"
  }' | jq .

echo -e "\nTest completed!"
echo "Note: Replace YOUR_JWT_TOKEN with actual token and ensure the server is running."
