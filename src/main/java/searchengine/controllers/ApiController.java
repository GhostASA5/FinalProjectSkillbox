package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StartIndResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.StopIndResponse;
import searchengine.services.ListUrl;
import searchengine.services.StartIndService;
import searchengine.services.StartIndexing;
import searchengine.services.StatisticsService;

import java.util.TreeSet;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StartIndexing startIndexing;
    private final StatisticsService statisticsService;
    private final StartIndService startIndService;

    public ApiController(StartIndexing startIndexing, StatisticsService statisticsService, StartIndService startIndService) {
        this.startIndexing = startIndexing;
        this.statisticsService = statisticsService;
        this.startIndService = startIndService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndResponse> startIndexing() {
        StartIndResponse response = new StartIndResponse();
        if(startIndexing.isIndexing()){
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else{
            ListUrl.URL_SET = new TreeSet<>();
            response = startIndService.getSites();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndResponse>  stopIndexing(){
        StopIndResponse response = new StopIndResponse();
        if (startIndexing.isIndexing()){
            response.setResult(true);
            startIndexing.stopIndexing();
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return ResponseEntity.ok(response);
    }
}
