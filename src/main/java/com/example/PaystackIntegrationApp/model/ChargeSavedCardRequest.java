/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.model;

/**
 * Model for the request body when charging a saved card from the Android app.
 */
public class ChargeSavedCardRequest {
    private long amount; // Amount in kobo
    private String email;
    private String authorizationCode; // This is the 'token' from your PaymentMethod entity
    private String transactionReference;
    private String orderFirebaseKey; // To link the charge to a specific order in Firebase

    // Constructors
    public ChargeSavedCardRequest() {
    }

    public ChargeSavedCardRequest(long amount, String email, String authorizationCode, String transactionReference, String orderFirebaseKey) {
        this.amount = amount;
        this.email = email;
        this.authorizationCode = authorizationCode;
        this.transactionReference = transactionReference;
        this.orderFirebaseKey = orderFirebaseKey;
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

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getOrderFirebaseKey() {
        return orderFirebaseKey;
    }

    public void setOrderFirebaseKey(String orderFirebaseKey) {
        this.orderFirebaseKey = orderFirebaseKey;
    }

    @Override
    public String toString() {
        return "ChargeSavedCardRequest{" +
               "amount=" + amount +
               ", email='" + email + '\'' +
               ", authorizationCode='" + authorizationCode + '\'' +
               ", transactionReference='" + transactionReference + '\'' +
               ", orderFirebaseKey='" + orderFirebaseKey + '\'' +
               '}';
    }
}
