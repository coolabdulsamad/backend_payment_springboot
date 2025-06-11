/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.model;

import java.util.Map;

/**
 * Model for the request body when initializing a Paystack transaction from the Android app.
 */
public class PaymentInitializationRequest {
    private long amount; // Amount in kobo
    private String email;
    private String reference; // Your unique transaction reference
    private String callbackUrl; // The URL Paystack should redirect to after payment (for webhooks/confirmation)
    private Map<String, Object> metadata; // Optional metadata

    // Constructors
    public PaymentInitializationRequest() {
    }

    public PaymentInitializationRequest(long amount, String email, String reference, String callbackUrl, Map<String, Object> metadata) {
        this.amount = amount;
        this.email = email;
        this.reference = reference;
        this.callbackUrl = callbackUrl;
        this.metadata = metadata;
    }

    // Getters and Setters
    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "PaymentInitializationRequest{" +
               "amount=" + amount +
               ", email='" + email + '\'' +
               ", reference='" + reference + '\'' +
               ", callbackUrl='" + callbackUrl + '\'' +
               ", metadata=" + metadata +
               '}';
    }
}
