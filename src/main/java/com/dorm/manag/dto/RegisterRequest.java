package com.dorm.manag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Email musi być prawidłowy")
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 6, max = 20, message = "Hasło musi mieć od 6 do 20 znaków")
    private String password;

    @NotBlank(message = "Imię jest wymagane")
    @Size(min = 2, max = 50, message = "Imię musi mieć od 2 do 50 znaków")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    @Size(min = 2, max = 50, message = "Nazwisko musi mieć od 2 do 50 znaków")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Nieprawidłowy numer telefonu")
    private String phoneNumber;

    @NotBlank(message = "Numer pokoju jest wymagany")
    private String roomNumber;

    @NotBlank(message = "Numer studencki jest wymagany")
    private String studentId;
}