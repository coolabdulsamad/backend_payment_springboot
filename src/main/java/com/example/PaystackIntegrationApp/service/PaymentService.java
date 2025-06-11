/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.service;

import com.example.PaystackIntegrationApp.model.ChargeResponse;
import com.example.PaystackIntegrationApp.model.PaymentMethod;
import com.example.PaystackIntegrationApp.model.PaymentInitializationResponse;
import com.example.PaystackIntegrationApp.repository.PaymentMethodRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${paystack.secretKey}")
    private String paystackSecretKey;

    private final PaymentMethodRepository paymentMethodRepository;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public PaymentService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    public void processPaymentReference(String reference, String userId) throws IOException {
        Map<String, Object> verificationResponse = verifyTransaction(reference);

        if (verificationResponse != null && (boolean) verificationResponse.get("status")) {
            Map<String, Object> data = (Map<String, Object>) verificationResponse.get("data");
            if (data.containsKey("authorization")) {
                Map<String, Object> authorization = (Map<String, Object>) data.get("authorization");
                String token = (String) authorization.get("authorization_code");
                String last4 = (String) authorization.get("last4");
                String cardType = (String) authorization.get("card_type");

                PaymentMethod paymentMethod = new PaymentMethod();
                paymentMethod.setUserId(userId);
                paymentMethod.setToken(token);
                paymentMethod.setMaskedCardNumber("****-****-****-" + last4);
                paymentMethod.setCardType(cardType);
                paymentMethodRepository.save(paymentMethod);

            } else {
                throw new IOException("Authorization details not found in Paystack response");
            }
        } else {
            throw new IOException("Failed to verify Paystack transaction: " + verificationResponse);
        }
    }

    private Map<String, Object> verifyTransaction(String reference) throws IOException {
        String url = "https://api.paystack.co/transaction/verify/" + reference;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to call Paystack API: " + response);
            }
            // Use TypeToken for correct deserialization of generic Map
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(response.body().string(), type);
        }
    }

    public List<PaymentMethod> getPaymentMethodsByUserId(String userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    public void deletePaymentMethod(Long id) {
        paymentMethodRepository.deleteById(id);
    }

    /**
     * Initializes a Paystack transaction for the client-side PaymentSheet.
     * This calls your Paystack backend to get an access code.
     * @param amount The amount in kobo.
     * @param email The customer's email.
     * @param reference Your unique transaction reference.
     * @param callbackUrl The URL Paystack should redirect to after payment (for webhooks/confirmation).
     * @param metadata Optional metadata to attach to the transaction.
     * @return PaymentInitializationResponse containing access_code and authorization_url.
     * @throws IOException If there's a network error or Paystack API call fails.
     */
    public PaymentInitializationResponse initializeTransaction(long amount, String email, String reference, String callbackUrl, Map<String, Object> metadata) throws IOException {
        String url = "https://api.paystack.co/transaction/initialize";

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("email", email);
        bodyMap.put("amount", amount);
        bodyMap.put("reference", reference);
        bodyMap.put("callback_url", callbackUrl);
        if (metadata != null && !metadata.isEmpty()) {
            bodyMap.put("metadata", metadata);
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("Failed to initialize Paystack transaction: " + response.code() + " - " + errorBody);
            }
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(response.body().string(), type);

            boolean status = (boolean) responseMap.get("status");
            String message = (String) responseMap.get("message");
            Map<String, String> data = (Map<String, String>) responseMap.get("data");

            String accessCode = null;
            String authorizationUrl = null;
            if (data != null) {
                accessCode = data.get("access_code");
                authorizationUrl = data.get("authorization_url");
            }
            return new PaymentInitializationResponse(status, message, accessCode, authorizationUrl);
        }
    }

    /**
     * Charges a previously saved card (authorization) using Paystack.
     * This makes a POST request to the /charge endpoint.
     * @param amount The amount to charge in kobo.
     * @param email The customer's email.
     * @param authorizationCode The authorization code from the saved PaymentMethod.
     * @param transactionReference Your unique transaction reference for this charge.
     * @param orderFirebaseKey The Firebase key of the order (for metadata).
     * @return ChargeResponse containing the status and details of the charge.
     * @throws IOException If there's a network error or Paystack API call fails.
     */
    public ChargeResponse chargeSavedCard(long amount, String email, String authorizationCode, String transactionReference, String orderFirebaseKey) throws IOException {
        String url = "https://api.paystack.co/charge";

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("email", email);
        bodyMap.put("amount", amount);
        bodyMap.put("reference", transactionReference);
        bodyMap.put("authorization_code", authorizationCode);

        // Add metadata to link to the Firebase order
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_firebase_key", orderFirebaseKey);
        bodyMap.put("metadata", metadata);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBodyString = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Failed to charge saved card: " + response.code() + " - " + responseBodyString);
            }

            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(responseBodyString, type);

            boolean status = (boolean) responseMap.get("status");
            String message = (String) responseMap.get("message");
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");

            String gatewayResponse = "unknown";
            String receivedReference = transactionReference; // Default to sent reference

            if (data != null) {
                if (data.containsKey("gateway_response")) {
                    gatewayResponse = (String) data.get("gateway_response");
                }
                if (data.containsKey("reference")) {
                    receivedReference = (String) data.get("reference");
                }
            }

            return new ChargeResponse(status, message, gatewayResponse, receivedReference);
        }
    }
}
