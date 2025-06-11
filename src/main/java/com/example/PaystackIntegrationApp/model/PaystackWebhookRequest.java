/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.model;

import java.util.Map;

/**
 * Represents the structure of a Paystack webhook payload.
 * Note: The 'data' field is a generic Map<String, Object> because its content
 * varies greatly based on the 'event' type.
 */
public class PaystackWebhookRequest {
    public String event; // e.g., "charge.success", "transfer.success"
    public Map<String, Object> data; // Contains transaction details, authorization, metadata etc.

    // Getters and Setters
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PaystackWebhookRequest{" +
               "event='" + event + '\'' +
               ", data=" + data +
               '}';
    }
}
