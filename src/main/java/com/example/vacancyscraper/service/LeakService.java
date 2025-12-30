package com.example.vacancyscraper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Управляемая "утечка" памяти для профилирования: накапливает чанки byte[] по расписанию.
 */
@Service
public class LeakService {

    private static final Logger log = LoggerFactory.getLogger(LeakService.class);

    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<byte[]> chunks = Collections.synchronizedList(new ArrayList<>());
    private volatile ScheduledFuture<?> task;

    public LeakService() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "leak-generator");
            t.setDaemon(true);
            return t;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(tf);
    }

    public Map<String, Object> start(int chunkKb, long periodMs) {
        if (running.compareAndSet(false, true)) {
            task = scheduler.scheduleAtFixedRate(() -> {
                chunks.add(new byte[chunkKb * 1024]);
            }, 0, periodMs, TimeUnit.MILLISECONDS);
            log.info("Leak started: chunk={} KB, period={} ms", chunkKb, periodMs);
        }
        return stats();
    }

    public Map<String, Object> stop() {
        if (running.compareAndSet(true, false) && task != null) {
            task.cancel(true);
            log.info("Leak stopped");
        }
        return stats();
    }

    public Map<String, Object> clear() {
        chunks.clear();
        System.gc();
        log.info("Leak chunks cleared");
        return stats();
    }

    public Map<String, Object> stats() {
        long totalBytes;
        synchronized (chunks) {
            totalBytes = chunks.stream().mapToLong(b -> b.length).sum();
        }
        return Map.of(
                "running", running.get(),
                "chunks", chunks.size(),
                "totalBytes", totalBytes
        );
    }
}
