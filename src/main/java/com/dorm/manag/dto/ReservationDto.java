package com.dorm.manag.dto;

import com.dorm.manag.entity.ReservationStatus;
import com.dorm.manag.entity.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    private Long id;

    // User information
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userRoomNumber;

    // Resource information
    private Long resourceId;
    private String resourceName;
    private ResourceType resourceType;
    private String resourceLocation;
    private String keyLocation;
    private Boolean requiresKey;

    // Reservation details
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private ReservationStatus status;

    private Integer numberOfPeople;

    private String notes;

    private String adminNotes;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedInAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    // Key management
    private Boolean keyPickedUp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime keyPickedUpAt;

    private String keyPickedUpBy;

    private Boolean keyReturned;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime keyReturnedAt;

    private String keyReturnedTo;

    // Payment (mostly $0 for dormitory resources)
    private BigDecimal totalCost;

    private String paymentStatus;

    private String cancellationReason;

    private Boolean reminderSent;

    // Calculated fields
    private int durationMinutes;

    private String formattedDuration;

    private boolean active;

    private boolean upcoming;

    private boolean overdue;

    private boolean canCancel;

    private boolean canCheckIn;

    private boolean canPickUpKey;

    private boolean needsKeyReturn;

    private long minutesUntilStart;

    private String statusDisplay;

    private String statusColor;

    // Helper methods
    public void calculateFields() {
        // Calculate duration
        if (startTime != null && endTime != null) {
            this.durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
            this.formattedDuration = formatDuration(durationMinutes);
        }

        // Calculate status flags
        this.active = status != null && status.isActive();
        this.overdue = endTime != null && LocalDateTime.now().isAfter(endTime) &&
                (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CONFIRMED);
        this.upcoming = status == ReservationStatus.CONFIRMED &&
                startTime != null && LocalDateTime.now().isBefore(startTime);

        // Calculate action flags
        this.canCancel = status != null && status.canBeCancelled() &&
                startTime != null && LocalDateTime.now().isBefore(startTime.minusHours(2));
        this.canCheckIn = status != null && status.canCheckIn() &&
                startTime != null && canCheckInNow();
        this.canPickUpKey = status == ReservationStatus.CONFIRMED &&
                !Boolean.TRUE.equals(keyPickedUp) &&
                Boolean.TRUE.equals(requiresKey) && canCheckInNow();
        this.needsKeyReturn = Boolean.TRUE.equals(keyPickedUp) &&
                !Boolean.TRUE.equals(keyReturned) &&
                Boolean.TRUE.equals(requiresKey);

        // Calculate time until start
        if (startTime != null) {
            this.minutesUntilStart = java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes();
        }

        // Set display strings
        this.statusDisplay = status != null ? status.getDisplayName() : "Unknown";
        this.statusColor = status != null ? status.getColor() : "#999999";
    }

    private boolean canCheckInNow() {
        if (startTime == null)
            return false;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = startTime.minusMinutes(15);
        LocalDateTime checkInEnd = startTime.plusMinutes(30);

        return now.isAfter(checkInStart) && now.isBefore(checkInEnd);
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + "h";
        } else {
            return hours + "h " + remainingMinutes + "m";
        }
    }

    // Display helper methods
    public String getTimeRange() {
        if (startTime == null || endTime == null)
            return "Unknown";

        return startTime.toLocalDate().equals(endTime.toLocalDate()) ? String.format("%s - %s",
                startTime.toLocalTime().toString(),
                endTime.toLocalTime().toString())
                : String.format("%s - %s",
                        startTime.toString().replace("T", " "),
                        endTime.toString().replace("T", " "));
    }

    public String getKeyStatus() {
        if (!Boolean.TRUE.equals(requiresKey)) {
            return "No key required";
        }

        if (Boolean.TRUE.equals(keyReturned)) {
            return "Key returned";
        } else if (Boolean.TRUE.equals(keyPickedUp)) {
            return "Key picked up - needs return";
        } else {
            return "Key not picked up";
        }
    }

    public String getNextAction() {
        if (canPickUpKey) {
            return "Pick up key at " + (keyLocation != null ? keyLocation : "reception");
        } else if (needsKeyReturn) {
            return "Return key to " + (keyLocation != null ? keyLocation : "reception");
        } else if (canCheckIn) {
            return "Ready to check in";
        } else if (upcoming) {
            long hours = minutesUntilStart / 60;
            long minutes = minutesUntilStart % 60;
            if (hours > 0) {
                return "Starts in " + hours + "h " + minutes + "m";
            } else {
                return "Starts in " + minutes + " minutes";
            }
        } else if (active) {
            return "Currently in use";
        } else if (status != null && status.isFinal()) {
            return "Reservation " + statusDisplay.toLowerCase();
        } else {
            return "No action needed";
        }
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ReservationDto dto = new ReservationDto();

        public Builder id(Long id) {
            dto.setId(id);
            return this;
        }

        public Builder userId(Long userId) {
            dto.setUserId(userId);
            return this;
        }

        public Builder userFullName(String userFullName) {
            dto.setUserFullName(userFullName);
            return this;
        }

        public Builder resourceId(Long resourceId) {
            dto.setResourceId(resourceId);
            return this;
        }

        public Builder resourceName(String resourceName) {
            dto.setResourceName(resourceName);
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            dto.setStartTime(startTime);
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            dto.setEndTime(endTime);
            return this;
        }

        public Builder status(ReservationStatus status) {
            dto.setStatus(status);
            return this;
        }

        public Builder requiresKey(Boolean requiresKey) {
            dto.setRequiresKey(requiresKey);
            return this;
        }

        public ReservationDto build() {
            dto.calculateFields();
            return dto;
        }
    }
}