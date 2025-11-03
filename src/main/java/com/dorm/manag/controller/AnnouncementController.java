package com.dorm.manag.controller;

import com.dorm.manag.entity.Announcement;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.AnnouncementService;
import com.dorm.manag.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserService userService;

    // ========== PUBLIC/STUDENT ENDPOINTS ==========

    /**
     * Pobierz aktywne ogłoszenia (dla zalogowanego użytkownika)
     */
    @GetMapping
    public ResponseEntity<?> getPublishedAnnouncements(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Announcement> announcements = announcementService.getPublishedAnnouncements(user);
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error retrieving announcements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve announcements"));
        }
    }

    /**
     * Pobierz przypięte ogłoszenia
     */
    @GetMapping("/pinned")
    public ResponseEntity<?> getPinnedAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.getPinnedAnnouncements();
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error retrieving pinned announcements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve announcements"));
        }
    }

    /**
     * Pobierz pilne ogłoszenia
     */
    @GetMapping("/urgent")
    public ResponseEntity<?> getUrgentAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.getUrgentAnnouncements();
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error retrieving urgent announcements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve announcements"));
        }
    }

    /**
     * Pobierz ogłoszenie po ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnnouncementById(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getAnnouncementById(id)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));

            // Zwiększ licznik wyświetleń
            announcementService.incrementViewCount(id);

            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            log.error("Error retrieving announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Announcement not found"));
        }
    }

    /**
     * Potwierdź przeczytanie ogłoszenia
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAnnouncement(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            announcementService.acknowledgeAnnouncement(id, user);

            return ResponseEntity.ok(Map.of("message", "Announcement acknowledged"));
        } catch (Exception e) {
            log.error("Error acknowledging announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to acknowledge announcement"));
        }
    }

    /**
     * Wyszukaj ogłoszenia
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchAnnouncements(@RequestParam String query) {
        try {
            List<Announcement> announcements = announcementService.searchAnnouncements(query);
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error searching announcements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search announcements"));
        }
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Stwórz ogłoszenie
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> createAnnouncement(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User author = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String title = request.get("title").toString();
            String content = request.get("content").toString();
            Announcement.AnnouncementType type = Announcement.AnnouncementType.valueOf(
                    request.getOrDefault("type", "GENERAL").toString().toUpperCase());
            Announcement.AnnouncementPriority priority = Announcement.AnnouncementPriority.valueOf(
                    request.getOrDefault("priority", "NORMAL").toString().toUpperCase());
            String targetAudience = request.getOrDefault("targetAudience", "ALL").toString();
            boolean isPinned = Boolean.parseBoolean(request.getOrDefault("isPinned", "false").toString());
            boolean isUrgent = Boolean.parseBoolean(request.getOrDefault("isUrgent", "false").toString());

            Announcement announcement = announcementService.createAnnouncement(
                    author, title, content, type, priority, targetAudience, isPinned, isUrgent);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Announcement created successfully");
            response.put("announcement", announcement);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create announcement", "message", e.getMessage()));
        }
    }

    /**
     * Zaplanuj ogłoszenie
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> scheduleAnnouncement(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User author = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String title = request.get("title").toString();
            String content = request.get("content").toString();
            Announcement.AnnouncementType type = Announcement.AnnouncementType.valueOf(
                    request.get("type").toString().toUpperCase());
            LocalDateTime scheduledFor = LocalDateTime.parse(request.get("scheduledFor").toString());

            Announcement announcement = announcementService.scheduleAnnouncement(
                    author, title, content, type, scheduledFor);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Announcement scheduled successfully");
            response.put("announcement", announcement);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error scheduling announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to schedule announcement", "message", e.getMessage()));
        }
    }

    /**
     * Aktualizuj ogłoszenie
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User modifier = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String title = request.get("title").toString();
            String content = request.get("content").toString();
            Announcement.AnnouncementType type = Announcement.AnnouncementType.valueOf(
                    request.get("type").toString().toUpperCase());
            Announcement.AnnouncementPriority priority = Announcement.AnnouncementPriority.valueOf(
                    request.get("priority").toString().toUpperCase());
            String targetAudience = request.get("targetAudience").toString();
            boolean isPinned = Boolean.parseBoolean(request.get("isPinned").toString());
            boolean isUrgent = Boolean.parseBoolean(request.get("isUrgent").toString());

            Announcement announcement = announcementService.updateAnnouncement(
                    id, title, content, type, priority, targetAudience, isPinned, isUrgent, modifier);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Announcement updated successfully");
            response.put("announcement", announcement);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update announcement", "message", e.getMessage()));
        }
    }

    /**
     * Przypnij ogłoszenie
     */
    @PostMapping("/{id}/pin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> pinAnnouncement(@PathVariable Long id) {
        try {
            announcementService.pinAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement pinned successfully"));
        } catch (Exception e) {
            log.error("Error pinning announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to pin announcement"));
        }
    }

    /**
     * Odepnij ogłoszenie
     */
    @PostMapping("/{id}/unpin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> unpinAnnouncement(@PathVariable Long id) {
        try {
            announcementService.unpinAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement unpinned successfully"));
        } catch (Exception e) {
            log.error("Error unpinning announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to unpin announcement"));
        }
    }

    /**
     * Archiwizuj ogłoszenie
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> archiveAnnouncement(@PathVariable Long id) {
        try {
            announcementService.archiveAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement archived successfully"));
        } catch (Exception e) {
            log.error("Error archiving announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to archive announcement"));
        }
    }

    /**
     * Usuń ogłoszenie
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting announcement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to delete announcement"));
        }
    }

    /**
     * Pobierz ogłoszenia po typie
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAnnouncementsByType(@PathVariable String type) {
        try {
            Announcement.AnnouncementType announcementType = Announcement.AnnouncementType.valueOf(type.toUpperCase());
            List<Announcement> announcements = announcementService.getAnnouncementsByType(announcementType);
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error retrieving announcements by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid announcement type"));
        }
    }

    /**
     * Pobierz zaplanowane ogłoszenia
     */
    @GetMapping("/scheduled")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getScheduledAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.getScheduledAnnouncements();
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            log.error("Error retrieving scheduled announcements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve announcements"));
        }
    }

    /**
     * Statystyki ogłoszeń
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAnnouncementStatistics() {
        try {
            AnnouncementService.AnnouncementStatsDto stats = announcementService.getAnnouncementStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving announcement statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve statistics"));
        }
    }

    /**
     * Lista typów ogłoszeń
     */
    @GetMapping("/types")
    public ResponseEntity<?> getAnnouncementTypes() {
        try {
            List<Map<String, Object>> types = List.of(Announcement.AnnouncementType.values())
                    .stream()
                    .map(type -> Map.of(
                            "name", (Object) type.name(),
                            "displayName", type.getDisplayName(),
                            "defaultPriority", type.getDefaultPriority().name(),
                            "urgentByDefault", type.isUrgentByDefault()))
                    .toList();

            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error retrieving announcement types: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve types"));
        }
    }

    /**
     * Lista priorytetów ogłoszeń
     */
    @GetMapping("/priorities")
    public ResponseEntity<?> getAnnouncementPriorities() {
        try {
            List<Map<String, Object>> priorities = List.of(Announcement.AnnouncementPriority.values())
                    .stream()
                    .map(priority -> Map.of(
                            "name", (Object) priority.name(),
                            "displayName", priority.getDisplayName(),
                            "level", priority.getLevel(),
                            "color", priority.getColor()))
                    .toList();

            return ResponseEntity.ok(priorities);
        } catch (Exception e) {
            log.error("Error retrieving announcement priorities: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve priorities"));
        }
    }
}