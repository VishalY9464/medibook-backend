package com.medibook.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * This is the DTO for initiating a payment.
 *
 * Think of it like the payment form patient fills
 * after booking an appointment.
 * Patient says: "I want to pay for appointment #5,
 * here is my payment method."
 *
 * Why a separate DTO?
 * We never expose Payment entity to outside world.
 * DTO carries only what patient needs to send.
 * Internal fields like status, transactionId, timestamps
 * are set by server — patient should not control those.
 *
 * RAZORPAY READY:
 * When we integrate Razorpay later,
 * this same DTO is used — no changes needed.
 * PaymentServiceImpl.callGateway() receives this
 * and passes it to Razorpay API.
 */
@Data
public class PaymentRequest {

    /*
     * Which appointment is this payment for?
     * Links payment to Appointment entity (UC4).
     * One appointment can have only one payment.
     * We verify this appointment exists before processing.
     */
    @NotNull(message = "Appointment ID is required")
    private int appointmentId;

    /*
     * Who is making this payment?
     * patientId is userId from User entity (UC1).
     * We verify patient owns this appointment.
     * Security check — patient A cannot pay for patient B.
     */
    @NotNull(message = "Patient ID is required")
    private int patientId;

    /*
     * How much to pay in rupees?
     * Minimum 1 rupee — cannot be zero or negative.
     * In real Razorpay → amount is in paise (multiply by 100).
     * We handle that conversion inside PaymentServiceImpl.
     * Patient sends amount in rupees — cleaner API.
     */
    @Min(value = 1, message = "Amount must be at least 1 rupee")
    private double amount;

    /*
     * What method is patient using to pay?
     * Values: CARD / UPI / NETBANKING / WALLET
     * Currently mock — all methods return SUCCESS.
     * When Razorpay integrated → this is sent to Razorpay
     * to pre-select payment method on their popup.
     */
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    /*
     * What currency is being used?
     * Default is INR for Indian platform.
     * Kept as field so future international expansion is easy.
     * Razorpay supports INR, USD, EUR etc.
     */
    private String currency = "INR";
}