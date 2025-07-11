package com.dorm.manag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidentCardDto {

    private Long id;
    private String cardNumber;
    private String qrCode;
    private LocalDate expiryDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Dane użytkownika dla karty
    private String firstName;
    private String lastName;
    private String roomNumber;
    private String studentId;
    private String email;
}