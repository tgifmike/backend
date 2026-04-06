package com.backend.backend.repositories;

import com.backend.backend.dto.EmployeeCheckCountDto;
import com.backend.backend.entity.LineCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LineCheckRepository extends JpaRepository<LineCheckEntity, UUID> {

    @Query("""
        SELECT lc
        FROM LineCheckEntity lc
        LEFT JOIN FETCH lc.stations s
        LEFT JOIN FETCH s.lineCheckItems
        WHERE lc.id = :id
    """)
    Optional<LineCheckEntity> findByIdWithStationsAndItems(UUID id);

    List<LineCheckEntity> findAllByOrderByCheckTimeDesc();
    List<LineCheckEntity> findAllByCompletedAtIsNotNullOrderByCheckTimeDesc();

    List<LineCheckEntity>
    findDistinctByCompletedAtIsNotNullAndStations_Station_Location_IdOrderByCheckTimeDesc(
            UUID locationId
    );

    @Query("""
        SELECT COUNT(l)
        FROM LineCheckEntity l
        WHERE l.checkTime >= :startOfDay
        AND l.checkTime < :endOfDay
        AND l.completedAt IS NOT NULL
        AND EXISTS (
            SELECT s
            FROM LineCheckStationEntity s
            WHERE s.lineCheck = l
            AND s.station.location.id = :locationId
        )
    """)
    long countChecksToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    @Query("""
        SELECT COUNT(l)
        FROM LineCheckEntity l
        WHERE l.checkTime >= :startOfWeek
        AND l.completedAt IS NOT NULL
        AND EXISTS (
            SELECT s
            FROM LineCheckStationEntity s
            WHERE s.lineCheck = l
            AND s.station.location.id = :locationId
        )
    """)
    long countChecksWeekToDate(
            @Param("locationId") UUID locationId,
            @Param("startOfWeek") Instant startOfWeek
    );

    @Query("""
        SELECT COUNT(l)
        FROM LineCheckEntity l
        WHERE l.checkTime >= :startOfMonth
        AND l.completedAt IS NOT NULL
        AND EXISTS (
            SELECT s
            FROM LineCheckStationEntity s
            WHERE s.lineCheck = l
            AND s.station.location.id = :locationId
        )
    """)
    long countChecksMonthToDate(
            @Param("locationId") UUID locationId,
            @Param("startOfMonth") Instant startOfMonth
    );

    @Query("""
SELECT new com.backend.backend.dto.EmployeeCheckCountDto(
    lc.user.id,
    lc.user.userName,
    COUNT(lc)
)
FROM LineCheckEntity lc
WHERE lc.checkTime BETWEEN :start AND :end
AND lc.completedAt IS NOT NULL
AND EXISTS (
    SELECT s
    FROM LineCheckStationEntity s
    WHERE s.lineCheck = lc
    AND s.station.location.id = :locationId
)
GROUP BY lc.user.id, lc.user.userName
""")
    List<EmployeeCheckCountDto> countChecksPerEmployee(
            @Param("locationId") UUID locationId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    // ✅ Employee performance returning entities for Java processing
    @Query("""
SELECT DISTINCT lc
FROM LineCheckEntity lc
JOIN lc.stations s
WHERE lc.completedAt IS NOT NULL
AND lc.checkTime BETWEEN :start AND :end
AND s.station.location.id = :locationId
""")
    List<LineCheckEntity> employeePerformance(
            @Param("locationId") UUID locationId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
    SELECT lc
    FROM LineCheckEntity lc
    JOIN lc.stations s
    WHERE lc.checkTime >= :startOfDay
      AND lc.checkTime < :endOfDay
      AND lc.completedAt IS NOT NULL
      AND s.station.location.id = :locationId
    ORDER BY lc.checkTime ASC
""")
    List<LineCheckEntity> findByLocationAndCheckTimeBetween(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    @Query(value = """
    SELECT AVG(EXTRACT(EPOCH FROM (lc.completed_at - lc.check_time)))
    FROM line_checks lc
    JOIN line_check_stations lcs
        ON lcs.line_check_id = lc.id
    JOIN stations s
        ON s.id = lcs.station_id
    WHERE lc.completed_at IS NOT NULL
      AND lc.check_time >= :startOfDay
      AND lc.check_time < :endOfDay
      AND s.location_id = :locationId
""", nativeQuery = true)
    Double avgCompletionSecondsToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );
}