package com.medibook.payment.service.impl;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.exception.BadRequestException;
import com.medibook.exception.DuplicateResourceException;
import com.medibook.exception.ResourceNotFoundException;
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
 * RAZORPAY SWAP DESIGN:
 * All gateway logic isolated in ONE private method callGateway().
 * RIGHT NOW → calls mockGateway()
 * WHEN RAZORPAY READY → change ONE LINE to razorpayGateway()
 * Nothing else changes anywhere.
 *
 * Exception handling:
 * → ResourceNotFoundException  → 404 when payment not found
 * → DuplicateResourceException → 409 when payment already exists
 * → BadRequestException        → 400 when business rule violated
 * All caught by GlobalExceptionHandler → clean JSON response always
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentService appointmentService;

    /*
     * Inner class holds gateway response.
     * Both mock and Razorpay return this same structure.
     * Rest of code only knows about GatewayResponse.
     */
    private static class GatewayResponse {
        String orderId;
        String paymentId;
        String status;

        GatewayResponse(String orderId,
                        String paymentId,
                        String status) {
            this.orderId = orderId;
            this.paymentId = paymentId;
            this.status = status;
        }
    }

    /*
     * THIS IS THE ONLY METHOD YOU CHANGE FOR RAZORPAY.
     * RIGHT NOW → calls mockGateway()
     * WHEN RAZORPAY READY → return razorpayGateway(request)
     * One line. Nothing else changes.
     */
    private GatewayResponse callGateway(PaymentRequest request) {
        return mockGateway(request);
        // RAZORPAY SWAP: return razorpayGateway(request);
    }

    /*
     * THIS IS THE ONLY METHOD YOU CHANGE FOR RAZORPAY REFUND.
     * RIGHT NOW → calls mockRefund()
     * WHEN RAZORPAY READY → return razorpayRefund(razorpayPaymentId)
     * One line. Nothing else changes.
     */
    private boolean callRefundGateway(String razorpayPaymentId) {
        return mockRefund(razorpayPaymentId);
        // RAZORPAY SWAP: return razorpayRefund(razorpayPaymentId);
    }

    /*
     * Mock gateway — works right now, no external dependency.
     * Creates fake orderId and paymentId.
     * Instantly returns SUCCESS.
     * Keep this even after Razorpay — useful for unit tests.
     */
    private GatewayResponse mockGateway(PaymentRequest request) {

        String mockOrderId = "MOCK_ORDER_"
                + request.getAppointmentId()
                + "_"
                + System.currentTimeMillis();

        String mockPaymentId = "MOCK_PAY_"
                + System.currentTimeMillis();

        return new GatewayResponse(
                mockOrderId, mockPaymentId, "SUCCESS"
        );
    }

    /*
     * Mock refund — always succeeds.
     * When Razorpay integrated → razorpayRefund() handles real API.
     */
    private boolean mockRefund(String razorpayPaymentId) {
        return true;
    }

    /*
     * RAZORPAY GATEWAY — uncomment when ready to integrate.
     *
     * private GatewayResponse razorpayGateway(PaymentRequest request) {
     *     try {
     *         RazorpayClient client =
     *             new RazorpayClient(keyId, keySecret);
     *         JSONObject orderRequest = new JSONObject();
     *         orderRequest.put("amount",
     *             (int)(request.getAmount() * 100));
     *         orderRequest.put("currency", request.getCurrency());
     *         orderRequest.put("receipt",
     *             "appt_" + request.getAppointmentId());
     *         Order order = client.orders.create(orderRequest);
     *         return new GatewayResponse(
     *             order.get("id"), null, "PENDING"
     *         );
     *     } catch (RazorpayException e) {
     *         throw new BadRequestException(
     *             "Razorpay order creation failed: "
     *             + e.getMessage()
     *         );
     *     }
     * }
     *
     * private boolean razorpayRefund(String razorpayPaymentId) {
     *     try {
     *         RazorpayClient client =
     *             new RazorpayClient(keyId, keySecret);
     *         JSONObject refundRequest = new JSONObject();
     *         Payment refund = client.payments.refund(
     *             razorpayPaymentId, refundRequest
     *         );
     *         return refund.get("status").equals("processed");
     *     } catch (RazorpayException e) {
     *         throw new BadRequestException(
     *             "Razorpay refund failed: " + e.getMessage()
     *         );
     *     }
     * }
     */

    /*
     * Initiate a new payment for an appointment.
     *
     * How it works:
     * 1. Verify appointment exists (UC4) — throws 404 if not
     * 2. Check appointment is SCHEDULED — throws 400 if not
     * 3. Check no payment already exists — throws 409 if exists
     * 4. Call gateway (mock now, Razorpay later)
     * 5. Save payment record to database
     * 6. Return PaymentResponse to patient
     */
    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // step 1 — verify appointment exists in UC4
        // throws 404 Not Found if appointment not found
        Appointment appointment = appointmentService
                .getById(request.getAppointmentId());

        // step 2 — appointment must be SCHEDULED to pay
        // throws 400 Bad Request if cancelled or completed
        if (!appointment.getStatus().equals("SCHEDULED")) {
            throw new BadRequestException(
                "Payment can only be made for scheduled appointments. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // step 3 — check payment already exists
        // throws 409 Conflict if payment already exists
        if (paymentRepository.findByAppointmentId(
                request.getAppointmentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Payment already exists for appointment: "
                + request.getAppointmentId()
            );
        }

        // step 4 — call gateway (mock now, Razorpay later)
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
                .notes("Payment initiated via "
                        + request.getPaymentMethod())
                .build();

        // step 6 — save payment to database
        Payment saved = paymentRepository.save(payment);

        return buildResponse(saved,
                "Payment processed successfully.");
    }

    /*
     * Verify payment after patient completes it.
     *
     * MOCK MODE → skips signature check → marks SUCCESS
     * RAZORPAY MODE → verifies HMAC-SHA256 signature
     *
     * Throws 404 if payment not found by orderId.
     */
    @Override
    @Transactional
    public PaymentResponse verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        // find payment by orderId
        // throws 404 Not Found if not found
        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment", "orderId", razorpayOrderId
                ));

        // check payment is not already verified
        if (payment.getStatus().equals("SUCCESS")) {
            throw new BadRequestException(
                "Payment is already verified and successful."
            );
        }

        // MOCK MODE: skip signature verification
        // RAZORPAY MODE: verify HMAC-SHA256 signature here
        // String expected = HmacSHA256(orderId+"|"+paymentId, secret)
        // if (!expected.equals(razorpaySignature))
        //     throw new BadRequestException("Invalid signature");

        // update payment with Razorpay details
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus("SUCCESS");
        payment.setNotes("Payment verified and confirmed.");

        Payment saved = paymentRepository.save(payment);

        return buildResponse(saved,
                "Payment verified successfully. " +
                "Appointment confirmed.");
    }

    /*
     * Get payment details for a specific appointment.
     * Patient views receipt after paying.
     * Throws 404 if no payment found for appointment.
     */
    @Override
    public PaymentResponse getPaymentByAppointment(
            int appointmentId) {

        Payment payment = paymentRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment", "appointmentId", appointmentId
                ));

        return buildResponse(payment,
                "Payment details retrieved.");
    }

    /*
     * Get payment by its own payment ID.
     * Used by admin and refund requests.
     * Throws 404 if payment not found.
     */
    @Override
    public PaymentResponse getPaymentById(int paymentId) {

        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment", "id", paymentId
                ));

        return buildResponse(payment,
                "Payment details retrieved.");
    }

    /*
     * Get all payments made by a specific patient.
     * Patient payment history on dashboard.
     */
    @Override
    public List<Payment> getPaymentsByPatient(int patientId) {
        return paymentRepository.findByPatientId(patientId);
    }

    /*
     * Initiate refund for a payment.
     *
     * How it works:
     * 1. Find payment — throws 404 if not found
     * 2. Verify status is SUCCESS — throws 400 if not
     * 3. Call refund gateway (mock now, Razorpay later)
     * 4. Update status to REFUNDED
     * 5. Save and return response
     *
     * Called by AppointmentService.cancelAppointment() in UC4.
     * Patient does not call this directly.
     */
    @Override
    @Transactional
    public PaymentResponse initiateRefund(int paymentId) {

        // find the payment — throws 404 if not found
        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment", "id", paymentId
                ));

        // can only refund successful payments
        // throws 400 if payment is pending or failed
        if (!payment.getStatus().equals("SUCCESS")) {
            throw new BadRequestException(
                "Refund can only be initiated for successful payments. " +
                "Current status: " + payment.getStatus()
            );
        }

        // check not already refunded
        if (payment.getStatus().equals("REFUNDED")) {
            throw new BadRequestException(
                "Payment has already been refunded."
            );
        }

        // call refund gateway
        // mock → returns true always
        // Razorpay → calls real refund API
        boolean refundSuccess = callRefundGateway(
                payment.getRazorpayPaymentId()
        );

        if (refundSuccess) {
            payment.setStatus("REFUNDED");
            payment.setNotes(
                "Refund initiated. Mock mode: instant. " +
                "Razorpay mode: 5-7 business days."
            );
        } else {
            throw new BadRequestException(
                "Refund failed. Please contact support."
            );
        }

        Payment saved = paymentRepository.save(payment);

        return buildResponse(saved,
                "Refund initiated successfully.");
    }

    /*
     * Get all payments with a specific status.
     * Admin analytics and filtering.
     * Validates status is one of allowed values.
     */
    @Override
    public List<Payment> getPaymentsByStatus(String status) {

        // validate status value
        if (!status.equals("PENDING")
                && !status.equals("SUCCESS")
                && !status.equals("FAILED")
                && !status.equals("REFUNDED")) {
            throw new BadRequestException(
                "Invalid status. Allowed values: " +
                "PENDING, SUCCESS, FAILED, REFUNDED"
            );
        }

        return paymentRepository.findByStatus(status);
    }

    /*
     * Get total revenue from all successful payments.
     * Admin dashboard analytics.
     */
    @Override
    public double getTotalRevenue() {
        Double total = paymentRepository.calculateTotalRevenue();
        return total != null ? total : 0.0;
    }

    /*
     * Update payment status manually.
     * Admin fix or webhook handler.
     * Throws 404 if payment not found.
     */
    @Override
    public void updatePaymentStatus(int paymentId, String status) {

        // validate status value
        if (!status.equals("PENDING")
                && !status.equals("SUCCESS")
                && !status.equals("FAILED")
                && !status.equals("REFUNDED")) {
            throw new BadRequestException(
                "Invalid status. Allowed values: " +
                "PENDING, SUCCESS, FAILED, REFUNDED"
            );
        }

        // find payment — throws 404 if not found
        Payment payment = paymentRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment", "id", paymentId
                ));

        payment.setStatus(status);
        payment.setNotes("Status manually updated to: " + status);

        // @PreUpdate auto updates updatedAt
        paymentRepository.save(payment);
    }

    /*
     * Helper method — builds PaymentResponse from Payment entity.
     * Used by every method above.
     * If response structure changes → change here only.
     */
    private PaymentResponse buildResponse(
            Payment payment, String message) {

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
                            DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"))
                        : LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"))
                )
                .build();
    }
}