package com.example.PaystackIntegrationApp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase; // Import FirebaseDatabase
import org.slf4j.Logger; // Import SLF4J Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class PaystackIntegrationAppApplication {

    private static final Logger logger = LoggerFactory.getLogger(PaystackIntegrationAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PaystackIntegrationAppApplication.class, args);
    }

    /**
     * Initializes the Firebase Admin SDK. This method ensures that the default FirebaseApp
     * is set up only once and provides robust error logging.
     * @return The initialized FirebaseApp instance.
     * @throws RuntimeException If the Firebase Admin SDK cannot be initialized.
     */
    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Attempting to initialize FirebaseApp with default name.");
                InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
                
                // IMPORTANT: Replace "https://YOUR-FIREBASE-PROJECT-ID.firebaseio.com" with your actual Firebase Realtime Database URL
                // Example: "https://my-project-id-default-rtdb.firebaseio.com" (find this in your Firebase console)
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://purple-food-f9a89-default-rtdb.firebaseio.com") // <--- REPLACE THIS WITH YOUR FIREBASE PROJECT URL
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized successfully: {}", app.getName());
                return app;
            } else {
                FirebaseApp existingApp = FirebaseApp.getInstance();
                logger.info("FirebaseApp already initialized, returning existing instance: {}", existingApp.getName());
                return existingApp;
            }
        } catch (IOException e) {
            logger.error("CRITICAL ERROR: Failed to load service account key file (serviceAccountKey.json). Make sure it's in src/main/resources and accessible.", e);
            throw new RuntimeException("Firebase Admin SDK initialization failed: Service account key not found or invalid.", e);
        } catch (Exception e) {
            logger.error("CRITICAL ERROR: Failed to initialize Firebase Admin SDK. Check your Firebase Database URL and service account key.", e);
            // Re-throw as runtime exception to prevent application from starting with broken Firebase
            throw new RuntimeException("Firebase Admin SDK initialization failed", e);
        }
    }

    /**
     * Exposes FirebaseDatabase as a Spring bean. This bean will be created only after
     * the FirebaseApp bean has been successfully initialized.
     * @param firebaseApp The FirebaseApp instance, injected by Spring.
     * @return The FirebaseDatabase instance for the default FirebaseApp.
     */
    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        logger.info("Retrieving FirebaseDatabase instance for FirebaseApp: {}", firebaseApp.getName());
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
