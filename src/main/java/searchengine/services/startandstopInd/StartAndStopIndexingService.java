package searchengine.services.startandstopInd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.startandstop.StartIndResponse;
import searchengine.dto.startandstop.StopIndResponse;
import searchengine.modul.Lemma;
import searchengine.modul.Page;
import searchengine.modul.SiteStatus;
import searchengine.services.indexPage.IndexPageService;
import searchengine.services.indexPage.IndexService;
import searchengine.services.repository.IndexRepository;
import searchengine.services.repository.LemmaRepository;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class StartAndStopIndexingService implements StartIndService{

    private final SitesList sites;
    private int count = 3;
    private static boolean active;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final IndexPageService indexPageService;
    private final IndexService indexService;
    private final int numberOfCores = Runtime.getRuntime().availableProcessors();

    @Autowired
    public StartAndStopIndexingService(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, IndexPageService indexPageService, IndexService indexService) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.indexPageService = indexPageService;
        this.indexService = indexService;
    }

    @Override
    public StartIndResponse beginIndexing() {
        StartIndResponse response = new StartIndResponse();
        if (isIndexing()){
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            startUpdate();
            response.setResult(true);
        }
        return response;
    }

    private void startUpdate(){
        count = 0;
        active = true;
        ListUrl.concurrentSet = ConcurrentHashMap.newKeySet();

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            new Thread(() -> updateSite(site)).start();
        }
    }

    private void updateSite(Site site){
        if (siteRepository.getSitesCount() != 0){
            indexRepository.deleteAll();
            searchengine.modul.Site oldSite = siteRepository.getSiteByName(site.getName());
            lemmaRepository.deleteAllBySiteId(oldSite);
            pageRepository.deleteAllBySiteId(oldSite);
            siteRepository.delete(oldSite);
        }

        searchengine.modul.Site newSite = new searchengine.modul.Site();
        newSite.setSiteStatus(SiteStatus.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        newSite.setUrl(site.getUrl());
        newSite.setName(site.getName());
        siteRepository.save(newSite);

        try(ForkJoinPool forkJoinPool = new ForkJoinPool(numberOfCores)){
            ConcurrentHashMap<String, Lemma> con = new ConcurrentHashMap<>();
            List<Page> pages = new ArrayList<>();
            ListUrl listUrl = new ListUrl(newSite, newSite, pageRepository, siteRepository, indexPageService, con, pages);
            forkJoinPool.invoke(listUrl);
            indexPageService.indexSite(newSite, con);
            //indexService.indexSite(newSite, con);
            if (newSite.getSiteStatus().equals(SiteStatus.INDEXING)){
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setSiteStatus(SiteStatus.INDEXED);
                siteRepository.save(newSite);
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        count++;
    }

    @Override
    public StopIndResponse stopIndexing(){
        StopIndResponse response = new StopIndResponse();
        if (isIndexing()){
            response.setResult(true);
            active = false;
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }

    public static boolean isActive() {
        return active;
    }

    public boolean isIndexing(){
        return count != sites.getSites().size();
    }
}
