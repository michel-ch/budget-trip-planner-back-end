package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "voyage_id", nullable = false, unique = true)
    private Voyage voyage;

    @Column(name = "transport_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal transportAmount = BigDecimal.ZERO;

    @Column(name = "hotel_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal hotelAmount = BigDecimal.ZERO;

    @Column(name = "restaurant_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal restaurantAmount = BigDecimal.ZERO;

    @Column(name = "activities_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal activitiesAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}