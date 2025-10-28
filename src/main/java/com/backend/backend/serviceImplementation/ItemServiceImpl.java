package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.service.ItemService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    private final StationRepository stationRepository;


    public ItemServiceImpl(ItemRepository itemRepository, StationRepository stationRepository){
        this.itemRepository = itemRepository;
        this.stationRepository = stationRepository;

    }

    @Override
    public ItemEntity createItem(UUID stationId, ItemEntity item) {

        // check if the item name already exists within this station
        if (itemRepository.existsByItemNameAndStationId(item.getItemName(), stationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item name already exists in this station");
        }

        // fetch station
        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));

        // attach the station to item
        item.setStation(station);

        return itemRepository.save(item);
    }




//    @Override
//    public LocationEntity updateLocation(UUID id, LocationEntity location){
//        LocationEntity existing = locationRepository.findById(id)
//                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
//
//        if (!existing.getLocationName().equals(location.getLocationName())
//                && locationRepository.existsByLocationNameAndAccountId(location.getLocationName(), existing.getAccount().getId())) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists in this account");
//        }
//
//        existing.setLocationName(location.getLocationName());
//        existing.setLocationStreet(location.getLocationStreet());
//        existing.setLocationTown(location.getLocationTown());
//        existing.setLocationState(location.getLocationState());
//        existing.setLocationZipCode(location.getLocationZipCode());
//        existing.setLocationTimeZone(location.getLocationTimeZone());
//
//        return locationRepository.save(existing);
//    }
//
//
//
//    @Override
//    public void deleteLocation(UUID id){
//        if(!locationRepository.existsById(id)){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
//        }
//        locationRepository.deleteById(id);
//    }
//
//    @Override
//    public LocationEntity getLocationById(UUID id){
//        return locationRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
//    }
//
//    @Override
//    public LocationEntity getLocationByName(String locationName){
//        return locationRepository.findByLocationName(locationName)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locaiton not found"));
//    }
//
//    @Override
//    public List<LocationEntity> getLocationByAccount(UUID accountId) {
//        List<LocationEntity> locations = locationRepository.findByAccountId(accountId);
//        if (locations.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No locations found for this account");
//        }
//        return locations;
//    }
//
//    @Override
//    public List<LocationEntity> getAllLocations() {
//        return  locationRepository.findAll();
//    }
//
//    @Override
//    @Transactional
//    public LocationDto toggleActive(UUID id, boolean active) {
//        LocationEntity location = locationRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
//
//        location.setLocationActive(active);
//        location.setUpdatedAt(LocalDateTime.now());
//
//        LocationEntity saved = locationRepository.save(location);
//
//        // Use the standardized converter
//        return LocationDto.fromEntity(saved);
//    }
//
//    @Override
//    public LocationEntity partialUpdate(UUID id, Map<String, Object> updates) {
//        LocationEntity existing = locationRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Location not found"));
//
//        if (updates.containsKey("locationName") && updates.get("locationName") != null) existing.setLocationName((String) updates.get("locationName"));
//        if (updates.containsKey("locationStreet") && updates.get("locationStreet") != null) existing.setLocationStreet((String) updates.get("locationStreet"));
//        if (updates.containsKey("locationTown") && updates.get("locationTown") != null) existing.setLocationTown((String) updates.get("locationTown"));
//        if (updates.containsKey("locationState") && updates.get("locationState") != null) existing.setLocationState((String) updates.get("locationState"));
//        if (updates.containsKey("locationZipCode") && updates.get("locationZipCode") != null) existing.setLocationZipCode((String) updates.get("locationZipCode"));
//        if (updates.containsKey("locationTimeZone") && updates.get("locationTimeZone") != null) existing.setLocationTimeZone((String) updates.get("locationTimeZone"));
//
//        return locationRepository.save(existing);
//    }
}
