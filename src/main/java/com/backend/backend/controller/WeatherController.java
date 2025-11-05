package com.backend.backend.controller;

import com.backend.backend.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService){
        this.weatherService = weatherService;
    }

    @GetMapping("/getWeather")
    public Map<String, Object> getWeather(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        return weatherService.getWeather(lat, lon);
    }
}
