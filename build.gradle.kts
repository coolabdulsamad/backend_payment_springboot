plugins {
	java
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
        

        implementation("com.squareup.okhttp3:okhttp:4.11.0") // Use the latest version
        implementation("com.google.code.gson:gson:2.10.1") // Use the latest version
        runtimeOnly ("org.postgresql:postgresql")

            // Logging dependencies
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.slf4j:slf4j-api:2.0.12")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.2.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
