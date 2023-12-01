package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StartIndResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StartIndService;
import searchengine.services.StatisticsService;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final StartIndService startIndService;

    public ApiController(StatisticsService statisticsService, StartIndService startIndService) {
        this.statisticsService = statisticsService;
        this.startIndService = startIndService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndResponse> startIndexing() {
        return ResponseEntity.ok(startIndService.getSites());
    }
}
