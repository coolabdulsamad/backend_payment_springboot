/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.model;

/**
 * Model for the response from the backend after initializing a Paystack transaction.
 */
public class PaymentInitializationResponse {
    private boolean status;
    private String message;
    private String accessCode;
    private String authorizationUrl;

    // Constructors
    public PaymentInitializationResponse() {
    }

    public PaymentInitializationResponse(boolean status, String message, String accessCode, String authorizationUrl) {
        this.status = status;
        this.message = message;
        this.accessCode = accessCode;
        this.authorizationUrl = authorizationUrl;
    }

    // Getters and Setters
    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public String toString() {
        return "PaymentInitializationResponse{" +
               "status=" + status +
               ", message='" + message + '\'' +
               ", accessCode='" + accessCode + '\'' +
               ", authorizationUrl='" + authorizationUrl + '\'' +
               '}';
    }
}
