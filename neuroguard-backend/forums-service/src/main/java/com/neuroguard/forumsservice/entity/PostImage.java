package com.neuroguard.forumsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post_images")
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** Stored file name (relative path under upload dir) e.g. post-images/1_abc123.jpg */
    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
