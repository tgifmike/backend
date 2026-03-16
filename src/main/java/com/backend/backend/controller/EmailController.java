package com.backend.backend.controller;

import com.backend.backend.dto.EmailRequestDto;
import com.backend.backend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequestDto request) {

        emailService.sendEmail(
                request.getTo(),
                request.getSubject(),
                request.getMessage()
        );

        return ResponseEntity.ok("Email sent successfully");
    }
}
