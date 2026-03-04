package com.neuroguard.userservice.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String username;
    @Email
    private String email;
    private String phoneNumber;
    private String gender;
    private Integer age;
    private String role;   // optional
    private String password; // optional, if provided encode
}