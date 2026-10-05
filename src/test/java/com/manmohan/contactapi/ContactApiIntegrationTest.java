package com.manmohan.contactapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end: real HTTP layer + real SMTP conversation against an in-memory GreenMail server. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "app.contact.to=owner@example.com",
        "app.contact.from=sender@example.com",
        "app.cors.allowed-origins=https://manmohan373.github.io",
        "app.rate-limit.max-requests=3"
})
class ContactApiIntegrationTest {

    @RegisterExtension
    GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired MockMvc mvc;

    private static String body(String name, String email, String msg, String honeypot) {
        return """
               {"name":"%s","email":"%s","message":"%s","website":"%s"}""".formatted(name, email, msg, honeypot);
    }

    @Test
    void validSubmissionIsDeliveredWithReplyTo() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://manmohan373.github.io").with(ip("10.0.0.1"))
                        .content(body("Asha Recruiter", "asha@corp.com", "Hi Manmohan, we have a Java role for you.", "")))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://manmohan373.github.io"))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(smtp.waitForIncomingEmail(2000, 1)).isTrue();
        MimeMessage m = smtp.getReceivedMessages()[0];
        assertThat(m.getAllRecipients()[0].toString()).isEqualTo("owner@example.com");
        assertThat(m.getReplyTo()[0].toString()).contains("asha@corp.com");
        assertThat(m.getSubject()).isEqualTo("Portfolio message from Asha Recruiter");
        assertThat(GreenMailUtil.getBody(m)).contains("we have a Java role");
    }

    @Test
    void headerInjectionInNameIsNeutralised() throws Exception {
        int before = smtp.getReceivedMessages().length;
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).with(ip("10.0.0.2"))
                        .content(body("Evil\\r\\nBcc: victim@x.com", "a@b.com", "A perfectly normal long message.", "")))
                .andExpect(status().isOk());
        assertThat(smtp.waitForIncomingEmail(2000, before + 1)).isTrue();
        MimeMessage m = smtp.getReceivedMessages()[before];
        assertThat(m.getHeader("Bcc")).isNull();
        assertThat(m.getSubject()).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void invalidInputGets400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).with(ip("10.0.0.3"))
                        .content(body("", "not-an-email", "short", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    void honeypotLooksSuccessfulButSendsNothing() throws Exception {
        int before = smtp.getReceivedMessages().length;
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).with(ip("10.0.0.4"))
                        .content(body("Bot", "bot@spam.com", "Buy cheap watches now, very cheap!", "http://spam")))
                .andExpect(status().isOk());
        Thread.sleep(300);
        assertThat(smtp.getReceivedMessages().length).isEqualTo(before);
    }

    @Test
    void foreignOriginIsRejectedByCors() throws Exception {
        mvc.perform(options("/api/contact").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateLimitReturns429WithCorsHeaders() throws Exception {
        String ok = body("Rate Tester", "r@t.com", "This is a rate limit test message.", "");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).with(ip("10.0.0.9")).content(ok))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://manmohan373.github.io").with(ip("10.0.0.9")).content(ok))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://manmohan373.github.io"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String addr) {
        return r -> { r.setRemoteAddr(addr); return r; };
    }
}
