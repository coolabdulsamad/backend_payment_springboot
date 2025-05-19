/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.service;

/**
 *
 * @author coola
 */
import com.example.PaystackIntegrationApp.model.PaymentMethod;
import com.example.PaystackIntegrationApp.repository.PaymentMethodRepository;
import com.google.gson.Gson;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
        // 1. Verify the transaction using the reference
        Map<String, Object> verificationResponse = verifyTransaction(reference);

        if (verificationResponse != null && (boolean) verificationResponse.get("status")) {
            Map<String, Object> data = (Map<String, Object>) verificationResponse.get("data");
            if (data.containsKey("authorization")) {
                Map<String, Object> authorization = (Map<String, Object>) data.get("authorization");
                String token = (String) authorization.get("authorization_code");
                String last4 = (String) authorization.get("last4");
                String cardType = (String) authorization.get("card_type");

                // 2. Store the token and masked card details in your database
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
            return gson.fromJson(response.body().string(), Map.class);
        }
    }
    
        public List<PaymentMethod> getPaymentMethodsByUserId(String userId) {
        return paymentMethodRepository.findByUserId(userId);
    }
        
            public void deletePaymentMethod(Long id) {
        paymentMethodRepository.deleteById(id);
    }
}
