package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexpage.IndexPageResponse;
import searchengine.dto.search.SearchErrorResponse;
import searchengine.dto.startandstop.StartIndResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.startandstop.StopIndResponse;
import searchengine.services.indexPage.IndexService;
import searchengine.services.search.SearchService;
import searchengine.services.startandstopInd.StartIndService;
import searchengine.services.statistics.StatisticsService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StartIndService startIndService;
    private final StatisticsService statisticsService;
    private final IndexService indexService;
    private final SearchService searchService;

    public ApiController(StartIndService startIndService, StatisticsService statisticsService, IndexService indexService, SearchService searchService) {
        this.startIndService = startIndService;
        this.statisticsService = statisticsService;
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndResponse> startIndexing() {
        return ResponseEntity.ok(startIndService.beginIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndResponse> stopIndexing(){
        return ResponseEntity.ok(startIndService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexPageResponse> indexPage(@RequestBody String url) {
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8).replaceAll("url=", "");
        return ResponseEntity.ok(indexService.indexPage(decodedUrl));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(value = "query", required = false) String query,
                                                 @RequestParam(value = "offset", required = false) Integer offset,
                                                 @RequestParam(value = "limit", required = false) Integer limit,
                                                 @RequestParam(value = "site", required = false) String site){

        if (query.equals("")){
            SearchErrorResponse response = new SearchErrorResponse();
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return ResponseEntity.ok(response);
        } else {
            if (site == null){
                return ResponseEntity.ok(searchService.search(query, offset, limit));
            } else {
                return ResponseEntity.ok(searchService.searchByOneSite(query, offset, limit, site));
            }
        }
    }
}
