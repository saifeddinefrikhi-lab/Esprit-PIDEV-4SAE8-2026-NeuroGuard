// UserDto.java
package com.neuroguard.userservice.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String gender;   // new
    private Integer age;


}
