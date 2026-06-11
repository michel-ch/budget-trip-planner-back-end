package com.planner.app.service;

import com.planner.app.dao.TravelGroupRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TravelGroupService {
    private TravelGroupRepository travelGroupRepository;
    

}
