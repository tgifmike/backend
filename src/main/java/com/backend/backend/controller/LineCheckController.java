package com.backend.backend.controller;

import com.backend.backend.dto.DashboardMetricsDto;
import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.service.LineCheckService;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.security.PinActionPrincipal;
import com.backend.backend.service.PinActionAuthorizationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/line-checks")
@RequiredArgsConstructor
public class LineCheckController {

    private final LineCheckService lineCheckService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final PinActionAuthorizationService pinActionAuthorizationService;

        // ---------------------------------------------------------
        // CREATE INITIAL LINE CHECK (before user fills items)
        // ---------------------------------------------------------
        @PostMapping("/create")
        public ResponseEntity<LineCheckDto> createLineCheck(
                @RequestParam("userId") UUID userId,
                @RequestBody List<UUID> stationIds,
                Authentication authentication
        ) {
            PinActionPrincipal pinPrincipal = pinPrincipal(authentication);
            UUID effectiveUserId = pinPrincipal != null ? pinPrincipal.userId() : userId;
            if (pinPrincipal != null) {
                pinActionAuthorizationService.validateCreate(pinPrincipal, effectiveUserId, stationIds);
            }
            LineCheckDto saved = lineCheckService.createLineCheck(effectiveUserId, stationIds);
            if (pinPrincipal != null) {
                pinActionAuthorizationService.recordOnlineAuthentication(saved.getId(), pinPrincipal);
            }
            return ResponseEntity.ok(saved);
        }


    // ---------------------------------------------------------
        // GET ALL LINE CHECKS (DTO)
        // ---------------------------------------------------------
        @GetMapping("/getAllLineChecks")
        public ResponseEntity<List<LineCheckDto>> getAllLineChecks() {
            return ResponseEntity.ok(lineCheckService.getAllLineChecksDto());
        }

        // ---------------------------------------------------------
        // GET SINGLE LINE CHECK
        // ---------------------------------------------------------
        @GetMapping("/{id}")
        public ResponseEntity<LineCheckDto> getLineCheck(@PathVariable UUID id) {
            return ResponseEntity.ok(lineCheckService.getLineCheckDtoById(id));
        }






    // ---------------------------------------------------------
        // SAVE COMPLETED LINE CHECK (mobile app submit)
        // ---------------------------------------------------------
    @PostMapping("/save")
    public ResponseEntity<LineCheckDto> saveLineCheck(@RequestBody LineCheckDto dto, Authentication authentication) {
        PinActionPrincipal pinPrincipal = pinPrincipal(authentication);
        if (pinPrincipal != null) {
            pinActionAuthorizationService.validateSave(pinPrincipal, dto.getId(), dto.getUserId());
        }
        LineCheckDto saved = lineCheckService.saveLineCheck(dto);
        return ResponseEntity.ok(saved);
    }

    private static PinActionPrincipal pinPrincipal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof PinActionPrincipal principal
                ? principal
                : null;
    }

        //get linecheck complted and by locaion
        @GetMapping("/completed/by-location/{locationId}")
        public ResponseEntity<List<LineCheckDto>> getCompletedLineChecksByLocation(@PathVariable UUID locationId) {
            List<LineCheckDto> lineChecks = lineCheckService.getCompletedLineChecksByLocation(locationId);
            return ResponseEntity.ok(lineChecks);
        }


    // ---------------------------------------------------------
    // dashboard endpoint
    // ---------------------------------------------------------

//    @GetMapping("/dashboard/{locationId}")
//    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics(@PathVariable UUID locationId) {
//        try {
//            DashboardMetricsDto metrics = lineCheckService.getDashboardMetrics(locationId);
//            return ResponseEntity.ok(metrics);
//        } catch (Exception e) {
//            e.printStackTrace(); // logs full error to Heroku console
//            return ResponseEntity.status(500).body(null);
//        }
//    }

    @GetMapping("/dashboard/{locationId}")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics(@PathVariable UUID locationId) {
        try {
            DashboardMetricsDto metrics = lineCheckService.getDashboardMetrics(locationId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            e.printStackTrace(); // logs full error
            return ResponseEntity.status(500).body(null);
        }
    }

}


