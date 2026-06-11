package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "voyages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voyage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "object_type", nullable = false, columnDefinition = "TEXT")
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Integer objectId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String destination;

    @Column(name = "budget_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne
    @JoinColumn(name = "cover_image_id")
    private Image coverImage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

}