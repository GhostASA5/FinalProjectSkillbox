package searchengine.services.startandstopInd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteYAML;
import searchengine.config.SitesList;
import searchengine.dto.startandstop.StartIndResponse;
import searchengine.modul.Site;
import searchengine.modul.SiteStatus;
import searchengine.services.indexPage.IndexPageService;
import searchengine.services.repository.IndexRepository;
import searchengine.services.repository.LemmaRepository;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.*;

@Service
public class StartAndStopIndexing implements StartIndService{

    private final SitesList sites;
    private int count = 3;
    private static boolean active;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final IndexPageService indexPageService;
    private final int numberOfCores = Runtime.getRuntime().availableProcessors();

    @Autowired
    public StartAndStopIndexing(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, IndexPageService indexPageService) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.indexPageService = indexPageService;
    }

    @Override
    public StartIndResponse beginIndexing() {
        startUpdate();
        StartIndResponse response = new StartIndResponse();
        response.setResult(true);
        return response;
    }

    private void startUpdate(){
        count = 0;
        active = true;
        ListUrl.URL_SET = new TreeSet<>();
        indexRepository.deleteAll();
        List<SiteYAML> sitesList = sites.getSites();
        for (SiteYAML siteYAML : sitesList) {
            new Thread(() -> updateSite(siteYAML)).start();
        }
    }

    private void updateSite(SiteYAML siteYAML){
        Site oldSite = siteRepository.getSiteByName(siteYAML.getName());
        lemmaRepository.deleteAllBySiteId(oldSite);
        pageRepository.deleteAllBySiteId(oldSite);
        siteRepository.delete(oldSite);

        Site newSite = new Site();
        newSite.setSiteStatus(SiteStatus.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        newSite.setUrl(siteYAML.getUrl());
        newSite.setName(siteYAML.getName());
        siteRepository.save(newSite);

        try(ForkJoinPool forkJoinPool = new ForkJoinPool(numberOfCores)){
            ListUrl listUrl = new ListUrl(newSite, newSite, pageRepository, siteRepository, indexPageService);
            forkJoinPool.invoke(listUrl);
            if (newSite.getSiteStatus().equals(SiteStatus.INDEXING)){
                newSite.setSiteStatus(SiteStatus.INDEXED);
                siteRepository.save(newSite);
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        count++;
    }

    public void stopIndexing(){
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public boolean isIndexing(){
        return count != sites.getSites().size();
    }
}
