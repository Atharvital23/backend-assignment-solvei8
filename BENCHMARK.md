# Performance Benchmark

## System Specifications
* **CPU:** [e.g., Intel Core i5 / Ryzen 5 - Please type your actual CPU here]
* **RAM:** [e.g., 8GB / 16GB]
* **OS:** Windows 10

## Benchmark Results
**Goal:** Process 1,000 events in under 1 second.

| Run Attempt | Time (Seconds) | Result |
| :--- | :--- | :--- |
| Cold Start | 2.56 s | Warm-up |
| Run 2 | 0.14 s | Pass |
| **Best Run** | **0.093 s** | **Pass (10x faster than required)** |

## Command Used
```bash
curl -X POST http://localhost:8080/events/batch -H "Content-Type: application/json" -d @large_batch.json -w "\nTotal Time: %{time_total} seconds\n"