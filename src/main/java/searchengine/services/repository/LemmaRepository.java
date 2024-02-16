package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.modul.Lemma;
import searchengine.modul.Site;

import java.util.List;

@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Query("delete Lemma l where l.siteId = :siteId")
    void deleteAllBySiteId(Site siteId);

    Lemma findByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") Site siteId);

    @Query("SELECT l FROM Lemma l WHERE l.lemma = :lemma")
    List<Lemma> findAllLemmasByLemma(@Param("lemma") String lemma);

    @Query("SELECT l FROM Lemma l JOIN l.indices i JOIN i.pageId p WHERE p.path = :pagePath and p.siteId = :siteId")
    List<Lemma> findByPagePath(@Param("pagePath") String pagePath, @Param("siteId") Site site);

}
