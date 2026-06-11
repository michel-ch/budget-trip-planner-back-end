package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name= "locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "city", length = 150, nullable = false)
    private String city;

    @Column(name = "country", length = 100, nullable = false)
    private String country;

    public Location(String city, String country) {
        this.city = city;
        this.country = country;
    }

}
