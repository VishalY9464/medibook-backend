package com.medibook.payment.service;

import com.medibook.payment.dto.PaymentRequest;
import com.medibook.payment.dto.PaymentResponse;
import com.medibook.payment.entity.Payment;

import java.util.List;

/*
 * This is the Service Interface for Payment.
 *
 * Think of it like the rules book for the accounts department.
 * It defines WHAT payment operations are possible.
 * PaymentServiceImpl defines HOW each operation works.
 *
 * Why interface first?
 * → Defines contract before writing logic
 * → PaymentResource depends on this interface not implementation
 * → When we swap mock → Razorpay:
 *   we only change PaymentServiceImpl
 *   this interface stays EXACTLY the same
 *   PaymentResource stays EXACTLY the same
 *   Zero breaking changes anywhere
 *
 * This is the key to our Razorpay-ready architecture.
 * Interface = stable contract
 * Impl = swappable logic
 */
public interface PaymentService {

    /*
     * Initiate a new payment for an appointment.
     *
     * MOCK MODE:
     * → creates Payment record with status=PENDING
     * → immediately marks as SUCCESS
     * → returns PaymentResponse with mock IDs
     *
     * RAZORPAY MODE (one method change in impl):
     * → creates Razorpay order via API
     * → saves Payment record with status=PENDING
     * → returns real orderId for frontend popup
     * → status stays PENDING until patient completes payment
     *
     * Called by: Patient after booking appointment
     * UI: Patient clicks "Pay Now" button
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /*
     * Verify payment after patient completes it.
     *
     * MOCK MODE:
     * → finds payment by orderId
     * → skips signature verification
     * → sets status = SUCCESS
     * → returns updated PaymentResponse
     *
     * RAZORPAY MODE (one method change in impl):
     * → receives orderId + paymentId + signature from frontend
     * → verifies: HMAC-SHA256(orderId|paymentId, secret) == signature
     * → if valid → status = SUCCESS
     * → if invalid → status = FAILED → throw exception
     *
     * Called by: Frontend after Razorpay popup completes
     * UI: Razorpay popup sends back payment details
     *     our frontend calls this to confirm
     */
    PaymentResponse verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    );

    /*
     * Get payment details for a specific appointment.
     *
     * Used when:
     * → Patient views their payment receipt
     * → Doctor checks if appointment is paid
     * → Admin verifies payment status
     *
     * Called by: Patient/Doctor/Admin viewing appointment
     * UI: Appointment details page shows payment status
     */
    PaymentResponse getPaymentByAppointment(int appointmentId);

    /*
     * Get payment by its own payment ID.
     *
     * Used when:
     * → Patient requests refund by paymentId
     * → Admin looks up specific transaction
     *
     * Called by: Admin or patient with paymentId
     * UI: Admin payment management page
     */
    PaymentResponse getPaymentById(int paymentId);

    /*
     * Get all payments made by a specific patient.
     *
     * Patient views their complete payment history.
     * All statuses shown — pending, success, failed, refunded.
     * Shown chronologically newest first.
     *
     * Called by: Patient viewing their dashboard
     * UI: Patient dashboard → Payment History tab
     */
    List<Payment> getPaymentsByPatient(int patientId);

    /*
     * Initiate refund for a payment.
     *
     * MOCK MODE:
     * → finds payment by paymentId
     * → verifies status is SUCCESS (cannot refund failed payment)
     * → sets status = REFUNDED
     * → returns updated PaymentResponse
     *
     * RAZORPAY MODE (one method change in impl):
     * → calls Razorpay refund API with real paymentId
     * → Razorpay processes refund
     * → sets status = REFUNDED
     * → patient gets money back in 5-7 days
     *
     * Called by: AppointmentService when appointment cancelled
     * UI: Patient cancels appointment → refund auto triggered
     *
     * This is called internally — patient does not call
     * this directly. cancelAppointment() in UC4 calls this.
     */
    PaymentResponse initiateRefund(int paymentId);

    /*
     * Get all payments with a specific status.
     *
     * Admin uses this to filter payments.
     * Example: see all FAILED payments to investigate
     * Example: see all PENDING payments to follow up
     * Example: see all REFUNDED payments for accounting
     *
     * Called by: Admin from payment management page
     * UI: Admin dashboard → Payments → filter by status
     */
    List<Payment> getPaymentsByStatus(String status);

    /*
     * Get total revenue from all successful payments.
     *
     * Used in admin analytics dashboard.
     * Shows total money platform has earned.
     * Example: "Total Revenue: ₹1,25,000"
     *
     * Called by: Admin analytics dashboard
     * UI: Admin dashboard → Revenue card
     */
    double getTotalRevenue();

    /*
     * Update payment status manually.
     *
     * Used by admin to fix incorrect payment statuses.
     * Also used internally when Razorpay webhook
     * sends payment confirmation or failure.
     *
     * Called by: Admin or Razorpay webhook handler
     * UI: Admin payment management → update status
     */
    void updatePaymentStatus(int paymentId, String status);
}