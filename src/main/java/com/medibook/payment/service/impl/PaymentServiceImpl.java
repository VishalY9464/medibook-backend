package com.medibook.payment.service.impl;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.payment.dto.PaymentRequest;
import com.medibook.payment.dto.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * This is the actual business logic for Payment Service.
 *
 * Think of it like the accounts department doing the actual work.
 * PaymentService (interface) says WHAT needs to be done.
 * This class actually DOES it.
 *
 * ================================================================
 * MOST IMPORTANT THING TO UNDERSTAND — RAZORPAY SWAP DESIGN:
 * ================================================================
 *
 * All gateway logic is isolated in ONE private method:
 * callGateway(PaymentRequest request)
 *
 * RIGHT NOW it calls → mockGateway()
 *
 * WHEN RAZORPAY READY — change ONE LINE:
 * return mockGateway(request)
 * becomes:
 * return razorpayGateway(request)
 *
 * NOTHING ELSE CHANGES. Not a single other line.
 * Same for refund:
 * callRefundGateway(paymentId) → mockRefund() now
 * → razorpayRefund() later — one line change
 *
 * This is called the Strategy Pattern.
 * Gateway is a swappable strategy.
 * Business logic is completely independent of gateway.
 * ================================================================
 *
 * Connections to other services:
 * → AppointmentService (UC4) → verify appointment exists
 *   and check appointment status before payment
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    /*
     * PaymentRepository — connection to payments table.
     * Spring injects automatically.
     */
    @Autowired
    private PaymentRepository paymentRepository;

    /*
     * AppointmentService — we verify appointment exists
     * and check status before processing payment.
     * This is how UC5 talks to UC4.
     */
    @Autowired
    private AppointmentService appointmentService;

    /*
     * ============================================================
     * GATEWAY ABSTRACTION — THE RAZORPAY SWAP POINT
     * ============================================================
     *
     * This inner class holds the gateway response.
     * Both mock and Razorpay return this same structure.
     * Rest of the code only knows about GatewayResponse.
     * It does not care if mock or real gateway produced it.
     */
    private static class GatewayResponse {
        String orderId;
        String paymentId;
        String status;

        GatewayResponse(String orderId, String paymentId, String status) {
            this.orderId = orderId;
            this.paymentId = paymentId;
            this.status = status;
        }
    }

    /*
     * ============================================================
     * THIS IS THE ONLY METHOD YOU CHANGE FOR RAZORPAY
     * ============================================================
     *
     * RIGHT NOW → calls mockGateway()
     *
     * WHEN RAZORPAY READY:
     * Change return mockGateway(request)
     * to     return razorpayGateway(request)
     *
     * That is literally the only change needed.
     * One line. Nothing else.
     */
    private GatewayResponse callGateway(PaymentRequest request) {

        // RIGHT NOW: mock gateway
        return mockGateway(request);

        // RAZORPAY SWAP: uncomment below and comment above
        // return razorpayGateway(request);
    }

    /*
     * ============================================================
     * THIS IS THE ONLY METHOD YOU CHANGE FOR RAZORPAY REFUND
     * ============================================================
     *
     * RIGHT NOW → calls mockRefund()
     *
     * WHEN RAZORPAY READY:
     * Change return mockRefund(razorpayPaymentId)
     * to     return razorpayRefund(razorpayPaymentId)
     *
     * One line. Nothing else.
     */
    private boolean callRefundGateway(String razorpayPaymentId) {

        // RIGHT NOW: mock refund
        return mockRefund(razorpayPaymentId);

        // RAZORPAY SWAP: uncomment below and comment above
        // return razorpayRefund(razorpayPaymentId);
    }

    /*
     * ============================================================
     * MOCK GATEWAY — works right now, no external dependency
     * ============================================================
     *
     * Simulates what Razorpay would do.
     * Creates fake orderId and paymentId.
     * Instantly returns SUCCESS.
     * No network call. No external account needed.
     * Perfect for testing all business logic.
     *
     * When Razorpay integrated → this method stays here
     * but callGateway() stops calling it.
     * Keep it for unit testing purposes.
     */
    private GatewayResponse mockGateway(PaymentRequest request) {

        // generate mock order ID using appointmentId
        // format: MOCK_ORDER_5_1712345678
        String mockOrderId = "MOCK_ORDER_"
                + request.getAppointmentId()
                + "_"
                + System.currentTimeMillis();

        // generate mock payment ID using timestamp
        // format: MOCK_PAY_1712345678
        String mockPaymentId = "MOCK_PAY_"
                + System.currentTimeMillis();

        // mock gateway always returns SUCCESS
        // real Razorpay returns PENDING until patient pays
        return new GatewayResponse(mockOrderId, mockPaymentId, "SUCCESS");
    }

    /*
     * ============================================================
     * MOCK REFUND — works right now
     * ============================================================
     *
     * Simulates Razorpay refund.
     * Just returns true — refund always succeeds in mock.
     * When Razorpay integrated → razorpayRefund() handles real API.
     */
    private boolean mockRefund(String razorpayPaymentId) {

        // mock refund always succeeds
        // real Razorpay would call their API and return true/false
        return true;
    }

    /*
     * ============================================================
     * RAZORPAY GATEWAY — uncomment when ready to integrate
     * ============================================================
     *
     * This is already written and waiting.
     * When you get Razorpay keys:
     * 1. Add razorpay-java dependency to pom.xml
     * 2. Add keys to application.properties
     * 3. Uncomment this method
     * 4. Change callGateway() to call this instead of mock
     * DONE. Full real payment works.
     *
     * private GatewayResponse razorpayGateway(PaymentRequest request) {
     *     try {
     *         RazorpayClient client = new RazorpayClient(keyId, keySecret);
     *
     *         JSONObject orderRequest = new JSONObject();
     *         orderRequest.put("amount", (int)(request.getAmount() * 100));
     *         orderRequest.put("currency", request.getCurrency());
     *         orderRequest.put("receipt", "appt_" + request.getAppointmentId());
     *
     *         Order order = client.orders.create(orderRequest);
     *
     *         return new GatewayResponse(
     *             order.get("id"),     // real orderId from Razorpay
     *             null,                // paymentId comes after patient pays
     *             "PENDING"            // stays pending until patient pays
     *         );
     *     } catch (RazorpayException e) {
     *         throw new RuntimeException("Razorpay order creation failed: " + e.getMessage());
     *     }
     * }
     *
     * private boolean razorpayRefund(String razorpayPaymentId) {
     *     try {
     *         RazorpayClient client = new RazorpayClient(keyId, keySecret);
     *         JSONObject refundRequest = new JSONObject();
     *         Payment refund = client.payments.refund(razorpayPaymentId, refundRequest);
     *         return refund.get("status").equals("processed");
     *     } catch (RazorpayException e) {
     *         throw new RuntimeException("Razorpay refund failed: " + e.getMessage());
     *     }
     * }
     */

    /*
     * ============================================================
     * BUSINESS LOGIC METHODS — these NEVER change regardless
     * of mock or Razorpay. Gateway swap is completely invisible here.
     * ============================================================
     */

    /*
     * Initiate a new payment for an appointment.
     *
     * How it works:
     * 1. Verify appointment exists (UC4)
     * 2. Check appointment is SCHEDULED status
     * 3. Check payment does not already exist for this appointment
     * 4. Call gateway (mock now, Razorpay later)
     * 5. Save payment record to database
     * 6. Return PaymentResponse to patient
     *
     * @Transactional — if gateway call succeeds but save fails
     * → everything rolls back → no orphan records
     */
    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // step 1 — verify appointment exists in UC4
        // throws exception if not found
        Appointment appointment = appointmentService
                .getById(request.getAppointmentId());

        // step 2 — appointment must be SCHEDULED to pay
        // cannot pay for cancelled or completed appointment
        if (!appointment.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException(
                "Payment can only be made for scheduled appointments. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // step 3 — check if payment already exists for this appointment
        // cannot pay twice for same appointment
        if (paymentRepository.findByAppointmentId(
                request.getAppointmentId()).isPresent()) {
            throw new RuntimeException(
                "Payment already exists for appointment: "
                + request.getAppointmentId() +
                ". Use refund if you need to cancel."
            );
        }

        // step 4 — call gateway (mock now, Razorpay later)
        // this is the only line that changes when we integrate
        GatewayResponse gatewayResponse = callGateway(request);

        // step 5 — build payment record from gateway response
        Payment payment = Payment.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(gatewayResponse.status)
                .razorpayOrderId(gatewayResponse.orderId)
                .razorpayPaymentId(gatewayResponse.paymentId)
                .notes("Payment initiated via " + request.getPaymentMethod())
                .build();

        // step 6 — save payment to database
        Payment saved = paymentRepository.save(payment);

        // step 7 — build and return response
        return buildResponse(saved, "Payment processed successfully.");
    }

    /*
     * Verify payment after patient completes it.
     *
     * MOCK MODE:
     * → finds payment by orderId
     * → skips signature check
     * → marks SUCCESS if not already
     *
     * RAZORPAY MODE (when integrated):
     * → verifies HMAC-SHA256 signature
     * → if valid → SUCCESS
     * → if invalid → FAILED (payment tampered)
     *
     * How it works:
     * 1. Find payment by Razorpay order ID
     * 2. Verify signature (mock skips this)
     * 3. Update payment with paymentId and signature
     * 4. Set status to SUCCESS
     * 5. Save and return response
     */
    @Override
    @Transactional
    public PaymentResponse verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        // step 1 — find payment by orderId
        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException(
                    "Payment not found for order: " + razorpayOrderId
                ));

        // step 2 — signature verification
        // MOCK MODE: skip verification — always valid
        // RAZORPAY MODE: verify HMAC-SHA256 signature
        // When Razorpay integrated — add this check:
        // String expectedSignature = HmacSHA256(orderId + "|" + paymentId, secret)
        // if (!expectedSignature.equals(razorpaySignature)) → FAILED

        // step 3 — update payment with real Razorpay details
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);

        // step 4 — mark as SUCCESS
        payment.setStatus("SUCCESS");
        payment.setNotes("Payment verified and confirmed.");

        // step 5 — save updated payment
        Payment saved = paymentRepository.save(payment);

        // return success response
        return buildResponse(saved, "Payment verified successfully. Appointment confirmed.");
    }

    /*
     * Get payment details for a specific appointment.
     *
     * Most commonly used query.
     * Patient views receipt after paying.
     * Doctor checks if appointment is paid.
     */
    @Override
    public PaymentResponse getPaymentByAppointment(int appointmentId) {

        // find payment by appointmentId
        Payment payment = paymentRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException(
                    "No payment found for appointment: " + appointmentId
                ));

        // return payment details
        return buildResponse(payment, "Payment details retrieved.");
    }

    /*
     * Get payment by its own payment ID.
     *
     * Used by admin and refund requests.
     */
    @Override
    public PaymentResponse getPaymentById(int paymentId) {

        // find payment by paymentId
        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException(
                    "Payment not found with id: " + paymentId
                ));

        // return payment details
        return buildResponse(payment, "Payment details retrieved.");
    }

    /*
     * Get all payments made by a specific patient.
     *
     * Patient payment history on dashboard.
     * Shows all statuses.
     */
    @Override
    public List<Payment> getPaymentsByPatient(int patientId) {

        // get all payments for this patient
        return paymentRepository.findByPatientId(patientId);
    }

    /*
     * Initiate refund for a payment.
     *
     * How it works:
     * 1. Find payment by paymentId
     * 2. Verify payment status is SUCCESS
     *    (cannot refund failed or pending payment)
     * 3. Call refund gateway (mock now, Razorpay later)
     * 4. Update status to REFUNDED
     * 5. Save and return response
     *
     * Called by: AppointmentService.cancelAppointment() in UC4
     * Patient does not call this directly.
     * Cancelling appointment auto triggers refund.
     */
    @Override
    @Transactional
    public PaymentResponse initiateRefund(int paymentId) {

        // step 1 — find the payment
        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException(
                    "Payment not found with id: " + paymentId
                ));

        // step 2 — can only refund successful payments
        // cannot refund a payment that failed or is pending
        if (!payment.getStatus().equals("SUCCESS")) {
            throw new RuntimeException(
                "Refund can only be initiated for successful payments. " +
                "Current status: " + payment.getStatus()
            );
        }

        // step 3 — call refund gateway
        // mock → returns true always
        // Razorpay → calls real refund API
        boolean refundSuccess = callRefundGateway(
                payment.getRazorpayPaymentId()
        );

        // step 4 — update payment status based on result
        if (refundSuccess) {
            payment.setStatus("REFUNDED");
            payment.setNotes(
                "Refund initiated. " +
                "Mock mode: instant. " +
                "Razorpay mode: 5-7 business days."
            );
        } else {
            throw new RuntimeException(
                "Refund failed. Please contact support."
            );
        }

        // step 5 — save updated payment
        Payment saved = paymentRepository.save(payment);

        // return refund response
        return buildResponse(saved, "Refund initiated successfully.");
    }

    /*
     * Get all payments with a specific status.
     * Admin analytics and filtering.
     */
    @Override
    public List<Payment> getPaymentsByStatus(String status) {

        // get all payments with this status
        return paymentRepository.findByStatus(status);
    }

    /*
     * Get total revenue from all successful payments.
     * Admin dashboard analytics.
     */
    @Override
    public double getTotalRevenue() {

        // get total from repository
        // handle null case — if no payments yet return 0
        Double total = paymentRepository.calculateTotalRevenue();
        return total != null ? total : 0.0;
    }

    /*
     * Update payment status manually.
     * Admin fix or webhook handler.
     */
    @Override
    public void updatePaymentStatus(int paymentId, String status) {

        // find payment
        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException(
                    "Payment not found with id: " + paymentId
                ));

        // update status
        payment.setStatus(status);
        payment.setNotes("Status manually updated to: " + status);

        // save — @PreUpdate auto updates updatedAt
        paymentRepository.save(payment);
    }

    /*
     * ============================================================
     * HELPER METHOD — builds PaymentResponse from Payment entity
     * ============================================================
     *
     * Used by every method above to build response.
     * Keeps response building in one place.
     * If response structure changes → change here only.
     */
    private PaymentResponse buildResponse(Payment payment, String message) {

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .appointmentId(payment.getAppointmentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .message(message)
                .transactionTime(
                    payment.getCreatedAt() != null
                        ? payment.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .build();
    }
}