package com.planner.app.dao;

import com.planner.app.entity.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoyageRepository extends JpaRepository<Voyage, Integer> {
    @Query("SELECT v FROM Voyage v WHERE v.id = :id")
    Optional<Voyage> findById(@Param("id") Integer id);

}
