package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.modul.Lemma;
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
        List<Site> sitesList = sites.getSites();
        if (siteRepository.getSitesCount() != 0){
            for (Site siteYAML : sitesList) {
                searchengine.modul.Site site = siteRepository.getSiteByName(siteYAML.getName());
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());

                int pages = site.getPages().size();
                int lemmas = site.getLemmas().stream().mapToInt(Lemma::getFrequency).sum();
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(site.getSiteStatus().toString());
                item.setError(site.getLastError());
                item.setStatusTime(site.getStatusTime().toInstant(ZoneOffset.UTC).toEpochMilli());
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
                detailed.add(item);
            }
        } else {
            total.setPages(0);
            total.setLemmas(0);
            total.setSites(0);
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
