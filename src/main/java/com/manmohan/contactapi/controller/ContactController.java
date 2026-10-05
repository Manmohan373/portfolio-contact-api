package com.manmohan.contactapi.controller;

import com.manmohan.contactapi.dto.ContactRequest;
import com.manmohan.contactapi.service.MailService;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);
    private final MailService mail;

    public ContactController(MailService mail) {
        this.mail = mail;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> contact(@Valid @RequestBody ContactRequest req) {
        if (req.website() != null && !req.website().isBlank()) {
            log.info("Honeypot triggered – dropping submission");
            return ResponseEntity.ok(Map.of("success", true));   // look successful to the bot
        }
        mail.sendToOwner(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Thanks! Your message has been sent."));
    }
}
