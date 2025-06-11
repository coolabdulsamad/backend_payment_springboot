package com.example.PaystackIntegrationApp.model;

public class ChargeResponse {
    private boolean status;
    private String message;
    private String gatewayResponse;
    private String transactionReference;
    private String actionRequired; // New field to indicate if further action (like PIN) is needed

    public ChargeResponse() {
    }

    public ChargeResponse(boolean status, String message, String gatewayResponse, String transactionReference) {
        this.status = status;
        this.message = message;
        this.gatewayResponse = gatewayResponse;
        this.transactionReference = transactionReference;
        this.actionRequired = null; // Default to null
    }

    public ChargeResponse(boolean status, String message, String gatewayResponse, String transactionReference, String actionRequired) {
        this.status = status;
        this.message = message;
        this.gatewayResponse = gatewayResponse;
        this.transactionReference = transactionReference;
        this.actionRequired = actionRequired;
    }

    // Getters
    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getActionRequired() { // New getter
        return actionRequired;
    }

    // Setters
    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public void setActionRequired(String actionRequired) { // New setter
        this.actionRequired = actionRequired;
    }
}
