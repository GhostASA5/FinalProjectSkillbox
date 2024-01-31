package searchengine.services.search;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.modul.Index;
import searchengine.modul.Lemma;
import searchengine.modul.Page;
import searchengine.modul.Site;
import searchengine.services.repository.IndexRepository;
import searchengine.services.repository.LemmaRepository;
import searchengine.services.repository.PageRepository;
import searchengine.services.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final String[] particles = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    //Поиск запроса по всем сайтам
    @Override
    public SearchResponse search(String query, Integer offset, Integer limit) {
        SearchResponse response = new SearchResponse();

        HashMap<String, Integer> lemmasMap = getAllLemmas(query, null);
        HashMap<Page, Float> pages = getLemmasPages(lemmasMap);
        List<SearchData> data = pages.isEmpty() ? new ArrayList<>() : sortPagesByRelevant(pages, query);

        response.setResult(true);
        if (!data.isEmpty()){
            response.setCount(data.size());
            response.setData(data);
        } else {
            response.setCount(0);
            response.setData(data);
        }
        return response;
    }

    //Поиск запроса по одному сайту
    @Override
    public SearchResponse searchByOneSite(String query, Integer offset, Integer limit, String site) {
        SearchResponse response = new SearchResponse();

        HashMap<String, Integer> lemmasMap = getAllLemmas(query, site);
        HashMap<Page, Float> pages = getLemmasPagesByOneSite(lemmasMap, site);
        List<SearchData> data = pages.isEmpty() ? new ArrayList<>() : sortPagesByRelevant(pages, query);

        response.setResult(true);
        if (!data.isEmpty()){
            response.setCount(data.size());
            response.setData(data);
        } else {
            response.setCount(0);
            response.setData(data);
        }
        return response;
    }

    private HashMap<Page, Float> getLemmasPages(HashMap<String, Integer> lemmasMap){
        HashMap<Page, Float> pages = new HashMap<>();
        int c = 0;
        //Находим страницы по леммам
        for (String lemma : lemmasMap.keySet()){
            List<Lemma> lemmasId = lemmaRepository.findAllLemmasByLemma(lemma);
            for (Lemma lemmaId : lemmasId){
                List<Index> indices = indexRepository.findAllByLemmaId(lemmaId);
                if (c < lemmasId.size()){
                    for (Index index : indices) {
                        pages.put(pageRepository.findByIndexId(index.getId()), index.getLemmaRank());
                    }
                } else {
                    HashMap<Page, Float> thisLemmaPages = new HashMap<>();
                    indices.forEach(index -> {
                        Page page = pageRepository.findByIndexId(index.getId());
                        thisLemmaPages.put(page, index.getLemmaRank());
                    });
                    pages = removeDifferentKeys(pages, thisLemmaPages);
                }
                c++;
            }
            c = Integer.MAX_VALUE;
        }
        return pages;
    }

    private HashMap<Page, Float> getLemmasPagesByOneSite(HashMap<String, Integer> lemmasMap, String siteUrl){
        int c = 0;
        HashMap<Page, Float> pages = new HashMap<>();
        Site site = siteRepository.getSiteByUrl(siteUrl);
        //Находим страницы по леммам
        for (String lemma : lemmasMap.keySet()){
            Lemma lemmaId = lemmaRepository.findByLemmaAndSiteId(lemma, site);
            List<Index> indices = indexRepository.findAllByLemmaId(lemmaId);
            if (c == 0){
                for (Index index : indices) {
                    pages.put(pageRepository.findByIndexId(index.getId()), index.getLemmaRank());
                }
            } else {
                HashMap<Page, Float> thisLemmaPages = new HashMap<>();
                indices.forEach(index -> {
                    Page page = pageRepository.findByIndexId(index.getId());
                    thisLemmaPages.put(page, index.getLemmaRank());
                });
                pages = removeDifferentKeys(pages, thisLemmaPages);
            }
            c++;
        }
        return pages;
    }

    private HashMap<Page, Float> removeDifferentKeys(HashMap<Page, Float> map1, HashMap<Page, Float> map2) {
        HashMap<Page, Float> result = new HashMap<>();
        for (HashMap.Entry<Page, Float> entry : map1.entrySet()) {
            Page key = entry.getKey();
            Float value = entry.getValue();

            if (map2.containsKey(key)) {
                value += map2.get(key);
            }

            result.put(key, value);
        }
        map1 = result;

        // Создаем временное множество, чтобы сохранить общие ключи
        Set<Page> commonKeys = new HashSet<>(map1.keySet());
        commonKeys.retainAll(map2.keySet());

        // Оставляем только общие ключи в исходных HashMap
        map1.keySet().retainAll(commonKeys);
        map2.keySet().retainAll(commonKeys);

        return map1;
    }

    private List<SearchData> sortPagesByRelevant(HashMap<Page, Float> pages, String query){
        List<SearchData> searchData = new ArrayList<>();
        float maxValue = (float) pages.values().stream()
                .mapToDouble(Double::valueOf)
                .max()
                .orElse(Double.MIN_VALUE);
        for (HashMap.Entry<Page, Float> entry : pages.entrySet()){
            Page page = entry.getKey();
            Float rel = entry.getValue() / maxValue;
            pages.put(page, rel);
        }
        pages = pages.entrySet()
                .stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        for (HashMap.Entry<Page, Float> entry : pages.entrySet()) {
            Page page = entry.getKey();
            Float rel = entry.getValue();
            String title = null, snippet = null;
            try {
                String html = page.getContent();
                Document doc = Jsoup.parse(html);
                title = doc.title();
                Elements elements = doc.getElementsContainingOwnText(query);
                snippet = findSnippet(elements, query);
            } catch (Exception ignored) {
            }
            if (snippet == null){
                continue;
            }
            SearchData data = new SearchData();
            data.setTitle(title);
            data.setSnippet(snippet);
            data.setSite(page.getSiteId().getUrl());
            data.setSiteName(page.getSiteId().getName());
            data.setUri(page.getPath());
            data.setRelevance(rel);
            searchData.add(data);
        }
        return searchData;
    }

    private String findSnippet(Elements elements, String query){
        for (Element element : elements){
            String[] words = element.text().toLowerCase().split("\\s+");
            String[] targets = query.toLowerCase().split("\\s+");
            int target = query.split("\\s+").length;
            int index = 0;
            query = query.toLowerCase();

            for (String word : words) {
                if (word.equalsIgnoreCase(targets[0])){
                    index = Arrays.asList(words).indexOf(word);
                    break;
                }
            }

            int start = Math.max(0, index - 5);
            int end = Math.min(words.length - 1, index + 5);
            String[] newWorld = new String[target];
            System.arraycopy(words, index, newWorld, 0, target);
            if (Arrays.equals(newWorld, targets)){
                //return element.text().toLowerCase().replace(query, "<b>" + query + "</b>");
                return String.join(" ", Arrays.copyOfRange(words, start, end + 1)).toLowerCase()
                        .replace(query, "<b>" + query + "</b>");
            }
        }
        return null;
    }

    private HashMap<String, Integer> getAllLemmas(String text, String site){
        String[] words = arrayContainsRussianWords(text);
        return mapOfOnlyLemmas(words, site);
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private HashMap<String, Integer> mapOfOnlyLemmas(String[] words, String site){
        HashMap<String, Integer> lemmas = new HashMap<>();
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

            if (!lemmas.containsKey(normalWord)) {
                try {
                    if (site != null){
                        int frequency = lemmaRepository.findByLemmaAndSiteId(normalWord,
                                siteRepository.getSiteByUrl(site)).getFrequency();
                        lemmas.put(normalWord, frequency);
                    } else {
                        List<Lemma> lemmaList = lemmaRepository.findAllLemmasByLemma(word);
                        int frequency = lemmaList.stream().mapToInt(Lemma::getFrequency).sum();
                        lemmas.put(normalWord, frequency);
                    }
                } catch (Exception ignored){
                }
            }
        }

        return lemmas.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new
                ));
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