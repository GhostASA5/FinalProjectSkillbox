package searchengine.services.indexPage;

import searchengine.dto.indexpage.IndexPageResponse;
import searchengine.modul.Lemma;
import searchengine.modul.Site;

import java.util.concurrent.ConcurrentHashMap;


public interface IndexService {

    IndexPageResponse indexPage(String url);

    void indexSite(Site site, ConcurrentHashMap<String, Lemma> concurrentHashMap);

}
