package com.dorm.manag.controller;

import com.dorm.manag.dto.CreateIssueRequest;
import com.dorm.manag.dto.IssueDto;
import com.dorm.manag.entity.IssueCategory;
import com.dorm.manag.entity.IssuePriority;
import com.dorm.manag.entity.IssueStatus;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.IssueService;
import com.dorm.manag.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class IssueController {

    private final IssueService issueService;
    private final UserService userService;

    // ========== STUDENT ENDPOINTS ==========

    @PostMapping("/report")
    public ResponseEntity<?> reportIssue(@Valid @RequestBody CreateIssueRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            IssueDto issue = issueService.reportIssue(request, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Issue reported successfully");
            response.put("issue", issue);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error reporting issue: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to report issue");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/my-issues")
    public ResponseEntity<?> getMyIssues(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<IssueDto> issues;
            if (page == 0 && size == 10) {
                issues = issueService.getUserIssues(user);
            } else {
                issues = issueService.getUserIssues(user, page, size);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("issues", issues);
            response.put("totalCount", issues.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving user issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIssueById(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<IssueDto> issueOpt = issueService.getIssueById(id);
            if (issueOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Issue not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            IssueDto issue = issueOpt.get();
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user owns the issue or has admin privileges
            if (!issue.getUserId().equals(user.getId()) && !user.getRole().hasReceptionistPrivileges()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            return ResponseEntity.ok(issue);
        } catch (Exception e) {
            log.error("Error retrieving issue {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issue");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<?> reopenIssue(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            IssueDto reopenedIssue = issueService.reopenIssue(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Issue reopened successfully");
            response.put("issue", reopenedIssue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reopening issue {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to reopen issue");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> rateIssue(@PathVariable Long id,
            @RequestParam int rating,
            Authentication authentication) {
        try {
            if (rating < 1 || rating > 5) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid rating");
                errorResponse.put("message", "Rating must be between 1 and 5");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            IssueDto ratedIssue = issueService.rateIssueResolution(id, rating, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Issue rated successfully");
            response.put("issue", ratedIssue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rating issue {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to rate issue");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // ========== ADMIN & RECEPTIONIST ENDPOINTS ==========

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAllIssues(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<IssueDto> issues = issueService.getAllIssues();

            Map<String, Object> response = new HashMap<>();
            response.put("issues", issues);
            response.put("totalCount", issues.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving all issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/open")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getOpenIssues() {
        try {
            List<IssueDto> openIssues = issueService.getOpenIssues();
            return ResponseEntity.ok(openIssues);
        } catch (Exception e) {
            log.error("Error retrieving open issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve open issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/urgent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getUrgentIssues() {
        try {
            List<IssueDto> urgentIssues = issueService.getUrgentIssues();
            return ResponseEntity.ok(urgentIssues);
        } catch (Exception e) {
            log.error("Error retrieving urgent issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve urgent issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getOverdueIssues() {
        try {
            List<IssueDto> overdueIssues = issueService.getOverdueIssues();
            return ResponseEntity.ok(overdueIssues);
        } catch (Exception e) {
            log.error("Error retrieving overdue issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve overdue issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getIssuesByCategory(@PathVariable IssueCategory category) {
        try {
            List<IssueDto> issues = issueService.getIssuesByCategory(category);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("Error retrieving issues by category {}: {}", category, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issues by category");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getIssuesByStatus(@PathVariable IssueStatus status) {
        try {
            List<IssueDto> issues = issueService.getIssuesByStatus(status);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("Error retrieving issues by status {}: {}", status, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issues by status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-priority/{priority}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getIssuesByPriority(@PathVariable IssuePriority priority) {
        try {
            List<IssueDto> issues = issueService.getIssuesByCategory(null); // This would need to be implemented in
                                                                            // service
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("Error retrieving issues by priority {}: {}", priority, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issues by priority");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> updateIssueStatus(@PathVariable Long id,
            @RequestParam IssueStatus status,
            @RequestParam(required = false) String adminNotes) {
        try {
            IssueDto updatedIssue = issueService.updateIssueStatus(id, status, adminNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Issue status updated successfully");
            response.put("issue", updatedIssue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating issue {} status: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update issue status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignIssue(@PathVariable Long id,
            @RequestParam Long assignedToUserId) {
        try {
            IssueDto assignedIssue = issueService.assignIssue(id, assignedToUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Issue assigned successfully");
            response.put("issue", assignedIssue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error assigning issue {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign issue");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAssignedIssues(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<IssueDto> assignedIssues = issueService.getAssignedIssues(user);
            return ResponseEntity.ok(assignedIssues);
        } catch (Exception e) {
            log.error("Error retrieving assigned issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve assigned issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/resolution-notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> addResolutionNotes(@PathVariable Long id,
            @RequestParam String resolutionNotes) {
        try {
            IssueDto updatedIssue = issueService.addResolutionNotes(id, resolutionNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resolution notes added successfully");
            response.put("issue", updatedIssue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding resolution notes to issue {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to add resolution notes");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/unacknowledged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUnacknowledgedIssues(@RequestParam(defaultValue = "2") int hoursThreshold) {
        try {
            List<IssueDto> unacknowledgedIssues = issueService.getUnacknowledgedIssues(hoursThreshold);
            return ResponseEntity.ok(unacknowledgedIssues);
        } catch (Exception e) {
            log.error("Error retrieving unacknowledged issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve unacknowledged issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> searchIssues(@RequestParam(required = false) IssueCategory category,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) Long assignedToId) {
        try {
            List<IssueDto> issues = issueService.findIssuesByCriteria(category, status, priority, assignedToId);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("Error searching issues: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search issues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========== STATISTICS ENDPOINTS ==========

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getIssueStatistics() {
        try {
            IssueService.IssueStatsDto stats = issueService.getIssueStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving issue statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve issue statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stats/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            IssueService.IssueStatsDto stats = issueService.getIssueStatistics();
            dashboard.put("stats", stats);

            List<IssueDto> urgentIssues = issueService.getUrgentIssues();
            dashboard.put("urgentIssues", urgentIssues);

            List<IssueDto> overdueIssues = issueService.getOverdueIssues();
            dashboard.put("overdueIssues", overdueIssues);

            List<IssueDto> unacknowledgedIssues = issueService.getUnacknowledgedIssues(2);
            dashboard.put("unacknowledgedIssues", unacknowledgedIssues);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving dashboard statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve dashboard statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    @GetMapping("/categories")
    public ResponseEntity<?> getIssueCategories() {
        try {
            IssueCategory[] categories = IssueCategory.values();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error retrieving issue categories: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve categories");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/statuses")
    public ResponseEntity<?> getIssueStatuses() {
        try {
            IssueStatus[] statuses = IssueStatus.values();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("Error retrieving issue statuses: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statuses");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/priorities")
    public ResponseEntity<?> getIssuePriorities() {
        try {
            IssuePriority[] priorities = IssuePriority.values();
            return ResponseEntity.ok(priorities);
        } catch (Exception e) {
            log.error("Error retrieving issue priorities: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve priorities");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}