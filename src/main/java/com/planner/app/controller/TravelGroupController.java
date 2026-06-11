package com.planner.app.controller;

import com.planner.app.service.TravelGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travelgroups")
@RequiredArgsConstructor
public class TravelGroupController {
    private final TravelGroupService travelGroupService;

}
