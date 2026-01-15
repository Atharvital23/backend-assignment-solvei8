package com.factory.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.factory.backend.BatchSummary;
import com.factory.backend.StatsResponse;
import com.factory.backend.entity.MachineEvent;
import com.factory.backend.repository.EventRepository;

@Service
public class EventService {

	@Autowired
	private EventRepository repository;

	// We process the whole batch in one Transaction for data integrity
	@Transactional
	public BatchSummary processBatch(List<MachineEvent> incomingEvents) {
		BatchSummary summary = new BatchSummary();
		List<MachineEvent> toSave = new ArrayList<>();

		// 1. Separate valid events from invalid ones immediately
		List<MachineEvent> validEvents = new ArrayList<>();

		for (MachineEvent event : incomingEvents) {
			String validationError = validate(event);
			if (validationError != null) {
				summary.addRejection(event.getEventId(), validationError);
			} else {
				// receivedTime is set if missing
				if (event.getReceivedTime() == null) {
					event.setReceivedTime(LocalDateTime.now());
				}
				validEvents.add(event);
			}
		}

		if (validEvents.isEmpty()) {
			return summary;
		}

		// 2. Fetch all existing records
		List<String> validIds = validEvents.stream().map(MachineEvent::getEventId).toList();
		List<MachineEvent> existingEvents = repository.findAllById(validIds);

		Map<String, MachineEvent> existingMap = existingEvents.stream()
				.collect(Collectors.toMap(MachineEvent::getEventId, Function.identity()));

		// 3. Process Deduplication and Updates
		for (MachineEvent incoming : validEvents) {
			if (existingMap.containsKey(incoming.getEventId())) {
				MachineEvent existing = existingMap.get(incoming.getEventId());

				if (isPayloadIdentical(existing, incoming)) {
					// Identical Payload -> Ignore
					summary.setDeduped(summary.getDeduped() + 1);
				} else {
					// Different Payload -> Check Timestamps
					// ONLY update if incoming is NEWER (or existing has no time)
					if (existing.getReceivedTime() == null
							|| incoming.getReceivedTime().isAfter(existing.getReceivedTime())) {
						existing.updateData(incoming);
						toSave.add(existing);
						summary.setUpdated(summary.getUpdated() + 1);
					} else {
						// Incoming is OLDER -> Ignore it (Treat as dedupe/stale)
						summary.setDeduped(summary.getDeduped() + 1);
					}
				}
			} else {
				// New ID -> Insert
				toSave.add(incoming);
				summary.setAccepted(summary.getAccepted() + 1);

				// Add to map so next item in THIS batch sees it
				existingMap.put(incoming.getEventId(), incoming);
			}
		}

		repository.saveAll(toSave);
		return summary;
	}

	private String validate(MachineEvent event) {
		// Rule: durationMs < 0 or > 6 hours (21600000 ms)
		if (event.getDurationMs() < 0 || event.getDurationMs() > 21600000) {
			return "INVALID_DURATION";
		}
		// Rule: eventTime > 15 mins in future
		if (event.getEventTime().isAfter(LocalDateTime.now().plusMinutes(15))) {
			return "FUTURE_EVENT_TIME";
		}
		return null;
	}

	private boolean isPayloadIdentical(MachineEvent existing, MachineEvent incoming) {
		// Compare business fields (excluding receivedTime as per rules)
		return Objects.equals(existing.getMachineId(), incoming.getMachineId())
				&& Objects.equals(existing.getEventTime(), incoming.getEventTime())
				&& existing.getDurationMs() == incoming.getDurationMs()
				&& existing.getDefectCount() == incoming.getDefectCount();
	}

	// New Method for Stats
	public StatsResponse getStats(String machineId, LocalDateTime start, LocalDateTime end) {
		// 1. Fetch from DB
		List<MachineEvent> events = repository.findEventsForStats(machineId, start, end);

		// 2. Calculate Variables
		long totalDurationSeconds = java.time.Duration.between(start, end).getSeconds();
		double windowHours = totalDurationSeconds / 3600.0;

		int eventsCount = events.size();
		int defectsCount = 0;

		for (MachineEvent e : events) {
			// "defectCount = -1 means unknown -> store event but ignore it for defect
			// calculations"
			if (e.getDefectCount() != -1) {
				defectsCount += e.getDefectCount();
			}
		}

		// 3. Calculate Rate
		double avgDefectRate = 0.0;
		if (windowHours > 0) {
			avgDefectRate = defectsCount / windowHours;
		}

		// 4. Determine Status ("Healthy if avg defect rate < 2.0")
		String status = (avgDefectRate < 2.0) ? "Healthy" : "Warning";

		return new StatsResponse(machineId, start, end, eventsCount, defectsCount, avgDefectRate, status);
	}

	public List<com.factory.backend.TopDefectLine> getTopDefectLines(LocalDateTime start, LocalDateTime end) {
		// 1. Run the aggregation query
		List<Object[]> results = repository.findTopDefectLines(start, end);

		List<com.factory.backend.TopDefectLine> report = new ArrayList<>();

		// 2. Map the raw database rows (Object[]) to our nice Java class
		for (Object[] row : results) {
			String machineId = (String) row[0];
			Long totalDefects = (Long) row[1];
			Long totalEvents = (Long) row[2];

			report.add(new com.factory.backend.TopDefectLine(machineId, totalDefects, totalEvents));
		}

		return report;
	}
}