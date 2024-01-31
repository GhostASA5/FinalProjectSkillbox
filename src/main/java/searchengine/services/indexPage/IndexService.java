package searchengine.services.indexPage;

import searchengine.dto.indexpage.IndexPageResponse;
import searchengine.modul.Page;


public interface IndexService {

    IndexPageResponse indexPage(String url);

    void indexPage(String url, Page page);
}
