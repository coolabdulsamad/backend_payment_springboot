package com.example.PaystackIntegrationApp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class PaystackIntegrationAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaystackIntegrationAppApplication.class, args);
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://purple-food-f9a89.firebaseio.com") // Replace with your Firebase project URL
                .build();

        if (FirebaseApp.getApps().isEmpty()) { // Check if already initialized
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance(); // Return existing app if already initialized
        }
    }
}