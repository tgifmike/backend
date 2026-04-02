package com.backend.backend.controller;

import com.backend.backend.dto.ContactDto;
import com.backend.backend.dto.EmailRequestDto;
import com.backend.backend.dto.SalesEmailDto;
import com.backend.backend.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

//    @PostMapping("/send")
//    public ResponseEntity<?> sendEmail(@RequestBody EmailRequestDto request) {
//
//        emailService.sendEmail(
//                request.getTo(),
//                request.getSubject(),
//                request.getMessage()
//        );
//
//        return ResponseEntity.ok("Email sent successfully");
//    }
    @PostMapping("/contact")
    public ResponseEntity<?> contact(@Valid @RequestBody ContactDto request) {
        emailService.sendContactEmail(request.getName(), request.getEmail(), request.getMessage());
        return ResponseEntity.ok("Email sent");
    }

    @PostMapping("/sales")
    public ResponseEntity<?> sales(@Valid @RequestBody SalesEmailDto request) {
        emailService.sendSalesEmail(request.getName(), request.getEmail(), request.getRestaurant(), request.getLocations(), request.getMessage());
        return ResponseEntity.ok("Email sent");
    }

//    @PostMapping("/sales")
//    public ResponseEntity<?> sendSalesEmail(@RequestBody SalesEmailDto request) {
//
//        emailService.sendEmail(
//                request.getName(),
//                request.getEmail(),
//                request.getRestaurant(),
//                request.getLocations(),
//                request.getMessage()
//        );
//
//        return ResponseEntity.ok("Email sent successfully");
//    }
}
