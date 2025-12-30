package com.example.vacancyscraper.controller;

import com.example.vacancyscraper.service.LeakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoints to start/stop a controlled memory leak for profiling demos.
 */
@RestController
@RequestMapping("/leak")
public class LeakController {

    private final LeakService leakService;

    public LeakController(LeakService leakService) {
        this.leakService = leakService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startLeak(
            @RequestParam(defaultValue = "1024") int chunkKb,
            @RequestParam(defaultValue = "50") long periodMs) {
        return ResponseEntity.ok(leakService.start(chunkKb, periodMs));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopLeak() {
        return ResponseEntity.ok(leakService.stop());
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearLeak() {
        return ResponseEntity.ok(leakService.clear());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> leakStats() {
        return ResponseEntity.ok(leakService.stats());
    }
}
