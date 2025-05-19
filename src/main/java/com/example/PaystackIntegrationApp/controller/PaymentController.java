/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.PaystackIntegrationApp.controller;

/**
 *
 * @author coola
 */
import com.example.PaystackIntegrationApp.model.PaymentMethod;
import com.example.PaystackIntegrationApp.service.PaymentService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            // Log the error properly
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing payment reference: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
     @GetMapping("/cards/{userId}") // New endpoint to get cards for a user
    public ResponseEntity<List<PaymentMethod>> getCards(@PathVariable String userId) {
        try {
            List<PaymentMethod> paymentMethods = paymentService.getPaymentMethodsByUserId(userId);
            if (paymentMethods.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Or return an empty list, depending on your preference
            }
            return new ResponseEntity<>(paymentMethods, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
        @DeleteMapping("/cards/{id}")  // New endpoint to delete a card by its ID
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
}
