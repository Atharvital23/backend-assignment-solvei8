package com.factory.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.factory.backend.BatchSummary;
import com.factory.backend.entity.MachineEvent;
import com.factory.backend.service.EventService;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    // Endpoint: POST /events/batch
    // Input: A JSON list of events
    @PostMapping("/batch")
    public BatchSummary ingestBatch(@RequestBody List<MachineEvent> events) {
        // 1. Receive the data
        // 2. Pass it to the service for processing
        return eventService.processBatch(events);
    }
    
    @GetMapping("/stats")
    public com.factory.backend.StatsResponse getStats(
            @RequestParam String machineId,
            @RequestParam String start, 
            @RequestParam String end) {
        
        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);
        
        return eventService.getStats(machineId, startTime, endTime);
    }	
    
    
    @GetMapping("/stats/top-defect-lines")
    public List<com.factory.backend.TopDefectLine> getTopDefectLines(
            @RequestParam(required = false) String factoryId, // Ignored for now as per data model
            @RequestParam String from, 
            @RequestParam String to) {
        
        LocalDateTime start = LocalDateTime.parse(from);
        LocalDateTime end = LocalDateTime.parse(to);
        
        return eventService.getTopDefectLines(start, end);
    }
}