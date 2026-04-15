package com.esprit.microservice.careplanservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long providerId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(columnDefinition = "TEXT")
    private String notes;



    @CreationTimestamp
   private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
