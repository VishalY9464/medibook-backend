package com.medibook.payment.resource;

import com.medibook.payment.dto.PaymentRequest;
import com.medibook.payment.dto.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * This is the REST Controller for Payment.
 *
 * Think of it like the payment counter at a hospital.
 * Patient walks up to counter (sends HTTP request).
 * Counter staff (this controller) takes the request.
 * Passes it to accounts department (PaymentService).
 * Returns receipt back to patient.
 *
 * No business logic here — just receive and respond.
 * All logic lives in PaymentServiceImpl.
 *
 * No gateway code here — completely gateway agnostic.
 * Whether mock or Razorpay runs behind the scenes,
 * this controller does not know and does not care.
 *
 * @RestController → handles REST API requests
 * @RequestMapping → all URLs start with /payments
 */
@RestController
@RequestMapping("/payments")
public class PaymentResource {

    /*
     * Inject PaymentService.
     * We depend on interface not implementation.
     * This is why controller stays unchanged
     * when we swap mock to Razorpay.
     */
    @Autowired
    private PaymentService paymentService;

    /*
     * Initiate a new payment for an appointment.
     *
     * Who calls this: Patient after booking appointment
     * When: Patient clicks "Pay Now" button
     * What happens:
     *   MOCK → instantly SUCCESS
     *   RAZORPAY → creates order, returns orderId
     *              frontend uses orderId to open popup
     *
     * URL: POST /payments/initiate
     * Body: PaymentRequest (appointmentId, patientId, amount, method)
     * Returns: 201 Created with payment details
     *
     * UI Screen: Patient Dashboard → Appointment Details → Pay Now
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {

        // call service to initiate payment
        PaymentResponse response = paymentService
                .initiatePayment(request);

        // return 201 Created with payment response
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Verify payment after patient completes it.
     *
     * Who calls this: Frontend after payment completed
     * When:
     *   MOCK → call directly from Swagger/Postman
     *   RAZORPAY → Razorpay popup closes, frontend auto calls this
     * What happens: Verifies signature, marks SUCCESS or FAILED
     *
     * URL: POST /payments/verify
     * Body: orderId + paymentId + signature
     * Returns: 200 OK with updated payment status
     *
     * UI Screen: Payment result page (auto called by frontend)
     * In Swagger: manually send mock IDs to test
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @RequestBody Map<String, String> body) {

        // extract verification details from request body
        String orderId = body.get("razorpayOrderId");
        String paymentId = body.get("razorpayPaymentId");

        // signature is null in mock mode — that is fine
        // in Razorpay mode → real signature sent by Razorpay
        String signature = body.get("razorpaySignature");

        // call service to verify payment
        PaymentResponse response = paymentService
                .verifyPayment(orderId, paymentId, signature);

        // return 200 OK with verified payment response
        return ResponseEntity.ok(response);
    }

    /*
     * Get payment details for a specific appointment.
     *
     * Who calls this: Patient / Doctor / Admin
     * When: Viewing appointment details page
     * What happens: Returns payment status and details
     *
     * URL: GET /payments/appointment/{appointmentId}
     * Returns: 200 OK with payment details
     *
     * UI Screen:
     * Patient → Appointment details → Payment section
     * Doctor  → Patient appointment → check payment status
     * Admin   → Appointment management → payment info
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<PaymentResponse> getByAppointment(
            @PathVariable int appointmentId) {

        // call service to get payment by appointment
        return ResponseEntity.ok(
                paymentService.getPaymentByAppointment(appointmentId)
        );
    }

    /*
     * Get payment by its own payment ID.
     *
     * Who calls this: Admin / Patient with paymentId
     * When: Looking up specific transaction
     * What happens: Returns full payment record
     *
     * URL: GET /payments/{paymentId}
     * Returns: 200 OK with payment details
     *
     * UI Screen: Admin payment management page
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable int paymentId) {

        // call service to get payment by id
        return ResponseEntity.ok(
                paymentService.getPaymentById(paymentId)
        );
    }

    /*
     * Get all payments made by a specific patient.
     *
     * Who calls this: Patient viewing their history
     * When: Patient opens payment history tab
     * What happens: Returns all payments for this patient
     *
     * URL: GET /payments/patient/{patientId}
     * Returns: 200 OK with list of payments
     *
     * UI Screen: Patient Dashboard → Payment History tab
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Payment>> getByPatient(
            @PathVariable int patientId) {

        // call service to get all payments for patient
        return ResponseEntity.ok(
                paymentService.getPaymentsByPatient(patientId)
        );
    }

    /*
     * Initiate refund for a payment.
     *
     * Who calls this: Patient cancelling appointment
     *                 OR admin processing manual refund
     * When: Patient cancels appointment → auto triggered
     *       OR admin manually initiates refund
     * What happens:
     *   MOCK → instantly REFUNDED
     *   RAZORPAY → real refund API called, 5-7 days
     *
     * URL: POST /payments/{paymentId}/refund
     * Returns: 200 OK with refund confirmation
     *
     * UI Screen:
     * Auto triggered → no direct UI (called by cancelAppointment)
     * Admin → Payment management → Refund button
     *
     * NOTE: This is usually called internally by
     * AppointmentService.cancelAppointment() not by patient directly.
     * But admin can call it manually if needed.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> initiateRefund(
            @PathVariable int paymentId) {

        // call service to initiate refund
        PaymentResponse response = paymentService
                .initiateRefund(paymentId);

        // return 200 OK with refund details
        return ResponseEntity.ok(response);
    }

    /*
     * Get all payments with a specific status.
     *
     * Who calls this: Admin
     * When: Admin filters payment list by status
     * What happens: Returns all payments matching status
     *
     * URL: GET /payments/status?status=FAILED
     * Examples:
     *   ?status=PENDING  → uninitiated payments
     *   ?status=SUCCESS  → confirmed payments
     *   ?status=FAILED   → failed attempts
     *   ?status=REFUNDED → refunded payments
     * Returns: 200 OK with filtered payment list
     *
     * UI Screen: Admin Dashboard → Payments → Filter by Status
     */
    @GetMapping("/status")
    public ResponseEntity<List<Payment>> getByStatus(
            @RequestParam String status) {

        // call service to get payments by status
        return ResponseEntity.ok(
                paymentService.getPaymentsByStatus(status)
        );
    }

    /*
     * Get total revenue from all successful payments.
     *
     * Who calls this: Admin
     * When: Admin views revenue analytics
     * What happens: Returns sum of all SUCCESS payments
     *
     * URL: GET /payments/revenue/total
     * Returns: 200 OK with total revenue amount
     *
     * UI Screen: Admin Dashboard → Revenue card
     * Example display: "Total Revenue: ₹1,25,000"
     */
    @GetMapping("/revenue/total")
    public ResponseEntity<?> getTotalRevenue() {

        // call service to get total revenue
        double total = paymentService.getTotalRevenue();

        // return formatted revenue response
        return ResponseEntity.ok(Map.of(
                "totalRevenue", total,
                "currency", "INR",
                "message", "Total revenue from all successful payments"
        ));
    }

    /*
     * Update payment status manually.
     *
     * Who calls this: Admin / Razorpay webhook handler
     * When: Admin fixes incorrect status
     *       OR Razorpay webhook sends payment update
     * What happens: Updates payment status to given value
     *
     * URL: PUT /payments/{paymentId}/status?status=SUCCESS
     * Returns: 200 OK with success message
     *
     * UI Screen: Admin → Payment Management → Update Status
     *
     * RAZORPAY WEBHOOK NOTE:
     * When Razorpay integrated → add POST /payments/webhook
     * Razorpay calls this automatically when payment succeeds
     * Webhook calls updatePaymentStatus() internally
     * We add webhook endpoint in web layer UC
     */
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable int paymentId,
            @RequestParam String status) {

        // call service to update status
        paymentService.updatePaymentStatus(paymentId, status);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Payment status updated to: " + status,
                "paymentId", paymentId
        ));
    }
}