package com.factory.backend;

import java.io.FileWriter;
import java.io.IOException;

public class BenchmarkTool {
	public static void main(String[] args) {
		StringBuilder json = new StringBuilder("[");

		// Generate 1000 events
		for (int i = 0; i < 1000; i++) {
			json.append(String.format("""
					{
					    "eventId": "EVT-%d",
					    "machineId": "M-%d",
					    "eventTime": "2026-01-13T10:00:00",
					    "durationMs": %d,
					    "defectCount": 0
					}""", i, (i % 10), (1000 + i)));

			if (i < 999)
				json.append(",");
		}
		json.append("]");

		try (FileWriter file = new FileWriter("large_batch.json")) {
			file.write(json.toString());
			System.out.println("✅ Generated large_batch.json with 1000 events!");
			System.out.println("File location: " + System.getProperty("user.dir"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}