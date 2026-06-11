package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "itinerary",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voyage_id", "day_number"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String activity;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}