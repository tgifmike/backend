package com.backend.backend.config;

import com.backend.backend.repositories.LineCheckItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TemperatureSnapshotBackfill implements ApplicationRunner {

    private final LineCheckItemRepository lineCheckItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        lineCheckItemRepository.backfillMissingTemperatureSnapshots();
    }
}
