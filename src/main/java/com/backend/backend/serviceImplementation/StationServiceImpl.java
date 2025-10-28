package com.backend.backend.serviceImplementation;

import com.backend.backend.entity.StationEntity;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.service.StationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;

    public StationServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public StationEntity createStation(StationEntity station){
        if (stationRepository.existsByStationName(station.getStationName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Station name already exists");
        }
        return  stationRepository.save(station);
    }

   @Override
   public StationEntity updateStation(UUID id, StationEntity station){
        StationEntity existing = stationRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
        if (!existing.getStationName().equals(station.getStationName())
            && stationRepository.existsByStationName(station.getStationName())){
            throw new ResponseStatusException(HttpStatus. CONFLICT, "Station name already exists");
       }

        existing.setStationName(station.getStationName());
        existing.setUpdatedAt(LocalDateTime.now());

        return stationRepository.save(existing);
   }

   @Override
   public void deleteStation(UUID id){
        if(!stationRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found");
        }
        stationRepository.deleteById(id);
   }

    @Override
    public StationEntity getStationById(UUID id){
        return stationRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public StationEntity getStationByName(String stationName){
        return stationRepository.findByStationName(stationName)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public List<StationEntity> getAllStations(){
        return stationRepository.findAll();
    }

    @Override
    @Transactional
    public StationEntity toggleActive(UUID id, boolean active){
        StationEntity station = stationRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Station not found " + id));

        station.setStationActive(active);
        station.setUpdatedAt(LocalDateTime.now());

        return stationRepository.save(station);
    }


}
