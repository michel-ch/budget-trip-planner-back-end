package com.planner.app.dto;

import com.planner.app.entity.Image;
import com.planner.app.entity.Voyage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyageDTO {
    private String objectType;
    private Integer objectId;
    private String destination;
    private BigDecimal budgetTotal;
    private Integer durationDays;
    private LocalDate startDate;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private Image coverImage;

    public VoyageDTO(String objectType, Integer objectId, String destination, BigDecimal budgetTotal, Integer durationDays, LocalDate startDate, Image coverImage) {
        this.objectType = objectType;
        this.objectId = objectId;
        this.destination = destination;
        this.budgetTotal = budgetTotal;
        this.durationDays = durationDays;
        this.startDate = startDate;
        this.coverImage = coverImage;
        this.createdAt = OffsetDateTime.now();
    }
}
