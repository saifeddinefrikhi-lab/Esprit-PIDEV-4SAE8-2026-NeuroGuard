package com.neuroguard.userservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String gender;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String password;

    /**
     * Latitude géographique de l'utilisateur (médecin ou patient).
     * Optionnel — renseigné lors de la création ou la mise à jour du profil.
     */
    private Double latitude;

    /**
     * Longitude géographique de l'utilisateur (médecin ou patient).
     */
    private Double longitude;

}