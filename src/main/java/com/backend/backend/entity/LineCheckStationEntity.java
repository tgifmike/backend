package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "line_check_stations")
public class LineCheckStationEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "line_check_id")
    @JsonBackReference("lineCheckE") // match name from LineCheckEntity
    private LineCheckEntity lineCheck;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonBackReference("station-linechecks")
    private StationEntity station;


//    @OneToMany(mappedBy = "lineCheckStation", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private Set<LineCheckItemEntity> items = new HashSet<>();

    @OneToMany(mappedBy = "lineCheckStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("LCSE")
    private List<LineCheckItemEntity> lineCheckItems = new ArrayList<>();



}

