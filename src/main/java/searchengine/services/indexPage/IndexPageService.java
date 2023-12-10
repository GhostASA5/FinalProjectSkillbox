package searchengine.services.indexPage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.services.repository.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service
public class IndexPageService {

    private final PageRepository pageRepository;

    @Autowired
    public IndexPageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public void indexPage(String url){

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .get();

            String parse = Jsoup.parse(document.outerHtml()).text();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HashMap<String, Integer> getAllLemmas(String text){
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = arrayContainsRussianWords(text);
        List<String> allLemmas = arrayOfOnlyLemmas(words);

        return lemmas;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private ArrayList<String> arrayOfOnlyLemmas(String[] words){
        ArrayList<String> lemmas = new ArrayList<>();
        for (String word : words){
            if (word.isBlank()){
                continue;
            }

        }
        return lemmas;
    }
}
