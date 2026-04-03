package com.backend.backend.repositories;

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


    // Count line checks today
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


    // Count week-to-date checks
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


    // Average completion time (seconds) scoped by location
    @Query(value = """
SELECT AVG(EXTRACT(EPOCH FROM (lc.completed_at - lc.check_time)))
FROM line_check lc
JOIN line_check_station lcs
    ON lcs.line_check_id = lc.id
JOIN station s
    ON s.id = lcs.station_id
WHERE lc.completed_at IS NOT NULL
AND lc.check_time >= :startOfDay
AND lc.check_time < :endOfDay
AND s.location_id = :locationId
""", nativeQuery = true)
    Double avgCompletionSecondsToday(
            UUID locationId,
            Instant startOfDay,
            Instant endOfDay
    );

}