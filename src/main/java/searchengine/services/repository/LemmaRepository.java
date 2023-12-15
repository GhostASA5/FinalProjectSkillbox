package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.modul.Lemma;
import searchengine.modul.Site;

@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    void deleteAllBySiteId(Site siteId);

    Lemma findByLemmaAndSiteId(String lemma, Site siteId);

    Lemma findBySiteId(Site siteId);
}
