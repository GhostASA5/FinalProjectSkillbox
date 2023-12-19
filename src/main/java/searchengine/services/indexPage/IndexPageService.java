package searchengine.services.indexPage;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteYAML;
import searchengine.config.SitesList;
import searchengine.modul.Index;
import searchengine.modul.Lemma;
import searchengine.services.repository.IndexRepository;
import searchengine.services.repository.LemmaRepository;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class IndexPageService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private String siteUrl;
    private String mainSiteUrl;
    private final SitesList sites;
    private final String[] particles = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> lemmasConcurrentMap = new ConcurrentHashMap<>();

    @Autowired
    public IndexPageService(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, SiteRepository siteRepository, SitesList sites) throws IOException {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        this.sites = sites;
    }

    public void indexPage(String url){
        for(SiteYAML siteYAML : sites.getSites()){
            if (url.contains(siteYAML.getUrl())){
                siteUrl = url.replaceAll(siteYAML.getUrl(), "");
                mainSiteUrl = siteYAML.getUrl();
                if(siteUrl.isEmpty()){
                    siteUrl = "/";
                }
            }
        }
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .get();

            String parse = Jsoup.parse(document.outerHtml()).text();
            HashMap<String, Float> lemmasMap = getAllLemmas(parse);
            saveToDB(lemmasMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void saveToDB(HashMap<String, Float> lemmasMap){
        for(String lemma : lemmasMap.keySet()){
            Integer siteId = siteRepository.getSiteByUrl(mainSiteUrl).getId();
            Lemma oldLemma = lemmaRepository.findByLemmaAndSiteId(lemma, siteRepository.getSiteByUrl(mainSiteUrl));
            Index index = new Index();
            if (!containsLemmaInSite(lemma, siteId)){
                Lemma newLemma = new Lemma();
                newLemma.setLemma(lemma);
                newLemma.setFrequency(1);
                newLemma.setSiteId(siteRepository.getSiteByUrl(mainSiteUrl));
                oldLemma = newLemma;
                lemmaRepository.save(oldLemma);
                CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
                list.add(siteId);
                lemmasConcurrentMap.put(lemma, list);
            } else {
                oldLemma.setFrequency(oldLemma.getFrequency() + 1);
                lemmaRepository.save(oldLemma);
                CopyOnWriteArrayList<Integer> lemmasList = lemmasConcurrentMap.get(lemma);
                lemmasList.add(siteId);
                lemmasConcurrentMap.put(lemma, lemmasList);
            }

            index.setPageId(pageRepository.findBySiteIdAndPath(siteRepository.getSiteByUrl(mainSiteUrl), siteUrl));
            index.setLemmaId(oldLemma);
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

    private boolean containsLemmaInSite(String lemma, Integer siteId) {
        CopyOnWriteArrayList<Integer> sitesForWord = lemmasConcurrentMap.get(lemma);
        boolean a;
        if (sitesForWord!=null){
            a = sitesForWord.contains(siteId);
        }
        return sitesForWord != null && sitesForWord.contains(siteId);
    }
}
