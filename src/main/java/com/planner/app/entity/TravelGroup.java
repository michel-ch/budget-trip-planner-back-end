package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "travel_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false,  length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "voyage_id")
    private Voyage voyage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime  createdAt =  OffsetDateTime.now();

    @ManyToMany(mappedBy = "travelGroups", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

}
