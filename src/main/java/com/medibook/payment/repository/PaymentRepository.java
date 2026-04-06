package com.medibook.payment.repository;

import com.medibook.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * This is the Repository for Payment entity.
 *
 * Think of it like the accounts department of a hospital.
 * We ask questions like:
 * "Show me all payments for patient Rahul"
 * "Has appointment #5 been paid for?"
 * "What is the total revenue this month?"
 * "Show me all failed payments"
 *
 * Spring Data JPA writes SQL automatically
 * from method names we define here.
 *
 * RAZORPAY READY:
 * findByRazorpayPaymentId() is already here.
 * When Razorpay integrated → used to find payment
 * during webhook verification.
 * findByRazorpayOrderId() used to match order
 * when Razorpay sends payment confirmation.
 * Zero changes needed to this file when we swap.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /*
     * Find payment by appointment ID.
     * Most commonly used query in entire payment service.
     *
     * Used when:
     * → Patient views their payment receipt
     * → Doctor checks if appointment is paid
     * → Admin verifies payment before appointment
     * → Refund needs to find payment to update
     *
     * Returns Optional because payment might not exist yet
     * if patient has not paid after booking.
     */
    Optional<Payment> findByAppointmentId(int appointmentId);

    /*
     * Find all payments made by a specific patient.
     * Patient views their complete payment history.
     * Shown on patient dashboard as payment records.
     * Includes all statuses — pending, success, failed, refunded.
     */
    List<Payment> findByPatientId(int patientId);

    /*
     * Find all payments with a specific status.
     * Admin filters payments by status.
     *
     * Examples:
     * findByStatus("PENDING")  → payments not yet confirmed
     * findByStatus("FAILED")   → failed payment attempts
     * findByStatus("REFUNDED") → all refunded payments
     * findByStatus("SUCCESS")  → all successful payments
     *
     * Used in admin analytics dashboard.
     */
    List<Payment> findByStatus(String status);

    /*
     * Find payment by Razorpay Order ID.
     * MOCK MODE → orderId = "MOCK_ORDER_5"
     * RAZORPAY MODE → orderId = "order_NBjjkv09NoyHAT"
     *
     * Used during payment verification:
     * → Razorpay sends back orderId after payment
     * → We find our payment record using this orderId
     * → Then update with paymentId and signature
     * → Then verify and mark SUCCESS
     *
     * This method is Razorpay-ready right now.
     * No changes needed when we integrate real gateway.
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /*
     * Find payment by Razorpay Payment ID.
     * MOCK MODE → paymentId = "MOCK_PAY_1712345678"
     * RAZORPAY MODE → paymentId = "pay_29QQoUBi66xm2f"
     *
     * Used when:
     * → Initiating refund — we find payment by paymentId
     * → Admin looks up specific transaction
     * → Webhook from Razorpay sends paymentId
     *   we find our record and update status
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /*
     * Calculate total revenue from all successful payments.
     * Used in admin analytics dashboard.
     * Example: "Total revenue this month: ₹45,000"
     *
     * We use @Query because this needs SUM aggregation
     * which Spring cannot generate from method name alone.
     * Only counts SUCCESS payments — not pending or failed.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double calculateTotalRevenue();

    /*
     * Calculate total revenue from a specific patient.
     * Used in patient payment summary.
     * Example: "You have spent ₹2,500 on MediBook"
     * Only counts SUCCESS payments.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE " +
           "p.patientId = :patientId AND p.status = 'SUCCESS'")
    Double calculateRevenueByPatient(@Param("patientId") int patientId);

    /*
     * Count total number of successful payments.
     * Used in admin platform analytics.
     * Example: "1,250 successful transactions this month"
     */
    long countByStatus(String status);

    /*
     * Find payment by its own ID.
     * Used when patient requests refund by paymentId.
     * Also used by admin to look up specific payment.
     */
    Optional<Payment> findByPaymentId(int paymentId);

    /*
     * Find all payments for a specific patient with specific status.
     * Used when patient wants to see only successful payments.
     * Or admin wants to see all failed payments by patient.
     * Example: findByPatientIdAndStatus(1, "SUCCESS")
     */
    List<Payment> findByPatientIdAndStatus(int patientId, String status);
}