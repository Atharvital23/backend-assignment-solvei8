package com.factory.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.factory.backend.entity.MachineEvent;
import com.factory.backend.repository.EventRepository;
import com.factory.backend.service.EventService;
	
@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository repository;

    @BeforeEach
    void setUp() {
        // Clear DB to ensure clean state for each test
        repository.deleteAll();
    }

    // --- Helper to create a standard event ---
    private MachineEvent createEvent(String eventId, String machineId, int defectCount, LocalDateTime receivedTime) {
        MachineEvent e = new MachineEvent();
        e.setEventId(eventId);
        e.setMachineId(machineId);
        e.setEventTime(LocalDateTime.now()); // Valid time
        e.setReceivedTime(receivedTime);
        e.setDurationMs(1000); // Valid duration
        e.setDefectCount(defectCount);
        return e;
    }

    // TEST 1: Identical duplicate eventId -> deduped [cite: 104]
    @Test
    void testIdenticalDuplicatesAreDeduped() {
        MachineEvent e1 = createEvent("E-1", "M-1", 0, LocalDateTime.now());
        MachineEvent e2 = createEvent("E-1", "M-1", 0, LocalDateTime.now()); // Identical

        BatchSummary summary = eventService.processBatch(List.of(e1, e2));

        assertEquals(1, summary.getAccepted(), "Should accept the first one");
        assertEquals(1, summary.getDeduped(), "Should dedupe the second one");
        assertEquals(1, repository.count(), "Database should only have 1 record");
    }

    // TEST 2: Different payload + newer receivedTime -> update happens [cite: 105]
    @Test
    void testUpdateHappensForNewerData() {
        // First event (Old)
        MachineEvent oldEvent = createEvent("E-1", "M-1", 0, LocalDateTime.now().minusMinutes(10));
        eventService.processBatch(List.of(oldEvent));

        // Second event (Newer) - Changed defect count to 5
        MachineEvent newEvent = createEvent("E-1", "M-1", 5, LocalDateTime.now());
        
        BatchSummary summary = eventService.processBatch(List.of(newEvent));

        assertEquals(1, summary.getUpdated(), "Should count as an update");
        MachineEvent saved = repository.findById("E-1").orElseThrow();
        assertEquals(5, saved.getDefectCount(), "The value in DB should be updated to 5");
    }

    // TEST 3: Different payload + older receivedTime -> ignored [cite: 106]
    @Test
    void testOlderUpdatesAreIgnored() {
        // First event (New)
        MachineEvent newEvent = createEvent("E-1", "M-1", 10, LocalDateTime.now());
        eventService.processBatch(List.of(newEvent));

        // Second event (Older) - Try to overwrite with 0 defects
        MachineEvent oldEvent = createEvent("E-1", "M-1", 0, LocalDateTime.now().minusMinutes(20));
        
        BatchSummary summary = eventService.processBatch(List.of(oldEvent));

        // Depending on your logic, this might be 'deduped' or just ignored. 
        // As long as DB isn't changed, it passes.
        MachineEvent saved = repository.findById("E-1").orElseThrow();
        assertEquals(10, saved.getDefectCount(), "DB should keep the newer value (10)");
    }

    // TEST 4: Invalid duration rejected [cite: 107]
    @Test
    void testInvalidDurationRejected() {
        MachineEvent e = createEvent("E-1", "M-1", 0, LocalDateTime.now());
        e.setDurationMs(-50); // Invalid

        BatchSummary summary = eventService.processBatch(List.of(e));

        assertEquals(1, summary.getRejected());
        assertEquals(0, repository.count());
    }

    // TEST 5: Future eventTime rejected [cite: 108]
    @Test
    void testFutureEventTimeRejected() {
        MachineEvent e = createEvent("E-1", "M-1", 0, LocalDateTime.now());
        e.setEventTime(LocalDateTime.now().plusDays(2)); // Future

        BatchSummary summary = eventService.processBatch(List.of(e));

        assertEquals(1, summary.getRejected());
        assertTrue(summary.getRejections().get(0).getReason().contains("FUTURE"));
    }

    // TEST 6: DefectCount = -1 ignored in totals [cite: 109]
    @Test
    void testUnknownDefectsIgnoredInStats() {
        // Event 1: 5 defects
        MachineEvent e1 = createEvent("E-1", "M-1", 5, LocalDateTime.now());
        // Event 2: -1 defects (Unknown)
        MachineEvent e2 = createEvent("E-2", "M-1", -1, LocalDateTime.now().plusMinutes(1));
        
        eventService.processBatch(List.of(e1, e2));

        // Get stats
        StatsResponse stats = eventService.getStats("M-1", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));

        assertEquals(2, stats.getEventsCount(), "Should count both events");
        assertEquals(5, stats.getDefectsCount(), "Should ignore the -1, so total is 5");
    }

    // TEST 7: start/end boundary correctness [cite: 110]
    @Test
    void testStatsTimeWindowBoundaries() {
        LocalDateTime now = LocalDateTime.now();
        MachineEvent e1 = createEvent("E-1", "M-1", 0, now); // Exact match
        eventService.processBatch(List.of(e1));

        // Window ends exactly at 'now' (Exclusive end) -> Should NOT find it
        StatsResponse statsBefore = eventService.getStats("M-1", now.minusHours(1), now);
        assertEquals(0, statsBefore.getEventsCount(), "Exclusive end should exclude event at exact boundary");

        // Window starts exactly at 'now' (Inclusive start) -> Should FIND it
        StatsResponse statsAfter = eventService.getStats("M-1", now, now.plusHours(1));
        assertEquals(1, statsAfter.getEventsCount(), "Inclusive start should include event at exact boundary");
    }

    // TEST 8: Thread-safety test [cite: 111]
    @Test
    void testThreadSafetyConcurrentIngestion() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Try to insert 10 DIFFERENT events at the exact same time
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    MachineEvent e = createEvent("E-" + index, "M-1", 0, LocalDateTime.now());
                    eventService.processBatch(List.of(e));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Wait for all threads to finish
        assertEquals(10, repository.count(), "All 10 concurrent events should be saved without race conditions");
    }
}