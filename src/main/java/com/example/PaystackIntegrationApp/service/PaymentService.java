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
import com.google.gson.JsonSyntaxException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Formatter; // Added for hex encoding
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private String paystackSecretKey; // This is your main Paystack Secret Key (sk_live_...)

    private final PaymentMethodRepository paymentMethodRepository;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final FirebaseDatabase firebaseDatabase;

    public PaymentService(PaymentMethodRepository paymentMethodRepository, FirebaseDatabase firebaseDatabase) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.firebaseDatabase = firebaseDatabase;
        logger.info("PaymentService constructor initialized with FirebaseDatabase bean.");
    }

    // This method ensures the secret key is trimmed after injection.
    // This is good practice to remove accidental whitespace.
    @Value("${paystack.secretKey}")
    public void setPaystackSecretKey(String paystackSecretKey) {
        this.paystackSecretKey = paystackSecretKey.trim();
        logger.info("Paystack Secret Key loaded and trimmed. Length: {}", this.paystackSecretKey.length());
        // Temporarily keep this debug line to confirm the key is correctly loaded and trimmed
        logger.info("DEBUG: Paystack Secret Key being used (first 5 chars after trim): {}", this.paystackSecretKey.substring(0, Math.min(this.paystackSecretKey.length(), 5)));
        // REMOVE the above DEBUG line in production code for security reasons.
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
                .header("Authorization", "Bearer " + paystackSecretKey) // Use paystackSecretKey for API calls
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
        } else {
            // Ensure metadata is always an empty JSON object if no specific metadata is provided
            bodyMap.put("metadata", new HashMap<>());
        }


        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey) // Use paystackSecretKey for API calls
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

        // --- ENFORCING METADATA AS A MAP START ---
        Map<String, String> metadataToSend = new HashMap<>();
        if (orderFirebaseKey != null && !orderFirebaseKey.isEmpty()) {
            metadataToSend.put("order_firebase_key", orderFirebaseKey);
        } else {
            logger.warn("orderFirebaseKey is null or empty for transaction {}. No 'order_firebase_key' will be sent in metadata.", transactionReference);
        }
        bodyMap.put("metadata", metadataToSend); // Always send a Map, even if empty
        // --- ENFORCING METADATA AS A MAP END ---


        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey) // Use paystackSecretKey for API calls
                .post(body)
                .build();

        logger.info("Attempting to charge saved card. Ref: {}, AuthCode: {}", transactionReference, authorizationCode);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBodyString = response.body() != null ? response.body().string() : "No response body";
            logger.info("Paystack /charge raw response: {}", responseBodyString);

            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(responseBodyString, type);

            boolean status = (boolean) responseMap.getOrDefault("status", false);
            String message = (String) responseMap.getOrDefault("message", "Unknown error");
            Map<String, Object> data = (Map<String, Object>) responseMap.getOrDefault("data", Collections.emptyMap());

            String gatewayResponse = (String) data.getOrDefault("gateway_response", "unknown");
            String receivedReference = (String) data.getOrDefault("reference", transactionReference);
            String actionRequired = (String) data.get("status");

            if (!response.isSuccessful()) {
                logger.error("Failed to charge saved card via Paystack (non-200 response): {} - {}", response.code(), responseBodyString);
                return new ChargeResponse(
                    status,
                    message,
                    gatewayResponse,
                    receivedReference,
                    actionRequired,
                    data
                );
            }

            logger.info("Charge saved card response. Status: {}, Message: {}, Gateway Response: {}, Action Required: {}", status, message, gatewayResponse, actionRequired);
            return new ChargeResponse(status, message, gatewayResponse, receivedReference, actionRequired, data);
        }
    }

    public ChargeResponse submitChallenge(String transactionReference, String challengeType, String challengeValue) throws IOException {
        String url;
        String paramKey;

        switch (challengeType.toLowerCase(Locale.ROOT)) {
            case "pin":
                url = "https://api.paystack.co/charge/submit_pin";
                paramKey = "pin";
                break;
            case "otp":
                url = "https://api.paystack.co/charge/submit_otp";
                paramKey = "otp";
                break;
            case "birthday":
                url = "https://api.paystack.co/charge/submit_birthday";
                paramKey = "birthday";
                break;
            default:
                logger.error("Unknown challenge type: {}", challengeType);
                return new ChargeResponse(false, "Unknown challenge type provided.", "invalid_challenge_type", transactionReference, null, null);
        }

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put(paramKey, challengeValue);
        bodyMap.put("reference", transactionReference);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(bodyMap));

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + paystackSecretKey) // Use paystackSecretKey for API calls
                .post(body)
                .build();

        logger.info("Attempting to submit {} for transaction: {}", challengeType, transactionReference);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBodyString = response.body() != null ? response.body().string() : "No response body";
            logger.info("Paystack {} raw response: {}", url, responseBodyString);

            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> responseMap = gson.fromJson(responseBodyString, type);

            boolean status = (boolean) responseMap.getOrDefault("status", false);
            String message = (String) responseMap.getOrDefault("message", "Unknown error during challenge submission.");
            Map<String, Object> data = (Map<String, Object>) responseMap.getOrDefault("data", Collections.emptyMap());

            String gatewayResponse = (String) data.getOrDefault("gateway_response", "unknown");
            String receivedReference = (String) data.getOrDefault("reference", transactionReference);
            String actionRequired = (String) data.get("status");

            if (!response.isSuccessful()) {
                logger.error("Failed to submit challenge via Paystack (non-200 response): {} - {}", response.code(), responseBodyString);
                return new ChargeResponse(
                    status,
                    message,
                    gatewayResponse,
                    receivedReference,
                    actionRequired,
                    data
                );
            }

            logger.info("Challenge submission response. Status: {}, Message: {}, Gateway Response: {}, Action Required: {}", status, message, gatewayResponse, actionRequired);
            return new ChargeResponse(status, message, gatewayResponse, receivedReference, actionRequired, data);
        }
    }

    /**
     * Processes the raw Paystack webhook payload, verifies its signature,
     * and updates Firebase if valid.
     * @param rawWebhookPayload The raw JSON string of the webhook body.
     * @param signature The X-Paystack-Signature header value.
     * @throws Exception if verification fails or processing encounters an error.
     */
    public void processPaystackWebhook(String rawWebhookPayload, String signature) throws Exception {
        // First, verify the signature using the raw payload string
        if (!verifyWebhookSignature(rawWebhookPayload, signature)) {
            throw new Exception("Webhook signature verification failed.");
        }

        // If verification passes, then parse the payload into the object
        PaystackWebhookRequest webhookPayload;
        try {
            webhookPayload = gson.fromJson(rawWebhookPayload, PaystackWebhookRequest.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse raw webhook payload JSON: {}", rawWebhookPayload, e);
            throw new Exception("Invalid JSON payload received.", e);
        }

        // Now, handle the processed webhook
        handlePaystackWebhook(webhookPayload);
    }

    /**
     * Verifies the Paystack webhook signature using the raw payload string.
     * @param rawPayload The raw webhook payload (JSON string).
     * @param signature The X-Paystack-Signature header value.
     * @return True if the signature is valid, false otherwise.
     */
    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        try {
            // This debug log shows the key being used. You should remove this in production.
            logger.info("DEBUG: Paystack Secret Key for webhook verification. Length: {}, Trimmed: {}. First 5 chars: {}",
                        paystackSecretKey.length(),
                        paystackSecretKey.equals(paystackSecretKey.trim()),
                        paystackSecretKey.substring(0, Math.min(paystackSecretKey.length(), 5)));
            // REMOVE the above DEBUG line once this is confirmed working.

            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            // Use the already trimmed paystackSecretKey (which is your main secret key)
            SecretKeySpec secretKeySpec = new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secretKeySpec);
            byte[] hash = sha512_HMAC.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));

            // *** CRITICAL FIX: Convert the byte array to a HEXADECIMAL string ***
            StringBuilder hexString = new StringBuilder();
            try (Formatter formatter = new Formatter(hexString)) {
                for (byte b : hash) {
                    formatter.format("%02x", b); // %02x ensures two hex characters, with leading zero if needed
                }
            }
            String calculatedSignature = hexString.toString(); // This is the hex-encoded signature

            logger.info("Received webhook signature (from header): {}", signature);
            logger.info("Calculated webhook signature (from raw payload): {}", calculatedSignature);

            // Paystack sends the hex signature as lowercase, but comparing case-insensitively is robust.
            if (!calculatedSignature.equalsIgnoreCase(signature)) {
                logger.error("WEBHOOK SIGNATURE MISMATCH! Calculated vs. Received. This is critical.");
                logger.error("Raw Payload: {}", rawPayload); // Log the raw payload for inspection
                return false;
            }
            logger.info("Webhook signature verified successfully.");
            return true;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error during webhook signature verification: {}", e.getMessage(), e);
            return false;
        }
    }

    public void handlePaystackWebhook(PaystackWebhookRequest webhookPayload) {
        String event = webhookPayload.event;
        PaystackWebhookRequest.Data data = webhookPayload.data; // Use the Data object directly

        String transactionReference = data.reference;
        String statusFromWebhook = data.status; // e.g., "success"

        String customerEmail = (data.customer != null) ? data.customer.email : "unknown@example.com";

        String orderFirebaseKey = null;

        // --- IMPROVED METADATA HANDLING START ---
        Object metadataObj = data.metadata; // Get as Object first

        if (metadataObj instanceof Map) { // Check if it's actually a Map
            Map<String, Object> metadata = (Map<String, Object>) metadataObj; // Safely cast
            if (metadata != null) {
                orderFirebaseKey = (String) metadata.get("order_firebase_key");
            }
        } else if (metadataObj != null) {
            // Log if metadata is not a Map, but not null (e.g., it's a Double, String, etc.)
            logger.warn("Webhook 'metadata' field is not a Map. Actual type: {}. Value: {}. Cannot extract order_firebase_key.",
                        metadataObj.getClass().getName(),
                        metadataObj);
        }
        // --- IMPROVED METADATA HANDLING END ---

        logger.info("Processing Paystack webhook (parsed): Event={}, Reference={}, Status={}, OrderFirebaseKey={}, CustomerEmail={}", event, transactionReference, statusFromWebhook, orderFirebaseKey, customerEmail);

        if (orderFirebaseKey == null) {
            logger.warn("Webhook received without valid 'order_firebase_key'. Cannot update Firebase order. Reference: {}", transactionReference);
            return;
        }

        final String finalOrderFirebaseKey = orderFirebaseKey;

        // Using a Firebase listener for a one-time read
        findUserIdByOrderKey(finalOrderFirebaseKey, (userIdFound) -> {
            if (userIdFound != null) {
                DatabaseReference orderStatusRef = firebaseDatabase.getReference("Users")
                        .child(userIdFound)
                        .child("orders")
                        .child(finalOrderFirebaseKey)
                        .child("status");

                String newOrderStatus;
                // Determine new status based on webhook event and data status
                if ("charge.success".equalsIgnoreCase(event) || "success".equalsIgnoreCase(statusFromWebhook)) {
                    newOrderStatus = "Confirmed";
                } else if ("charge.failed".equalsIgnoreCase(event) || "transfer.failed".equalsIgnoreCase(event) || "failed".equalsIgnoreCase(statusFromWebhook) || "reversed".equalsIgnoreCase(statusFromWebhook)) {
                    newOrderStatus = "Payment Failed";
                } else {
                    newOrderStatus = "Payment Pending"; // Default or other statuses
                }

                logger.info("Attempting to update Firebase order {} status to: {}", finalOrderFirebaseKey, newOrderStatus);
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
