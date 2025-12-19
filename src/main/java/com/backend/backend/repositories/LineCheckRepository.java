package com.backend.backend.repositories;

import com.backend.backend.entity.LineCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LineCheckRepository extends JpaRepository<LineCheckEntity, UUID> {

    @Query("""
    SELECT lc FROM LineCheckEntity lc
    LEFT JOIN FETCH lc.stations s
    LEFT JOIN FETCH s.lineCheckItems
    WHERE lc.id = :id
""")
    Optional<LineCheckEntity> findByIdWithStationsAndItems(UUID id);


    List<LineCheckEntity> findAllByOrderByCheckTimeDesc();
    List<LineCheckEntity> findAllByCompletedAtIsNotNullOrderByCheckTimeDesc();
    List<LineCheckEntity> findDistinctByCompletedAtIsNotNullAndStations_Station_Location_IdOrderByCheckTimeDesc(UUID locationId);
}

