package com.example.PaystackIntegrationApp.controller;

import com.example.PaystackIntegrationApp.model.ChargeSavedCardRequest;
import com.example.PaystackIntegrationApp.model.ChargeResponse;
import com.example.PaystackIntegrationApp.model.PaymentMethod;
import com.example.PaystackIntegrationApp.model.PaymentInitializationRequest;
import com.example.PaystackIntegrationApp.model.PaymentInitializationResponse;
import com.example.PaystackIntegrationApp.model.PaystackWebhookRequest;
import com.example.PaystackIntegrationApp.service.PaymentService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/add-card")
    public ResponseEntity<Map<String, String>> addCard(@RequestBody Map<String, String> payload) {
        String reference = payload.get("reference");
        String userId = payload.get("userId");

        if (reference == null || reference.isEmpty() || userId == null || userId.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request: Missing reference or userId");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        try {
            paymentService.processPaymentReference(reference, userId);
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "Payment reference processed successfully");
            return new ResponseEntity<>(successResponse, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing payment reference: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/cards/{userId}")
    public ResponseEntity<List<PaymentMethod>> getCards(@PathVariable String userId) {
        try {
            List<PaymentMethod> paymentMethods = paymentService.getPaymentMethodsByUserId(userId);
            if (paymentMethods.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(paymentMethods, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Map<String, String>> deleteCard(@PathVariable Long id) {
        try {
            paymentService.deletePaymentMethod(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Card deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error deleting card: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/charge-saved-card")
    public ResponseEntity<ChargeResponse> chargeSavedCard(@RequestBody ChargeSavedCardRequest request) {
        try {
            ChargeResponse response = paymentService.chargeSavedCard(
                request.getAmount(),
                request.getEmail(),
                request.getAuthorizationCode(),
                request.getTransactionReference(),
                request.getOrderFirebaseKey()
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            // Create a ChargeResponse even on network error, with data as null
            return new ResponseEntity<>(new ChargeResponse(
                false,
                "Failed to communicate with Paystack: " + e.getMessage(),
                "network_error",
                request.getTransactionReference(),
                null,
                null
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            e.printStackTrace();
            // Create a ChargeResponse even on internal error, with data as null
            return new ResponseEntity<>(new ChargeResponse(
                false,
                "An unexpected error occurred: " + e.getMessage(),
                "internal_error",
                request.getTransactionReference(),
                null,
                null
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/initialize-paystack-transaction")
    public ResponseEntity<PaymentInitializationResponse> initializePaystackTransaction(@RequestBody PaymentInitializationRequest request) {
        try {
            PaymentInitializationResponse response = paymentService.initializeTransaction(
                request.getAmount(),
                request.getEmail(),
                request.getReference(),
                request.getCallbackUrl(),
                request.getMetadata()
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(new PaymentInitializationResponse(
                false,
                "Failed to communicate with Paystack: " + e.getMessage(),
                null,
                null
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new PaymentInitializationResponse(
                false,
                "An unexpected error occurred during initialization: " + e.getMessage(),
                null,
                null
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/paystack-webhook")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestHeader("x-paystack-signature") String signature,
            @RequestBody PaystackWebhookRequest webhookPayload) {
        try {
            if (!paymentService.verifyWebhookSignature(webhookPayload, signature)) {
                return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
            }

            paymentService.handlePaystackWebhook(webhookPayload);
            return new ResponseEntity<>("Webhook received and processed", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error processing webhook: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * New endpoint to submit PIN/OTP/Birthday for Paystack transaction challenges.
     * The payload now explicitly includes the 'challengeType'.
     * @param payload A map containing "reference" (transaction reference), "challengeValue" (the PIN/OTP/Birthday),
     * and "challengeType" (e.g., "pin", "otp", "birthday").
     * @return ChargeResponse indicating the status of the submission.
     */
    @PostMapping("/submit-challenge")
    public ResponseEntity<ChargeResponse> submitChallenge(@RequestBody Map<String, String> payload) {
        String reference = payload.get("reference");
        String challengeValue = payload.get("challengeValue"); // Renamed for clarity
        String challengeType = payload.get("challengeType"); // New: type of challenge

        if (reference == null || reference.isEmpty() || challengeValue == null || challengeValue.isEmpty() || challengeType == null || challengeType.isEmpty()) {
            return new ResponseEntity<>(new ChargeResponse(
                false,
                "Invalid request: Missing reference, challenge value, or challenge type.",
                "invalid_input",
                reference,
                null,
                null
            ), HttpStatus.BAD_REQUEST);
        }

        try {
            // Pass the challengeType to the service method
            ChargeResponse response = paymentService.submitChallenge(reference, challengeType, challengeValue);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ChargeResponse(
                false,
                "Failed to communicate with Paystack for challenge submission: " + e.getMessage(),
                "network_error",
                reference,
                null,
                null
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ChargeResponse(
                false,
                "An unexpected error occurred during challenge submission: " + e.getMessage(),
                "internal_error",
                reference,
                null,
                null
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
