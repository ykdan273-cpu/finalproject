package com.example.vacancyscraper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/leak")
public class LeakController {

    private static final List<byte[]> LEAK = Collections.synchronizedList(new ArrayList<>());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "leak-filler");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> fillerTask;

    @PostMapping("/start")
    public synchronized ResponseEntity<String> startLeak(
            @RequestParam(name = "chunkKb", defaultValue = "1024") int chunkKb,
            @RequestParam(name = "periodMs", defaultValue = "100") long periodMs
    ) {
        if (fillerTask != null && !fillerTask.isDone()) {
            return ResponseEntity.ok("Leak is already running; chunks stored: " + LEAK.size());
        }
        int safeChunkKb = Math.max(1, chunkKb);
        long safePeriodMs = Math.max(10, periodMs);
        fillerTask = scheduler.scheduleAtFixedRate(
                () -> LEAK.add(new byte[safeChunkKb * 1024]),
                0,
                safePeriodMs,
                TimeUnit.MILLISECONDS
        );
        return ResponseEntity.ok(
                "Started leak: " + safeChunkKb + "KB allocated every " + safePeriodMs + "ms"
        );
    }

    @PostMapping("/stop")
    public synchronized ResponseEntity<String> stopLeak() {
        if (fillerTask != null) {
            fillerTask.cancel(false);
        }
        return ResponseEntity.ok("Leak stopped");
    }

    @PostMapping("/clear")
    public ResponseEntity<String> clearLeak() {
        int removed;
        long freedBytes;
        synchronized (LEAK) {
            removed = LEAK.size();
            freedBytes = LEAK.stream().mapToLong(arr -> arr.length).sum();
            LEAK.clear();
        }
        return ResponseEntity.ok(
                "Cleared " + removed + " chunks (~" + freedBytes / (1024 * 1024) + " MB)"
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        int chunks;
        long totalBytes;
        synchronized (LEAK) {
            chunks = LEAK.size();
            totalBytes = LEAK.stream().mapToLong(arr -> arr.length).sum();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chunks", chunks);
        body.put("totalBytes", totalBytes);
        body.put("totalMB", totalBytes / (1024.0 * 1024.0));
        body.put("leakRunning", fillerTask != null && !fillerTask.isDone());
        return ResponseEntity.ok(body);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
