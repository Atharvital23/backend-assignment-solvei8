package com.factory.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "machine_events")
public class MachineEvent {

	@Id
	@Column(name = "event_id")
	private String eventId;

	@Column(name = "machine_id")
	private String machineId;

	@Column(name = "event_time")
	private LocalDateTime eventTime;

	@Column(name = "received_time")
	private LocalDateTime receivedTime;

	@Column(name = "duration_ms")
	private long durationMs;

	@Column(name = "defect_count")
	private int defectCount;

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}

	public LocalDateTime getEventTime() {
		return eventTime;
	}

	public void setEventTime(LocalDateTime eventTime) {
		this.eventTime = eventTime;
	}

	public LocalDateTime getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(LocalDateTime receivedTime) {
		this.receivedTime = receivedTime;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

	public int getDefectCount() {
		return defectCount;
	}

	public void setDefectCount(int defectCount) {
		this.defectCount = defectCount;
	}

	// Required for updates
	public void updateData(MachineEvent newData) {
		this.machineId = newData.machineId;
		this.eventTime = newData.eventTime;
		this.receivedTime = newData.receivedTime;
		this.durationMs = newData.durationMs;
		this.defectCount = newData.defectCount;
	}

}