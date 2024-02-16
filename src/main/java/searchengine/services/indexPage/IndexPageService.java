package searchengine.services.indexPage;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexpage.IndexPageResponse;
import searchengine.modul.Index;
import searchengine.modul.Lemma;
import searchengine.modul.Page;
import searchengine.services.repository.IndexRepository;
import searchengine.services.repository.LemmaRepository;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;
import searchengine.services.startandstopInd.StartAndStopIndexingService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class IndexPageService implements IndexService{

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private String siteUrl;
    private String mainSiteUrl;
    private final SitesList sites;
    private final String[] particles = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();
    int numberOfThreads = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);


    @Autowired
    public IndexPageService(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, SiteRepository siteRepository, SitesList sites) throws IOException {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        this.sites = sites;
    }

    @Override
    public IndexPageResponse indexPage(String url){
        IndexPageResponse response = new IndexPageResponse();
        findMainSite(url);
        if (mainSiteUrl == null){
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        } else {
            response.setResult(true);
            searchengine.modul.Site siteId = siteRepository.getSiteByUrl(mainSiteUrl);
            Page page = pageRepository.findBySiteIdAndPath(siteId, siteUrl);
            if (page != null) {
                List<Lemma> lemmaList = lemmaRepository.findByPagePath(page.getPath(), siteId);
                lemmaList.forEach(indexRepository::deleteByLemmaId);
                lemmaRepository.deleteAll(lemmaList);
                pageRepository.delete(page);
            }
            updateWithNewPage(url, siteId);
        }
        return response;
    }

    private void findMainSite(String url){
        for(Site site : sites.getSites()){
            if (url.contains(site.getUrl())){
                siteUrl = url.replaceAll(site.getUrl(), "");
                mainSiteUrl = site.getUrl();
                if(siteUrl.isEmpty()){
                    siteUrl = "/";
                }
            }
        }
    }

    private void updateWithNewPage(String url, searchengine.modul.Site site){
        Page page = new Page();
        page.setSiteId(site);
        page.setPath(siteUrl);
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true).get();
            page.setContent(document.outerHtml());
            page.setCode(Jsoup.connect(url).execute().statusCode());
            pageRepository.save(page);
            HashMap<String, Float> lemmasMap = getAllLemmas(page.getContent());
            saveToDB(lemmasMap, page, site);
        } catch (HttpStatusException ex){
            page.setCode(ex.getStatusCode());
            page.setContent("");
            pageRepository.save(page);
        } catch (Exception ignored){
        }
    }

    private void saveToDB(HashMap<String, Float> lemmasMap, Page page, searchengine.modul.Site site){
        for (String lemma : lemmasMap.keySet()){
            Lemma lemmaDB = lemmaRepository.findByLemmaAndSiteId(lemma, page.getSiteId());
            if (lemmaDB == null){
                lemmaDB = new Lemma();
                lemmaDB.setLemma(lemma);
                lemmaDB.setSiteId(site);
            }
            lemmaDB.incrementFrequency();

            Index index = new Index();
            index.setPageId(page);
            index.setLemmaId(lemmaDB);
            index.setLemmaRank(lemmasMap.get(lemma));
            lemmaRepository.save(lemmaDB);
            indexRepository.save(index);
        }
    }

    @Override
    public void indexSite(searchengine.modul.Site site, ConcurrentHashMap<String, Lemma> concurrentHashMap){
        List<Page> pages = pageRepository.findAllBySiteId(site);

        for(Page page : pages){
            if (StartAndStopIndexingService.isActive()){
                String htmlPageCode = page.getContent();
                HashMap<String, Float> lemmasMap = getAllLemmas(htmlPageCode);
                List<Index> indices = new ArrayList<>();
                executorService.submit(() -> updateLemmas(lemmasMap, concurrentHashMap, site, indices, page));
                lemmaRepository.saveAll(concurrentHashMap.values());
                indexRepository.saveAll(indices);
            } else {
                break;
            }
        }
    }

    private void updateLemmas(HashMap<String, Float> lemmasMap, ConcurrentHashMap<String, Lemma> concurrentHashMap,
                              searchengine.modul.Site site, List<Index> indices, Page page){
        for (String lemma : lemmasMap.keySet()){
            Lemma lemmaDB;
            if (!concurrentHashMap.containsKey(lemma)){
                lemmaDB = new Lemma();
                lemmaDB.setLemma(lemma);
                lemmaDB.setSiteId(site);
            } else {
                lemmaDB = concurrentHashMap.get(lemma);
            }
            lemmaDB.incrementFrequency();
            concurrentHashMap.put(lemma, lemmaDB);

            Index index = new Index();
            index.setPageId(page);
            index.setLemmaId(lemmaDB);
            index.setLemmaRank(lemmasMap.get(lemma));
            indices.add(index);
        }
    }

    private HashMap<String, Float> getAllLemmas(String text){
        String[] words = arrayContainsRussianWords(text);
        return mapOfOnlyLemmas(words);
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private HashMap<String, Float> mapOfOnlyLemmas(String[] words){
        HashMap<String, Float> lemmas = new HashMap<>();
        for (String word : words){
            if (word.isBlank()){
                continue;
            }

            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)){
                continue;
            }

            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1F);
            }

        }
        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particles) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
