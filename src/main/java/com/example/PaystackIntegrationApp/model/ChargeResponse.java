/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.model;

/**
 * Model for the response body from the backend after a saved card charge attempt.
 */
public class ChargeResponse {
    private boolean status;
    private String message;
    private String gatewayResponse; // e.g., "success", "failed", "reversed"
    private String transactionReference;

    // Constructors
    public ChargeResponse() {
    }

    public ChargeResponse(boolean status, String message, String gatewayResponse, String transactionReference) {
        this.status = status;
        this.message = message;
        this.gatewayResponse = gatewayResponse;
        this.transactionReference = transactionReference;
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

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    @Override
    public String toString() {
        return "ChargeResponse{" +
               "status=" + status +
               ", message='" + message + '\'' +
               ", gatewayResponse='" + gatewayResponse + '\'' +
               ", transactionReference='" + transactionReference + '\'' +
               '}';
    }
}
