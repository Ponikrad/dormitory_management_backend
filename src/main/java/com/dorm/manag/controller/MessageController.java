package com.dorm.manag.controller;

import com.dorm.manag.entity.Message;
import com.dorm.manag.entity.MessageType;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.MessageService;
import com.dorm.manag.service.UserService;
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

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    // USER

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) MessageType type,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User sender = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            MessageType messageType = type != null ? type : MessageType.DIRECT;
            Message message = messageService.sendMessageToAdmin(sender, subject, content, messageType);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Message sent successfully");
            response.put("messageId", message.getId());
            response.put("threadId", message.getThreadId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> getInbox(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Message> messages = messageService.getInboxMessages(user);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving inbox: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve inbox");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentMessages(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Message> messages = messageService.getSentMessages(user);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving sent messages: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve sent messages");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Message> messages = messageService.getUnreadMessages(user);

            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("count", messages.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving unread messages: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve unread messages");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<?> getMessageThread(@PathVariable String threadId, Authentication authentication) {
        try {
            List<Message> thread = messageService.getMessageThread(threadId);
            return ResponseEntity.ok(thread);
        } catch (Exception e) {
            log.error("Error retrieving thread: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve thread");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Message message = messageService.getMessageById(id)
                    .orElseThrow(() -> new RuntimeException("Message not found"));

            // Check access
            boolean hasAccess = message.getSender().getId().equals(user.getId()) ||
                    (message.getRecipient() != null && message.getRecipient().getId().equals(user.getId())) ||
                    user.getRole().hasReceptionistPrivileges();

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error retrieving message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyToMessage(@PathVariable Long id,
            @RequestParam String content,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User sender = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Message reply = messageService.replyToMessage(id, sender, content);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reply sent successfully");
            response.put("reply", reply);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error replying to message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send reply");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Message message = messageService.markAsRead(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Marked as read");
            response.put("messageId", message.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to mark as read");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            messageService.deleteMessage(id, user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Message deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // ADMIN

    @GetMapping("/requiring-response")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getMessagesRequiringResponse() {
        try {
            List<Message> messages = messageService.getMessagesRequiringResponse();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving messages requiring response: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve messages");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOverdueMessages() {
        try {
            List<Message> messages = messageService.getOverdueMessages();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving overdue messages: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve overdue messages");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignMessage(@PathVariable Long id,
            @RequestParam Long assigneeId) {
        try {
            User assignee = userService.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Message message = messageService.assignMessage(id, assignee);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Message assigned successfully");
            response.put("assignedTo", assignee.getFullName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error assigning message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> resolveMessage(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Message message = messageService.markAsResolved(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Message resolved");
            response.put("messageId", message.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resolving message: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to resolve message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMessageStatistics() {
        try {
            MessageService.MessageStatsDto stats = messageService.getMessageStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving message statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}