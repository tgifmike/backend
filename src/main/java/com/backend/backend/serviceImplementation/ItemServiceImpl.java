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
import jakarta.persistence.EntityNotFoundException;
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

        // fetch station
        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));


        // check if the item name already exists within this station
        boolean exists = itemRepository.existsByItemNameAndStationId(item.getItemName(), stationId);
        if (exists) {
            throw new IllegalStateException("Item with name '" + item.getItemName() + "' already exists for this station.");
        }

        // attach the station to item
        item.setStation(station);
        item.setItemActive(true);

        return itemRepository.save(item);
    }

    // get all items for station
    @Override
    @Transactional
    public List<ItemEntity> getItemsByStation(UUID stationId){
        return itemRepository.findByStationId(stationId);
    }


    //get item by id
    @Override
    @Transactional
    public ItemEntity getItemById(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with ID: " + id));
    }

    //update item
    @Override
    public ItemEntity updateItem(UUID id, ItemEntity updatedItem) {
        ItemEntity existingItem = getItemById(id);

        if (!existingItem.getItemName().equals(updatedItem.getItemName())) {
            boolean nameExists = itemRepository.existsByItemNameAndStationId(updatedItem.getItemName(), existingItem.getStation().getId());
            if (nameExists) {
                throw new IllegalStateException("Item with name '" + updatedItem.getItemName() + "' already exists for this station.");
            }
        }

        existingItem.setItemName(updatedItem.getItemName());
        existingItem.setItemTemperature(updatedItem.getItemTemperature());
        existingItem.setTempTaken(updatedItem.isTempTaken());
        existingItem.setCheckMark(updatedItem.isCheckMark());
        existingItem.setNotes(updatedItem.getNotes());
        existingItem.setItemActive(updatedItem.isItemActive());

        return itemRepository.save(existingItem);
    }

    //toggle active item
    @Override
    public ItemEntity toggleActive(UUID id, boolean active) {
        ItemEntity item = getItemById(id);
        item.setItemActive(active);
        return itemRepository.save(item);
    }

    //delete item
    @Override
    public void deleteItem(UUID id) {
        ItemEntity item = getItemById(id);
        itemRepository.delete(item);
    }

}
