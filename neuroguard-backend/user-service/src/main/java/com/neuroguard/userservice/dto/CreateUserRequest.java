package com.neuroguard.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String username;
    @Email @NotBlank
    private String email;
    private String phoneNumber;
    private String gender;
    private Integer age;
    @NotNull
    private String role;   // e.g., "PATIENT", "PROVIDER", etc.
    @NotBlank
    private String password;
}