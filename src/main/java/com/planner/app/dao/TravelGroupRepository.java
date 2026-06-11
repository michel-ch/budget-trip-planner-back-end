package com.planner.app.dao;

import com.planner.app.entity.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelGroupRepository extends JpaRepository<TravelGroup, Integer> {


}
