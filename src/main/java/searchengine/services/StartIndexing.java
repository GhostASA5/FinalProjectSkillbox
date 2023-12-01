package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteYAML;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.modul.Site;
import searchengine.modul.SiteStatus;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.*;

@Service
public class StartIndexing implements StartIndService{

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static final int numberOfCores = Runtime.getRuntime().availableProcessors();


    @Autowired
    public StartIndexing(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public StartIndResponse getSites() {
        startUpdate();
        StartIndResponse response = new StartIndResponse();
        response.setResult(true);
        return response;
    }

    private void startUpdate(){
        List<SiteYAML> sitesList = sites.getSites();
        for (SiteYAML siteYAML : sitesList) {
            new Thread(() -> updateSite(siteYAML)).start();
        }
    }

    private void updateSite(SiteYAML siteYAML){
        Site oldSite = siteRepository.getSiteByName(siteYAML.getName());
        pageRepository.deleteAllBySiteId(oldSite);
        siteRepository.delete(oldSite);

        Site newSite = new Site();
        newSite.setSiteStatus(SiteStatus.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        newSite.setUrl(siteYAML.getUrl());
        newSite.setName(siteYAML.getName());
        siteRepository.save(newSite);

        try{

            ListUrl listUrl = new ListUrl(newSite, newSite, pageRepository, siteRepository);
            TreeSet<String> siteMap;
            siteMap = new ForkJoinPool(numberOfCores).invoke(listUrl);
            System.out.println(siteMap);
            System.out.println(siteMap.size());

            newSite.setSiteStatus(SiteStatus.INDEXED);
        } catch (Exception ex){
            newSite.setSiteStatus(SiteStatus.FAILED);
        }
        newSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(newSite);
    }
}
