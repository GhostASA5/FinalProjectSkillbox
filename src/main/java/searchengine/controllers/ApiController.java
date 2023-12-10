package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexpage.IndexPageResponse;
import searchengine.dto.startandstop.StartIndResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.startandstop.StopIndResponse;
import searchengine.services.indexPage.IndexPageService;
import searchengine.services.startandstopInd.ListUrl;
import searchengine.services.startandstopInd.StartIndService;
import searchengine.services.startandstopInd.StartAndStopIndexing;
import searchengine.services.statistics.StatisticsService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StartAndStopIndexing startAndStopIndexing;
    private final StatisticsService statisticsService;
    private final StartIndService startIndService;
    private final IndexPageService indexPageService;

    public ApiController(StartAndStopIndexing startAndStopIndexing, StatisticsService statisticsService, StartIndService startIndService, IndexPageService indexPageService) {
        this.startAndStopIndexing = startAndStopIndexing;
        this.statisticsService = statisticsService;
        this.startIndService = startIndService;
        this.indexPageService = indexPageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndResponse> startIndexing() {
        StartIndResponse response = new StartIndResponse();
        if(startAndStopIndexing.isIndexing()){
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else{
            ListUrl.URL_SET = new TreeSet<>();
            response = startIndService.beginIndexing();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndResponse> stopIndexing(){
        StopIndResponse response = new StopIndResponse();
        if (startAndStopIndexing.isIndexing()){
            response.setResult(true);
            startAndStopIndexing.stopIndexing();
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexPageResponse> indexPage(@RequestBody String url) {
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8).replaceAll("url=", "");
        IndexPageResponse response = new IndexPageResponse();
        response.setResult(true);
        indexPageService.indexPage(decodedUrl);
        return ResponseEntity.ok(response);
    }
}
