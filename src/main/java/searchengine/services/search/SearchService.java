package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse search(String query, Integer offset, Integer limit);

    SearchResponse searchByOneSite(String query, Integer offset, Integer limit, String site);
}
