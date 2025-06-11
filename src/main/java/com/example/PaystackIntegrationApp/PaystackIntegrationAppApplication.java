package com.example.PaystackIntegrationApp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase; // Import FirebaseDatabase
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

    /**
     * Initializes the Firebase Admin SDK. This method ensures that the default FirebaseApp
     * is set up only once.
     * @return The initialized FirebaseApp instance.
     * @throws IOException If the service account key file cannot be loaded.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://purple-food-f9a89-default-rtdb.firebaseio.com") // <--- REPLACE THIS WITH YOUR FIREBASE PROJECT URL
                .build();

        if (FirebaseApp.getApps().isEmpty()) { // Check if already initialized to prevent errors on hot reload
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance(); // Return existing app if already initialized
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
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
