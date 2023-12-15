package searchengine.services.startandstopInd;

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
import searchengine.modul.Page;
import searchengine.modul.Site;
import searchengine.modul.SiteStatus;
import searchengine.services.indexPage.IndexPageService;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;


public class ListUrl extends RecursiveTask<TreeSet<String>> {

    private final Site site;
    private final Site mainSite;
    public static TreeSet<String> URL_SET = new TreeSet<>();
    private static final String CSS_QUERY = "a[href]";
    private static final String ATTRIBUTE_KEY = "href";
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexPageService indexPageService;


    public ListUrl(Site site, Site mainSite, PageRepository pageRepository, SiteRepository siteRepository, IndexPageService indexPageService) {
        this.site = site;
        this.mainSite = mainSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.indexPageService = indexPageService;
    }

    @Override
    protected TreeSet<String> compute() {
        TreeSet<String> urlSet = new TreeSet<>();
        String main = mainSite.getUrl();
        String url = site.getUrl();
        List<ListUrl> setUrlList = new ArrayList<>();
        if (!StartAndStopIndexing.isActive()) {
            mainSite.setSiteStatus(SiteStatus.FAILED);
            mainSite.setLastError("Индексация остановлена пользователем");
            mainSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(mainSite);
        } else {
            String urlPart = url.replace(main, "");
            urlSet.add(urlPart);
            Elements elements;
            Document document;
            boolean pathExists = pageRepository.existsByPathAndSiteId(urlPart, mainSite); // Проверяем наличие пути в базе данных
            if (!pathExists){
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
                        //System.out.println(url);
                        //indexPageService.indexPage(url);
                    }

                    elements = document.select(CSS_QUERY);
                    for (Element element : elements) {
                        String attributeUrl = element.absUrl(ATTRIBUTE_KEY);
                        Site site1 = new Site();
                        site1.setUrl(attributeUrl);

                        if (!attributeUrl.isEmpty() && attributeUrl.startsWith(url)
                                && !pageRepository.existsByPathAndSiteId(attributeUrl.replace(main, ""), mainSite)
                                && !URL_SET.contains(attributeUrl)
                                && !attributeUrl.contains("#")) {
                            ListUrl setUrl = new ListUrl(site1, mainSite, pageRepository, siteRepository, indexPageService);
                            setUrl.fork();
                            setUrlList.add(setUrl);
                            URL_SET.add(attributeUrl);
                        }
                    }
                    for (ListUrl link : setUrlList) {
                        TreeSet<String> linkResult = link.join();
                        linkResult.remove(main.trim());
                        urlSet.addAll(linkResult);
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
                } catch (Exception ex){
                    Thread.currentThread().interrupt();
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
            }
        }
        return urlSet;
    }
}

