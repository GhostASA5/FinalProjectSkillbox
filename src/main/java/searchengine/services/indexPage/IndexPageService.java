package searchengine.services.indexPage;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
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

import java.io.IOException;
import java.util.*;

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
            String htmlPageCode = pageRepository.findBySiteIdAndPath(siteId, siteUrl).getContent();
            HashMap<String, Float> lemmasMap = getAllLemmas(htmlPageCode);
            saveToDB(lemmasMap, pageRepository.findBySiteIdAndPath(siteId, siteUrl));
        }
        return response;
    }

    @Override
    public void indexPage(String url, Page page) {
        findMainSite(url);
        String htmlPageCode = page.getContent();
        HashMap<String, Float> lemmasMap = getAllLemmas(htmlPageCode);
        saveToDB(lemmasMap, page);
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

    private void saveToDB(HashMap<String, Float> lemmasMap, Page page){
        for(String lemma : lemmasMap.keySet()){
            searchengine.modul.Site site = siteRepository.getSiteByUrl(mainSiteUrl);
            Lemma lemmaDB = lemmaRepository.findByLemmaAndSiteId(lemma, site);

            if (lemmaDB == null){
                lemmaDB = new Lemma();
                lemmaDB.setLemma(lemma);
                lemmaDB.setSiteId(site);
            }
            lemmaDB.incrementFrequency();
            lemmaRepository.save(lemmaDB);
            Index index = new Index();
            index.setPageId(page);
            index.setLemmaId(lemmaDB);
            index.setLemmaRank(lemmasMap.get(lemma));
            indexRepository.save(index);
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
