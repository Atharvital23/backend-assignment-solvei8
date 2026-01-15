package com.factory.backend;

import java.time.LocalDateTime;

public class StatsResponse {
	private String machineId;
	private LocalDateTime start;
	private LocalDateTime end;
	private int eventsCount;
	private int defectsCount;
	private double avgDefectRate;
	private String status;

	public StatsResponse(String machineId, LocalDateTime start, LocalDateTime end, int eventsCount, int defectsCount,
			double avgDefectRate, String status) {
		this.machineId = machineId;
		this.start = start;
		this.end = end;
		this.eventsCount = eventsCount;
		this.defectsCount = defectsCount;
		this.avgDefectRate = avgDefectRate;
		this.status = status;
	}

	// Getters and Setters
	public String getMachineId() {
		return machineId;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public int getEventsCount() {
		return eventsCount;
	}

	public int getDefectsCount() {
		return defectsCount;
	}

	public double getAvgDefectRate() {
		return avgDefectRate;
	}

	public String getStatus() {
		return status;
	}
}