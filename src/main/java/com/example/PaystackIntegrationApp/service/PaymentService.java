package com.example.PaystackIntegrationApp.service;

import com.example.PaystackIntegrationApp.model.ChargeResponse;
import com.example.PaystackIntegrationApp.model.PaymentMethod;
import com.example.PaystackIntegrationApp.model.PaymentInitializationResponse;
import com.example.PaystackIntegrationApp.model.PaystackWebhookRequest;
import com.example.PaystackIntegrationApp.repository.PaymentMethodRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.micrometer.common.lang.NonNull;
import io.micrometer.common.lang.Nullable;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer; // Import Consumer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${paystack.secretKey}")
    private String paystackSecretKey;

    private final PaymentMethodRepository paymentMethodRepository;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final FirebaseDatabase firebaseDatabase;

    public PaymentService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public void processPaymentReference(String reference, String userId) throws IOException {
        Map<String, Object> verificationResponse = verifyTransaction(reference);

        logger.info("Paystack verify transaction response body: {}", gson.toJson(verificationResponse));

        if (verificationResponse != null && (boolean) verificationResponse.get("status")) {
            Map<String, Object> data = (Map<String, Object>) verificationResponse.get("data");
            if (data.containsKey("authorization")) {
                Map<String, Object> authorization = (Map<String, Object>) data.get("authorization");
                String token = (String) authorization.get("authorization_code");
                String last4 = (String) authorization.get("last4");
                String cardType = (String) authorization.get("card_type");

                if (token == null || token.isEmpty()) {
                    throw new IOException("Authorization code (token) is null or empty in Paystack response.");
                }

                PaymentMethod paymentMethod = new PaymentMethod();
                paymentMethod.setUserId(userId);
                paymentMethod.setToken(token);
                paymentMethod.setMaskedCardNumber("****-****-****-" + last4);
                paymentMethod.setCardType(cardType);
                paymentMethodRepository.save(paymentMethod);
                logger.info("Successfully saved payment method for user {}. Token: {}", userId, token);

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
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.error("Failed to call Paystack API for verification: {} - {}", response.code(), errorBody);
                throw new IOException("Failed to call Paystack API for verification: " + response.code() + " - " + errorBody);
            }
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            String responseBody = response.body().string();
            logger.info("Paystack /transaction/verify raw response: {}", responseBody);
            return gson.fromJson(responseBody, type);
        }
    }

    public List<PaymentMethod> getPaymentMethodsByUserId(String userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    public void deletePaymentMethod(Long id) {
        paymentMethodRepository.deleteById(id);
    }

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
            String responseBodyString = response.body() != null ? response.body().string() : "No response body";
            if (!response.isSuccessful()) {
                logger.error("Failed to initialize Paystack transaction: {} - {}", response.code(), responseBodyString);
                throw new IOException("Failed to initialize Paystack transaction: " + response.code() + " - " + responseBodyString);
            }
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(responseBodyString, type);

            boolean status = (boolean) responseMap.get("status");
            String message = (String) responseMap.get("message");
            Map<String, String> data = (Map<String, String>) responseMap.get("data");

            String accessCode = null;
            String authorizationUrl = null;
            if (data != null) {
                accessCode = data.get("access_code");
                authorizationUrl = data.get("authorization_url");
            }
            logger.info("Paystack transaction initialized. Status: {}, Message: {}", status, message);
            return new PaymentInitializationResponse(status, message, accessCode, authorizationUrl);
        }
    }

    public ChargeResponse chargeSavedCard(long amount, String email, String authorizationCode, String transactionReference, String orderFirebaseKey) throws IOException {
        String url = "https://api.paystack.co/charge";

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("email", email);
        bodyMap.put("amount", amount);
        bodyMap.put("reference", transactionReference);
        bodyMap.put("authorization_code", authorizationCode);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_firebase_key", orderFirebaseKey);
        bodyMap.put("metadata", metadata);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey)
                .post(body)
                .build();

        logger.info("Attempting to charge saved card. Ref: {}, AuthCode: {}", transactionReference, authorizationCode);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBodyString = response.body() != null ? response.body().string() : "No response body";
            logger.info("Paystack /charge raw response: {}", responseBodyString);

            if (!response.isSuccessful()) {
                logger.error("Failed to charge saved card via Paystack: {} - {}", response.code(), responseBodyString);
                Map<String, Object> errorResponseMap = gson.fromJson(responseBodyString, new TypeToken<Map<String, Object>>() {}.getType());
                String paystackMessage = (String) errorResponseMap.getOrDefault("message", "Unknown Paystack error");
                Map<String, Object> dataMap = (Map<String, Object>) errorResponseMap.getOrDefault("data", Collections.emptyMap());
                String paystackGatewayResponse = (String) dataMap.getOrDefault("gateway_response", "failed");

                return new ChargeResponse(
                    false,
                    "Failed to charge saved card: " + response.code() + " - " + paystackMessage,
                    paystackGatewayResponse,
                    transactionReference
                );
            }

            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(responseBodyString, type);

            boolean status = (boolean) responseMap.get("status");
            String message = (String) responseMap.get("message");
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");

            String gatewayResponse = "unknown";
            String receivedReference = transactionReference;

            if (data != null) {
                if (data.containsKey("gateway_response")) {
                    gatewayResponse = (String) data.get("gateway_response");
                }
                if (data.containsKey("reference")) {
                    receivedReference = (String) data.get("reference");
                }
            }
            logger.info("Charge saved card response. Status: {}, Message: {}, Gateway Response: {}", status, message, gatewayResponse);
            return new ChargeResponse(status, message, gatewayResponse, receivedReference);
        }
    }

    public boolean verifyWebhookSignature(PaystackWebhookRequest webhookPayload, String signature) {
        try {
            String jsonPayload = gson.toJson(webhookPayload);
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secretKeySpec);
            byte[] hash = sha512_HMAC.doFinal(jsonPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            logger.info("Received webhook signature: {}", signature);
            logger.info("Calculated webhook signature: {}", expectedSignature);

            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error during webhook signature verification: {}", e.getMessage(), e);
            return false;
        }
    }

    public void handlePaystackWebhook(PaystackWebhookRequest webhookPayload) {
        String event = webhookPayload.event;
        Map<String, Object> data = webhookPayload.data;
        String transactionReference = (String) data.get("reference");
        String statusFromWebhook = (String) data.get("status");

        Map<String, Object> customerData = (Map<String, Object>) data.get("customer");
        String customerEmail = (customerData != null) ? (String) customerData.get("email") : "unknown@example.com";

        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
        String orderFirebaseKey = null;
        if (metadata != null) {
            orderFirebaseKey = (String) metadata.get("order_firebase_key");
        }

        logger.info("Processing Paystack webhook: Event={}, Reference={}, Status={}, OrderFirebaseKey={}, CustomerEmail={}", event, transactionReference, statusFromWebhook, orderFirebaseKey, customerEmail);

        if (orderFirebaseKey == null) {
            logger.warn("Webhook received without 'order_firebase_key' in metadata. Cannot update Firebase order. Reference: {}", transactionReference);
            return;
        }

        // Make orderFirebaseKey effectively final for use in lambda
        final String finalOrderFirebaseKey = orderFirebaseKey;

        findUserIdByOrderKey(finalOrderFirebaseKey, (userIdFound) -> {
            if (userIdFound != null) {
                DatabaseReference orderStatusRef = firebaseDatabase.getReference("Users")
                        .child(userIdFound)
                        .child("orders")
                        .child(finalOrderFirebaseKey) // Use finalOrderFirebaseKey here
                        .child("status");

                String newOrderStatus;
                if ("charge.success".equalsIgnoreCase(event) || "transfer.success".equalsIgnoreCase(event) || "success".equalsIgnoreCase(statusFromWebhook)) {
                    newOrderStatus = "Confirmed";
                } else if ("charge.failed".equalsIgnoreCase(event) || "transfer.failed".equalsIgnoreCase(event) || "failed".equalsIgnoreCase(statusFromWebhook) || "reversed".equalsIgnoreCase(statusFromWebhook)) {
                    newOrderStatus = "Payment Failed";
                } else {
                    newOrderStatus = "Payment Pending";
                }

                orderStatusRef.setValue(newOrderStatus, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error != null) {
                            logger.error("Failed to update Firebase order {} status to {}: {}", finalOrderFirebaseKey, newOrderStatus, error.getMessage(), error);
                        } else {
                            logger.info("Firebase order {} status updated to: {}", finalOrderFirebaseKey, newOrderStatus);
                        }
                    }
                });
            } else {
                logger.warn("Could not find Firebase User ID for orderFirebaseKey: {}. Cannot update Firebase order.", finalOrderFirebaseKey);
            }
        });
    }

    /**
     * Helper method to find the Firebase User ID given an orderFirebaseKey.
     * This iterates through users to find the order. In a large app,
     * it's better to store userId directly in Paystack metadata during payment initialization
     * to avoid this lookup.
     * @param orderFirebaseKey The Firebase key of the order.
     * @param callback A callback to receive the found userId or null.
     */
    private void findUserIdByOrderKey(String orderFirebaseKey, Consumer<String> callback) {
        DatabaseReference usersRef = firebaseDatabase.getReference("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                String foundUserId = null;
                for (DataSnapshot userNode : usersSnapshot.getChildren()) {
                    if (userNode.child("orders").hasChild(orderFirebaseKey)) {
                        foundUserId = userNode.getKey();
                        break;
                    }
                }
                callback.accept(foundUserId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.error("Firebase lookup for userId by order key cancelled: {}", error.getMessage(), error);
                callback.accept(null);
            }
        });
    }
}