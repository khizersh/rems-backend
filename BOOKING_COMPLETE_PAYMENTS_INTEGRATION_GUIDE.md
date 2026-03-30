# Booking Complete + Payment Block Integration Guide

## Overview
This change adds a **booking completion flag** and blocks new payments when a booking is marked complete. The frontend should use the new API to mark a booking complete (or reopen it) and handle payment failures for completed bookings.

## What Changed
- **New field**: `bookingComplete` in `booking` table (`boolean`, default `false`).
- **New API**: `POST /api/booking/markComplete` to mark a booking complete or reopen it.
- **Payment blocking**: `payInstallment` now fails if the related booking is complete.

## Key Rules
- If a booking is marked **complete**, **no new payments** are allowed.
- Existing payments remain unchanged.
- Reopen a booking by setting `bookingComplete = false`.

---

## API: Mark Booking Complete / Reopen
**Endpoint**
- `POST /api/booking/markComplete`

**Headers**
- `Authorization: Bearer <JWT>`
- `Content-Type: application/json`

**Request Body**
```json
{
  "bookingId": 123,
  "bookingComplete": true
}
```

**Success Response (standard wrapper)**
```json
{
  "data": {
    "id": 123,
    "bookingComplete": true,
    "updatedBy": "<username>",
    "updatedDate": "2026-03-11T12:30:00"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error Cases**
- Booking not found → `NO_DATA_FOUND`
- Invalid input → `INVALID_PARAMETER`

---

## Payment API Behavior (payInstallment)
When calling the existing payment API (e.g., `payInstallment`):
- If booking is complete → response will be an error.
- Example error message:
  - `"Booking is marked complete; no further payments are allowed."`

**Frontend Handling**
- If you receive the above error, disable payment UI for that booking.
- Provide a button to reopen booking via `bookingComplete=false` if allowed by role.

---

## Suggested UI Updates
- Show booking status: **Complete / Open**.
- Add action button:
  - **Mark Complete** → calls `POST /api/booking/markComplete` with `true`.
  - **Reopen Booking** → calls `POST /api/booking/markComplete` with `false`.
- On payment screens:
  - Block submission when `bookingComplete = true`.

---

## Example cURL
```bash
curl --location 'http://localhost:8081/api/booking/markComplete' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <JWT>' \
--data '{
  "bookingId": 123,
  "bookingComplete": true
}'
```

---

## Backend References
- Entity: `Booking` includes `bookingComplete` field.
- Controller: `BookingController#markBookingComplete`
- Service: `BookingService#markBookingComplete`
- Payment validation: `CustomerPaymentService#updateCustomerPayment`
