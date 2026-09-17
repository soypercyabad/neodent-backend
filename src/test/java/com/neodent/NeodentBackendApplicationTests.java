package com.neodent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "DB_URL=jdbc:mysql://localhost:3306/neodent_test",
    "DB_USERNAME=root",
    "DB_PASSWORD=root",
    "SPRING_JPA_HIBERNATE_DDL_AUTO=none",
    "SHOW_SQL=false",
    "PORT=8080",
    "DNI_API_TOKEN=test-token",
    "BREVO_API_KEY=test-brevo-key",
    "BREVO_SENDER_EMAIL=test@neodent.pe",
    "JWT_SECRET=neodent-jwt-super-secret-key-which-has-more-than-256-bits-length",
    "TURNSTILE_SECRET=test-turnstile-secret",
    "FRONTEND_URL=http://localhost:3000"
})
class NeodentBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
