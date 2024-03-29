package searchengine.services.startandstopInd;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.modul.Lemma;
import searchengine.modul.Page;
import searchengine.modul.Site;
import searchengine.modul.SiteStatus;
import searchengine.services.indexPage.IndexPageService;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;


public class ListUrl extends RecursiveAction {

    private final Site site;
    private final Site mainSite;
    public static Set<String> concurrentSet = ConcurrentHashMap.newKeySet();
    private static final String CSS_QUERY = "a[href]";
    private static final String ATTRIBUTE_KEY = "href";
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexPageService indexPageService;
    private final ConcurrentHashMap<String, Lemma> concurrentHashMap;
    private final List<Page> pages;

    public ListUrl(Site site, Site mainSite, PageRepository pageRepository,
                   SiteRepository siteRepository, IndexPageService indexPageService, ConcurrentHashMap<String, Lemma> concurrentHashMap, List<Page> pages) {
        this.site = site;
        this.mainSite = mainSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.indexPageService = indexPageService;
        this.concurrentHashMap = concurrentHashMap;
        this.pages = pages;
    }

    @Override
    protected void compute() {
        String main = mainSite.getUrl();
        String url = site.getUrl();
        List<ListUrl> setUrlList = new ArrayList<>();
        if (!StartAndStopIndexingService.isActive()) {
            mainSite.setSiteStatus(SiteStatus.FAILED);
            mainSite.setLastError("Индексация остановлена пользователем");
            mainSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(mainSite);
        } else if (mainSite.getSiteStatus().equals(SiteStatus.FAILED)){

        } else {
            String urlPart = url.replace(main, "");
            Elements elements;
            Document document;
            Page page = new Page();
            page.setPath(urlPart);
            page.setSiteId(mainSite);
            try {
                Thread.sleep(1500);
                Connection connection = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("https://www.google.com")
                        .ignoreContentType(true);

                document = connection.get();
                if (!urlPart.isEmpty()){
                    page.setCode(Jsoup.connect(url).execute().statusCode());
                    page.setContent(document.outerHtml());
                    pageRepository.save(page);
                    mainSite.setStatusTime(LocalDateTime.now());
                    siteRepository.save(mainSite);
                    pages.add(page);
                }

                elements = document.select(CSS_QUERY);
                for (Element element : elements) {
                    String attributeUrl = element.absUrl(ATTRIBUTE_KEY);
                    Site newSite = new Site();
                    newSite.setUrl(attributeUrl);

                    if (checkUrl(attributeUrl) && !concurrentSet.contains(attributeUrl)) {
                        ListUrl setUrl = new ListUrl(newSite, mainSite, pageRepository, siteRepository, indexPageService, concurrentHashMap, pages);
                        setUrl.fork();
                        setUrlList.add(setUrl);
                        concurrentSet.add(attributeUrl);

                    }
                }
                for (ListUrl link : setUrlList) {
                    link.join();
                }

            } catch (HttpStatusException ex) {
                httpStatusException(ex, page);
            } catch (MalformedURLException | UnsupportedMimeTypeException ex){
                malformedURLException(page);
            } catch (Exception ex){
                simpleException(ex);
            }
        }
    }

    private void simpleException(Exception ex){
        String error = ex.getMessage();
        ex.printStackTrace();
        if(error.equals("Read timed out")){
            mainSite.setLastError("Индексация остановлена из-за долгого ответа сервера. Проверьте интернет соединение");
        } else {
            mainSite.setLastError(error);
        }
        mainSite.setSiteStatus(SiteStatus.FAILED);
        mainSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(mainSite);
    }

    private void malformedURLException(Page page){
        page.setCode(404);
        page.setContent("");
        pageRepository.save(page);
        mainSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(mainSite);
    }

    private void httpStatusException(HttpStatusException ex, Page page){
        int statusCode = ex.getStatusCode();
        String responseBody = ex.getMessage();
        page.setCode(statusCode);
        page.setContent(Jsoup.parse(responseBody).outerHtml());
        pageRepository.save(page);
        mainSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(mainSite);
    }

    private boolean checkUrl(String url) {
        try {
            new URL(url);
            return !url.isEmpty() && url.startsWith(site.getUrl()) && !url.contains("#");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}

