package com.medibook.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/*
 * This is the DTO for payment response.
 *
 * Think of it like the receipt you get after paying.
 * Server sends this back to patient after payment is processed.
 * Contains everything patient needs to know about their payment.
 *
 * RAZORPAY READY:
 * When we integrate Razorpay later:
 * → razorpayOrderId field carries real Razorpay order ID
 * → razorpayPaymentId carries real payment ID
 * → frontend uses these to open Razorpay popup
 * → no changes needed to this class at all
 *
 * Right now in mock mode:
 * → razorpayOrderId = "MOCK_ORDER_" + appointmentId
 * → razorpayPaymentId = "MOCK_PAY_" + timestamp
 * → status = SUCCESS immediately
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {

    /*
     * Unique ID of this payment record in our database.
     * Patient uses this for refund requests.
     * Admin uses this to look up payment details.
     */
    private int paymentId;

    /*
     * Which appointment this payment is for.
     * Patient can verify they paid for correct appointment.
     */
    private int appointmentId;

    /*
     * Current status of this payment.
     * PENDING  → payment initiated, not confirmed
     * SUCCESS  → payment confirmed
     * FAILED   → payment failed
     * REFUNDED → money returned after cancellation
     *
     * Frontend shows different UI based on this status.
     * SUCCESS → show green confirmation
     * FAILED  → show retry button
     */
    private String status;

    /*
     * How much was paid in rupees.
     * Shown on patient receipt and dashboard.
     */
    private double amount;

    /*
     * What currency was used.
     * Example: INR
     */
    private String currency;

    /*
     * What payment method was used.
     * Example: CARD, UPI, NETBANKING, WALLET
     * Shown on patient payment history.
     */
    private String paymentMethod;

    /*
     * Razorpay Order ID.
     * RIGHT NOW → "MOCK_ORDER_" + appointmentId
     * WHEN RAZORPAY INTEGRATED:
     * → real order ID from Razorpay like "order_ABC123XYZ"
     * → frontend sends this to Razorpay JS SDK
     * → Razorpay popup opens with this order
     *
     * This field is why we are Razorpay-ready.
     * Structure is already correct — just swap mock value.
     */
    private String razorpayOrderId;

    /*
     * Razorpay Payment ID.
     * RIGHT NOW → "MOCK_PAY_" + timestamp
     * WHEN RAZORPAY INTEGRATED:
     * → real payment ID from Razorpay like "pay_ABC123XYZ"
     * → used to verify payment and initiate refund
     *
     * When patient cancels appointment:
     * → we use this paymentId to call Razorpay refund API
     * → mock mode → just update status to REFUNDED
     */
    private String razorpayPaymentId;

    /*
     * Human readable message about payment result.
     * Example: "Payment successful. Appointment confirmed."
     * Example: "Payment failed. Please try again."
     * Example: "Refund initiated. Amount will be credited in 5-7 days."
     * Shown to patient on payment result page.
     */
    private String message;

    /*
     * When was this payment processed?
     * Shown on patient receipt.
     * Format: "2026-04-10 10:30:00"
     */
    private String transactionTime;
}