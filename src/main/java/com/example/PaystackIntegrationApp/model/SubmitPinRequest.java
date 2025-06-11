 package com.example.PaystackIntegrationApp.model;

 public class SubmitPinRequest {
    private String reference;
    private String pin;

    // Getters and Setters
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
 }
