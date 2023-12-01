package searchengine.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.modul.Page;
import searchengine.modul.Site;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

@Service
public class ListUrl extends RecursiveTask<TreeSet<String>> {

    private final Site site;
    private final Site mainSite;
    private static final TreeSet<String> URL_SET = new TreeSet<>();
    private static final String CSS_QUERY = "a[href]";
    private static final String ATTRIBUTE_KEY = "href";
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public ListUrl(Site site, Site mainSite, PageRepository pageRepository, SiteRepository siteRepository) {
        this.site = site;
        this.mainSite = mainSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    protected TreeSet<String> compute() {
        TreeSet<String> urlSet = new TreeSet<>();
        String main = mainSite.getUrl();
        String url = site.getUrl();
        List<ListUrl> setUrlList = new ArrayList<>();
        try {
            String urlPart = url.replace(main, "");
            urlSet.add(urlPart);

            Elements elements;
            Document document;
            Thread.sleep(2500);
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true);

            Page page = new Page();
            page.setPath(urlPart);
            page.setSiteId(mainSite);

            try {
                document = connection.get();
                if (!urlPart.isEmpty()){
                    page.setCode(Jsoup.connect(url).execute().statusCode());
                    page.setContent(document.outerHtml());
                    pageRepository.save(page);
                    mainSite.setStatusTime(LocalDateTime.now());
                    siteRepository.save(mainSite);
                }

                elements = document.select(CSS_QUERY);
                for (Element element : elements) {
                    String attributeUrl = element.absUrl(ATTRIBUTE_KEY);
                    Site site1 = new Site();
                    site1.setUrl(attributeUrl);
                    if (!attributeUrl.isEmpty() && attributeUrl.startsWith(url) && !URL_SET.contains(attributeUrl)
                            && !attributeUrl.contains("#")) {
                        ListUrl setUrl = new ListUrl(site1, mainSite, pageRepository, siteRepository);
                        setUrl.fork();
                        setUrlList.add(setUrl);
                        URL_SET.add(attributeUrl);
                    }
                }
            } catch (HttpStatusException ex) {
                int statusCode = ex.getStatusCode();
                String responseBody = ex.getMessage();
                document = Jsoup.parse(responseBody);
                page.setCode(statusCode);
                page.setContent(document.outerHtml());
                pageRepository.save(page);
                mainSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(mainSite);
                return urlSet;
            }


        } catch (Exception ex){
            ex.printStackTrace();
            Thread.currentThread().interrupt();
        }

        for (ListUrl link : setUrlList) {
            TreeSet<String> linkResult = link.join();
            linkResult.remove(main.trim());
            urlSet.addAll(linkResult);
        }

        return urlSet;
    }
}

