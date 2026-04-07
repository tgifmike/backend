package com.backend.backend.repositories;

import com.backend.backend.entity.LineCheckItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LineCheckItemRepository extends JpaRepository<LineCheckItemEntity, UUID> {

    List<LineCheckItemEntity> findByLineCheckStation_Id(UUID stationId);


    /*
     * =========================
     * Missing items (TODAY)
     * =========================
     */

    @Query("""
        SELECT COUNT(i)
        FROM LineCheckItemEntity i
        WHERE i.isMissing = true
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    long countMissingItemsToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.isMissing = true
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    List<String> findMissingItemNamesToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    /*
     * =========================
     * Out-of-temp items (TODAY)
     * =========================
     */

    @Query("""
        SELECT COUNT(i)
        FROM LineCheckItemEntity i
        WHERE i.temperature IS NOT NULL
          AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
          AND i.isMissing = false
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    long countOutOfTempItemsToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.temperature IS NOT NULL
          AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
          AND i.isMissing = false
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    List<String> findOutOfTempItemNamesToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    /*
     * =========================
     * Incorrect prep items (TODAY)
     * =========================
     */

    @Query("""
        SELECT COUNT(i)
        FROM LineCheckItemEntity i
        WHERE i.isChecked = false
          AND i.isMissing = false
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    long countIncorrectPrepItemsToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.isChecked = false
          AND i.isMissing = false
          AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
          AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
          AND i.lineCheckStation.station.location.id = :locationId
    """)
    List<String> findIncorrectPrepItemNamesToday(
            @Param("locationId") UUID locationId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );


    /*
     * =========================
     * Missing items (PER LINE CHECK)
     * =========================
     */

    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
          AND i.isMissing = true
    """)
    List<String> findMissingItemNamesByLineCheck(
            @Param("lineCheckId") UUID lineCheckId
    );


    /*
     * =========================
     * Out-of-temp items (PER LINE CHECK)
     * =========================
     */

    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
          AND i.temperature IS NOT NULL
          AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
          AND i.isMissing = false
    """)
    List<String> findOutOfTempItemNamesByLineCheck(
            @Param("lineCheckId") UUID lineCheckId
    );


    /*
     * =========================
     * Incorrect prep items (PER LINE CHECK)
     * =========================
     */

    @Query("""
        SELECT i.item.itemName
        FROM LineCheckItemEntity i
        WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
          AND i.isChecked = false
          AND i.isMissing = false
    """)
    List<String> findIncorrectPrepItemNamesByLineCheck(
            @Param("lineCheckId") UUID lineCheckId
    );

//    missing items by weekday
@Query(value = """
SELECT TRIM(TO_CHAR(lc.check_time, 'Day')) AS dayOfWeek,
       COUNT(i.id) AS count
FROM line_check_items i
JOIN line_check_stations lcs ON i.line_check_station_id = lcs.id
JOIN line_checks lc ON lcs.line_check_id = lc.id
JOIN stations s ON lcs.station_id = s.id
WHERE i.is_missing = true
AND lc.check_time >= :startDate
AND lc.check_time <= NOW()
AND s.location_id = :locationId
GROUP BY dayOfWeek
ORDER BY count DESC
""", nativeQuery = true)
List<Object[]> missingItemsByWeekday(
        @Param("locationId") UUID locationId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
);

// out of temp weekday agregation
@Query(value = """
SELECT TRIM(TO_CHAR(lc.check_time, 'Day')) AS dayOfWeek,
       COUNT(i.id) AS count
FROM line_check_items i
JOIN items it ON it.id = i.item_id
JOIN line_check_stations lcs ON i.line_check_station_id = lcs.id
JOIN line_checks lc ON lcs.line_check_id = lc.id
JOIN stations s ON lcs.station_id = s.id
WHERE i.temperature IS NOT NULL
AND (i.temperature < it.min_temp OR i.temperature > it.max_temp)
AND i.is_missing = false
AND lc.check_time >= :startDate
AND lc.check_time <= NOW()
AND s.location_id = :locationId
GROUP BY dayOfWeek
ORDER BY count DESC
""", nativeQuery = true)
List<Object[]> outOfTempByWeekday(
        @Param("locationId") UUID locationId,
        @Param("startDate") Instant startDate
);

// incorrect prep weekday aggregation
@Query(value = """
SELECT TRIM(TO_CHAR(lc.check_time, 'Day')) AS dayOfWeek,
       COUNT(i.id) AS count
FROM line_check_items i
JOIN line_check_stations lcs ON i.line_check_station_id = lcs.id
JOIN line_checks lc ON lcs.line_check_id = lc.id
JOIN stations s ON lcs.station_id = s.id
WHERE i.is_checked = false
AND i.is_missing = false
AND lc.check_time >= :startDate
AND lc.check_time <= NOW()
AND s.location_id = :locationId
GROUP BY dayOfWeek
ORDER BY count DESC
""", nativeQuery = true)
List<Object[]> incorrectPrepByWeekday(
        @Param("locationId") UUID locationId,
        @Param("startDate") Instant startDate
);

}