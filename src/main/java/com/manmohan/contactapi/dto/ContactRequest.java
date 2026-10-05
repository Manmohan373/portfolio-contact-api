package com.manmohan.contactapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload from the portfolio contact form.
 * {@code website} is a honeypot: real visitors never see or fill it, bots usually do.
 */
public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name is too long")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 200, message = "Email is too long")
        String email,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 3000, message = "Message must be 10–3000 characters")
        String message,

        String website) {
}
