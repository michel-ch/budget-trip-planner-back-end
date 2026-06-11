package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "object_type", nullable = false, columnDefinition = "TEXT")
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Integer objectId;

}