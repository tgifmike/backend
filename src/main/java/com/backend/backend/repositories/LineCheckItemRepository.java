//package com.backend.backend.repositories;
//
//import com.backend.backend.entity.LineCheckItemEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//
//@Repository
//public interface LineCheckItemRepository extends JpaRepository<LineCheckItemEntity, UUID> {
//
//    List<LineCheckItemEntity> findByLineCheckStation_Id(UUID stationId);
//
//
//    // Missing items today
//    @Query("""
//            SELECT COUNT(i)
//            FROM LineCheckItemEntity i
//            WHERE i.isMissing = true
//              AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
//              AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
//              AND i.station.location.id = :locationId
//            """)
//    long countMissingItemsToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//
//    // Out-of-temp items today
//    @Query("""
//            SELECT COUNT(i)
//            FROM LineCheckItemEntity i
//            WHERE i.temperature IS NOT NULL
//              AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
//              AND i.isMissing = false
//              AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
//              AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
//              AND i.station.location.id = :locationId
//            """)
//    long countOutOfTempItemsToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//
//    // Names of out-of-temp items
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity i
//            WHERE i.temperature IS NOT NULL
//              AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
//              AND i.isMissing = false
//              AND i.lineCheckStation.lineCheck.checkTime >= :startOfDay
//              AND i.lineCheckStation.lineCheck.checkTime < :endOfDay
//              AND i.station.location.id = :locationId
//            """)
//    List<String> findOutOfTempItemNamesToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//
//    // Count items not prepped correctly
//    @Query("""
//            SELECT COUNT(i)
//            FROM LineCheckItemEntity i
//            JOIN i.lineCheckStation lcs
//            JOIN lcs.station s
//            WHERE i.isChecked = false
//              AND i.isMissing = false
//              AND lcs.lineCheck.checkTime >= :startOfDay
//              AND lcs.lineCheck.checkTime < :endOfDay
//              AND s.location.id = :locationId
//            """)
//    long countIncorrectPrepItemsToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//
//    // Names of incorrectly prepped items
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity i
//            JOIN i.lineCheckStation lcs
//            JOIN lcs.station s
//            WHERE i.isChecked = false
//              AND i.isMissing = false
//              AND lcs.lineCheck.checkTime >= :startOfDay
//              AND lcs.lineCheck.checkTime < :endOfDay
//              AND s.location.id = :locationId
//            """)
//    List<String> findIncorrectPrepItemNamesToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//    //name of missing
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity lci
//            JOIN lci.item i
//            JOIN lci.lineCheckStation lcs
//            JOIN lcs.station s
//            WHERE s.location.id = :locationId
//              AND lci.isMissing = true
//              AND lcs.lineCheck.checkTime >= :startOfDay
//              AND lcs.lineCheck.checkTime < :endOfDay
//            """)
//    List<String> findMissingItemNamesToday(
//            @Param("locationId") UUID locationId,
//            @Param("startOfDay") Instant startOfDay,
//            @Param("endOfDay") Instant endOfDay
//    );
//
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity i
//            WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
//              AND i.isMissing = true
//            """)
//    List<String> findMissingItemNamesByLineCheck(@Param("lineCheckId") UUID lineCheckId);
//
//    // Names of out-of-temp items for a given line check
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity i
//            WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
//              AND i.temperature IS NOT NULL
//              AND (i.temperature < i.item.minTemp OR i.temperature > i.item.maxTemp)
//              AND i.isMissing = false
//            """)
//    List<String> findOutOfTempItemNamesByLineCheck(@Param("lineCheckId") UUID lineCheckId);
//
//    // Names of incorrectly prepped items for a given line check
//    @Query("""
//            SELECT i.itemName
//            FROM LineCheckItemEntity i
//            WHERE i.lineCheckStation.lineCheck.id = :lineCheckId
//              AND i.isItemChecked = false
//              AND i.isMissing = false
//            """)
//    List<String> findIncorrectPrepItemNamesByLineCheck(@Param("lineCheckId") UUID lineCheckId);
//}

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

}