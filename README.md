# MediBook — UC5 Payment Service

---

## What we built in UC5

A complete payment management system for appointments.
Think of it like the hospital billing counter.
Patient pays for appointment online after booking.
System tracks every payment from pending to success.
Refunds processed when appointment cancelled.
Doctor sees their earnings. Admin sees platform revenue.

---

## What problem UC5 solves
```
Without UC5:
→ No way to accept payment for appointments
→ No payment tracking or history
→ No refund management
→ No doctor earnings visibility
→ No admin revenue analytics

With UC5:
→ Patient pays online after booking (UPI/Card/Wallet/Cash)
→ Payment tracked: PENDING → SUCCESS → REFUNDED
→ Refund auto triggered on cancellation
→ Doctor sees total earnings dashboard
→ Admin sees platform total revenue
→ Mock gateway now — Razorpay ready to plug in later
```

---

## How UC5 connects to UC4
```
UC4 gave us → Appointment record (appointmentId)

UC5 builds on top:
→ One payment per appointment (PDF rule)
→ Payment links to appointmentId
→ Before paying → appointment must be SCHEDULED
→ Cannot pay for COMPLETED or NO_SHOW appointment
→ When appointment cancelled → refund triggered

Three records now exist for every booking:
1. users table         → patient account (UC1)
2. providers table     → doctor profile (UC2)
3. availability_slots  → time slot (UC3)
4. appointments table  → booking record (UC4)
5. payments table      → payment record (UC5)
```

---

## Files we created and why each exists
```
PaymentRequest.java (DTO)
→ what patient sends when initiating payment
→ appointmentId links payment to appointment
→ patientId identifies who is paying
→ amount is the consultation fee
→ paymentMethod is UPI or CARD or WALLET or CASH
→ currency is INR
→ we never expose Payment entity directly to outside

PaymentResponse.java (DTO)
→ what server sends back after payment operation
→ paymentId, appointmentId, status
→ amount, currency, paymentMethod
→ razorpayOrderId for future Razorpay integration
→ message tells patient what happened
→ transactionTime is when payment was processed

Payment.java (entity)
→ maps to payments table in MySQL
→ links to appointments via appointmentId
→ status: PENDING / SUCCESS / FAILED / REFUNDED
→ stores razorpayOrderId, razorpayPaymentId,
  razorpaySignature for future Razorpay integration
→ one payment per appointment enforced in service
→ createdAt auto set by @PrePersist

PaymentRepository.java
→ findByAppointmentId() get payment for one appointment
→ findByPatientId() patient payment history
→ findByStatus() admin filters payments by status
→ existsByAppointmentId() checks duplicate payment
→ sumAmountByStatus() total revenue calculation

PaymentService.java (interface)
→ defines contract what methods must exist
→ initiatePayment(), verifyPayment()
→ initiateRefund(), updatePaymentStatus()
→ getPaymentsByPatient(), getPaymentsByStatus()
→ getTotalRevenue(), getPaymentByAppointment()

PaymentServiceImpl.java (business logic)
→ initiatePayment: checks appointment SCHEDULED
  checks no existing payment for this appointment
  creates PENDING payment record
  mock mode instantly marks SUCCESS
→ verifyPayment: mock verification returns SUCCESS
  future: real Razorpay signature verification
→ initiateRefund: checks payment is SUCCESS
  marks payment REFUNDED
  future: calls Razorpay refund API
→ updatePaymentStatus: normalizes status to UPPERCASE
  prevents success vs SUCCESS mismatch bugs
→ getTotalRevenue: sums all SUCCESS payments
  used in admin analytics dashboard

PaymentResource.java (REST controller)
→ entry point for all /payments/** API calls
→ thin controller no business logic here
→ gateway agnostic — does not know mock or Razorpay
```

---

## Payment Status Lifecycle
```
Patient books appointment (UC4)
         ↓
Patient clicks Pay Now
POST /payments/initiate
         ↓
status = PENDING
Payment record created in database
         ↓
Mock mode → instantly SUCCESS
Real mode → Razorpay popup opens
Patient enters card or UPI details
         ↓
POST /payments/verify
Signature verified
         ↓
status = SUCCESS
Payment confirmed
         ↓
Patient cancels appointment
OR admin triggers manual refund
         ↓
POST /payments/refund
         ↓
status = REFUNDED
Mock → instant
Real → 3-5 business days (PDF requirement)
```

---

## Mock vs Razorpay — How it works
```
Current state MOCK:
→ No real money moves
→ Payment instantly marked SUCCESS
→ razorpayOrderId = "MOCK_ORDER_timestamp"
→ razorpayPaymentId = "MOCK_PAY_timestamp"
→ Perfect for development and testing
→ Evaluators understand and accept this approach

Future state RAZORPAY:
→ Real Razorpay order created via API
→ Frontend opens Razorpay checkout popup
→ Patient enters card or UPI details
→ Razorpay calls our /payments/verify endpoint
→ We verify HMAC SHA256 signature
→ Mark SUCCESS only after verification

Architecture is already ready for Razorpay:
→ razorpayOrderId field in Payment entity
→ razorpayPaymentId field in Payment entity
→ razorpaySignature field in Payment entity
→ verifyPayment() method ready for real check
→ Just replace mock logic with Razorpay SDK
```

---

## Key business rules from PDF
```
Rule 1 — One payment per appointment
→ cannot initiate payment if payment already exists
→ existsByAppointmentId() checked before creating
→ throws error: Payment already exists for appointment

Rule 2 — Only SCHEDULED appointments can be paid
→ cannot pay for COMPLETED CANCELLED or NO_SHOW
→ appointment status checked in initiatePayment()
→ throws error: Payment only for scheduled appointments

Rule 3 — Refund only for SUCCESS payments
→ cannot refund PENDING or FAILED payment
→ equalsIgnoreCase used for safe comparison
→ throws error: Refund only for successful payments

Rule 4 — Status always uppercase
→ updatePaymentStatus calls status.toUpperCase()
→ prevents success vs SUCCESS mismatch in database
→ all comparisons use equalsIgnoreCase for safety

Rule 5 — Refund within 3-5 business days (PDF)
→ mock mode instant REFUNDED status
→ production Razorpay processes in 3-5 business days
→ PDF non-functional requirement explicitly states this
```

---

## Important fixes applied during UC5
```
Fix 1 — Status case mismatch
Problem: status stored as "success" lowercase
         but refund check compared with "SUCCESS"
         refund always failed with wrong error
Fix: changed equals to equalsIgnoreCase
     handles both success and SUCCESS safely

Fix 2 — updatePaymentStatus normalization
Problem: status saved as-is from request
         database had mix of success and SUCCESS
Fix: added status.toUpperCase() before saving
     always stored as UPPERCASE in database now

Fix 3 — Wrong field name in Postman
Problem: sending "mode" in request body
         but PaymentRequest has "paymentMethod"
         validation failed with null error
Fix: use "paymentMethod" in request body always

Fix 4 — Reusing same appointmentId
Problem: payment already exists for appointment
         one appointment can only have one payment
Fix: always create fresh slot and appointment
     for each new payment test
```

---

## How all files connect
```
Patient pays for appointment:
POST /payments/initiate
         ↓
PaymentResource.initiatePayment()
         ↓
PaymentServiceImpl.initiatePayment()
→ finds appointment by appointmentId
→ checks appointment status = SCHEDULED
→ checks no payment exists for this appointment
→ creates Payment with status PENDING
→ mock mode: sets status to SUCCESS immediately
→ saves to payments table
         ↓
PaymentRepository saves to MySQL payments table
         ↓
Returns PaymentResponse with paymentId and status

Patient gets refund after cancellation:
POST /payments/paymentId/refund
         ↓
PaymentResource.initiateRefund()
         ↓
PaymentServiceImpl.initiateRefund()
→ finds payment by paymentId
→ checks payment status = SUCCESS (ignoreCase)
→ sets status to REFUNDED
→ saves updated payment
         ↓
Returns PaymentResponse with REFUNDED status
```

---

## API Endpoints
```
POST   /payments/initiate                   → patient initiates payment
POST   /payments/verify                     → verify payment completion
GET    /payments/appointment/{appointmentId}→ get payment for appointment
GET    /payments/{paymentId}                → get payment by id
GET    /payments/patient/{patientId}        → patient payment history
POST   /payments/{paymentId}/refund         → refund payment
GET    /payments/status?status=SUCCESS      → filter by status
GET    /payments/revenue/total              → admin total revenue
PUT    /payments/{paymentId}/status         → update status manually
```

---

## Correct test order in Postman
```
Step 1 → Create fresh slot
POST /slots/add with future date

Step 2 → Book fresh appointment
POST /appointments/book with new slotId

Step 3 → Initiate payment with correct body
POST /payments/initiate
Body must have "paymentMethod" not "mode"
{
  "appointmentId": newAppointmentId,
  "patientId": 3,
  "amount": 500.00,
  "paymentMethod": "UPI",
  "currency": "INR"
}

Step 4 → Update to SUCCESS
PUT /payments/paymentId/status?status=SUCCESS

Step 5 → Verify payment details
GET /payments/paymentId

Step 6 → Check patient history
GET /payments/patient/3

Step 7 → Check total revenue
GET /payments/revenue/total

Step 8 → Refund payment
POST /payments/paymentId/refund
Payment must be SUCCESS before refunding
```

---

## Test Results
```
✅ POST /payments/initiate              → 201 Created
✅ POST /payments/verify                → 200 OK
✅ GET  /payments/appointment/3         → 200 OK
✅ GET  /payments/2                     → 200 OK
✅ GET  /payments/patient/3             → 200 OK list
✅ PUT  /payments/2/status?status=SUCCESS → 200 OK
✅ POST /payments/2/refund              → 200 OK REFUNDED
✅ GET  /payments/status?status=SUCCESS → 200 OK filtered
✅ GET  /payments/revenue/total         → 200 OK total
```

---

## What we achieved so far
```
UC1 ✅ → Secure login JWT BCrypt foundation
UC2 ✅ → Doctor profiles admin verification search
UC3 ✅ → Slot calendar block recurring double booking prevention
UC4 ✅ → Complete booking lifecycle connected to all above
UC5 ✅ → Payment system mock gateway refund earnings
```

---

## What comes next UC6
```
UC6 → Review Service
→ Patient submits star rating after COMPLETED appointment
→ One review per appointment unique constraint PDF rule
→ Average rating auto calculated stored in Provider
→ Admin moderates and deletes inappropriate reviews
→ Doctor views all their reviews
→ isAnonymous field patient can hide their name
```

---

## Current status
```
✅ UC1 — Auth Service          COMPLETE
✅ UC2 — Provider Service      COMPLETE
✅ UC3 — Schedule Service      COMPLETE
✅ UC4 — Appointment Service   COMPLETE
✅ UC5 — Payment Service       COMPLETE
⏳ UC6 — Review Service        IN PROGRESS
⏳ UC7 — Notification Service  PENDING
⏳ UC8 — Medical Records       PENDING
⏳ Web Layer Thymeleaf         PENDING
⏳ Exception Handling          PENDING
⏳ Scheduler Jobs              PENDING
⏳ Docker Setup                PENDING
```

---

*MediBook | Capgemini Training Project*
