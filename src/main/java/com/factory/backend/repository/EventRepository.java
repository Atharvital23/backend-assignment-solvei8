package com.factory.backend.repository;

import com.factory.backend.entity.MachineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<MachineEvent, String> {

	@Query("SELECT e FROM MachineEvent e WHERE e.machineId = :machineId AND e.eventTime >= :start AND e.eventTime < :end")
	List<MachineEvent> findEventsForStats(@Param("machineId") String machineId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	// Aggregation Query: Sum defects per machine, ordered by highest defects first
	@Query("SELECT e.machineId, SUM(e.defectCount), COUNT(e) " + "FROM MachineEvent e "
			+ "WHERE e.eventTime >= :start AND e.eventTime < :end AND e.defectCount <> -1 " + "GROUP BY e.machineId "
			+ "ORDER BY SUM(e.defectCount) DESC")
	List<Object[]> findTopDefectLines(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}