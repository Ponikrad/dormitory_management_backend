package com.dorm.manag.controller;

import com.dorm.manag.dto.ResidentCardDto;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.ResidentCardService;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResidentCardController {

    private final ResidentCardService residentCardService;
    private final UserService userService;

    @GetMapping("/my-card")
    public ResponseEntity<?> getMyCard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<ResidentCardDto> card = residentCardService.getCardByUser(user);

            if (card.isPresent()) {
                return ResponseEntity.ok(card.get());
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No active resident card found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving resident card: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve card");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateCard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ResidentCardDto card = residentCardService.generateCard(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resident card generated successfully");
            response.put("card", card);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error generating resident card: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate card");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/verify/{qrCode}")
    public ResponseEntity<?> verifyCard(@PathVariable String qrCode) {
        try {
            Optional<ResidentCardDto> card = residentCardService.verifyQrCode(qrCode);

            if (card.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("card", card.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Invalid or expired QR code");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error verifying QR code: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Verification failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getCardByUserId(@PathVariable Long userId) {
        try {
            Optional<ResidentCardDto> card = residentCardService.getCardByUserId(userId);

            if (card.isPresent()) {
                return ResponseEntity.ok(card.get());
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No active card found for user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving card for user {}: {}", userId, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve card");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllActiveCards() {
        try {
            List<ResidentCardDto> cards = residentCardService.getActiveCards();
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("Error retrieving active cards: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve cards");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExpiredCards() {
        try {
            List<ResidentCardDto> cards = residentCardService.getExpiredCards();
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("Error retrieving expired cards: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve expired cards");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/deactivate/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateCard(@PathVariable Long userId) {
        try {
            residentCardService.deactivateCard(userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Card deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deactivating card for user {}: {}", userId, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to deactivate card");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCardStats() {
        try {
            long activeCards = residentCardService.countActiveCards();

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeCards", activeCards);
            stats.put("expiredCards", residentCardService.getExpiredCards().size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving card statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}