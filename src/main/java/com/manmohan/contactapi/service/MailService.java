package com.manmohan.contactapi.service;

import com.manmohan.contactapi.AppProperties;
import com.manmohan.contactapi.dto.ContactRequest;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final AppProperties props;

    public MailService(JavaMailSender sender, AppProperties props) {
        this.sender = sender;
        this.props = props;
    }

    /** Delivers the visitor's message to the portfolio owner. Reply-To is the visitor, so "Reply" just works. */
    public void sendToOwner(ContactRequest req) {
        String visitorName = clean(req.name());
        String visitorEmail = clean(req.email());
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            String from = props.contact().from();
            h.setFrom(new InternetAddress(from, "Portfolio Contact Form", StandardCharsets.UTF_8.name()));
            h.setTo(props.contact().to());
            h.setReplyTo(new InternetAddress(visitorEmail, visitorName, StandardCharsets.UTF_8.name()));
            h.setSubject("Portfolio message from " + visitorName);
            h.setText("""
                    New message from your portfolio contact form

                    Name:  %s
                    Email: %s

                    ------------------------------
                    %s
                    ------------------------------
                    Hit Reply to respond directly to the sender.
                    """.formatted(visitorName, visitorEmail, req.message().strip()), false);
            sender.send(mime);
            log.info("Contact email delivered for sender domain {}", domain(visitorEmail));
        } catch (Exception e) {
            // Never log message content / full address; just the failure.
            log.error("Failed to send contact email: {}", e.toString());
            throw new MailDeliveryException(e);
        }
    }

    /** Strips CR/LF and control chars so user input can never inject mail headers. */
    static String clean(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Cntrl}]", " ").strip();
    }

    private static String domain(String email) {
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1) : "unknown";
    }

    public static class MailDeliveryException extends RuntimeException {
        public MailDeliveryException(Throwable cause) {
            super("Mail delivery failed", cause);
        }
    }
}
