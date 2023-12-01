package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteYAML;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.modul.Site;
import searchengine.services.repository.SiteRepository;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private SiteRepository siteRepository;

    @Autowired
    public StatisticsServiceImpl(SiteRepository siteRepository, SitesList sites) {
        this.siteRepository = siteRepository;
        this.sites = sites;
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteYAML> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = siteRepository.getSiteByName(sitesList.get(i).getName());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            int pages = site.getPages().size();
            int lemmas = pages * 10000;
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getSiteStatus().toString());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().toInstant(ZoneOffset.UTC).toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
