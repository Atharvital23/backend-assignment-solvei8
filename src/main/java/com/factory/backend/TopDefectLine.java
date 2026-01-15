package com.factory.backend;

import lombok.Data;

@Data
public class TopDefectLine {
	private String lineId; // We will map machineId to this lineId
	private long totalDefects;
	private long eventCount;
	private double defectsPercent; // Defects per 100 events

	// Constructor needed for the Service to build this object
	public TopDefectLine(String lineId, long totalDefects, long eventCount) {
		this.lineId = lineId;
		this.totalDefects = totalDefects;
		this.eventCount = eventCount;
		// Calculate percentage: (Defects / Events) * 100
		if (eventCount > 0) {
			this.defectsPercent = Math.round(((double) totalDefects / eventCount) * 100.0 * 100.0) / 100.0;
		} else {
			this.defectsPercent = 0.0;
		}
	}

	public String getLineId() {
		return lineId;
	}

	public long getTotalDefects() {
		return totalDefects;
	}

	public long getEventCount() {
		return eventCount;
	}

	public double getDefectsPercent() {
		return defectsPercent;
	}
}