package com.example.PaystackIntegrationApp.model;

import java.util.Map;

public class ChargeResponse {
    private boolean status;
    private String message;
    private String gatewayResponse;
    private String transactionReference;
    private String actionRequired; // e.g., "send_pin", "send_otp", "send_birthday"
    private Map<String, Object> data; // New field to hold the raw 'data' object from Paystack

    public ChargeResponse() {
    }

    // Existing constructor (update if needed to set data to null)
    public ChargeResponse(boolean status, String message, String gatewayResponse, String transactionReference) {
        this.status = status;
        this.message = message;
        this.gatewayResponse = gatewayResponse;
        this.transactionReference = transactionReference;
        this.actionRequired = null;
        this.data = null;
    }

    // Updated constructor including actionRequired and data map
    public ChargeResponse(boolean status, String message, String gatewayResponse, String transactionReference, String actionRequired, Map<String, Object> data) {
        this.status = status;
        this.message = message;
        this.gatewayResponse = gatewayResponse;
        this.transactionReference = transactionReference;
        this.actionRequired = actionRequired;
        this.data = data;
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

    public String getActionRequired() {
        return actionRequired;
    }

    public Map<String, Object> getData() { // New getter for the data map
        return data;
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

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    public void setData(Map<String, Object> data) { // New setter for the data map
        this.data = data;
    }
}
