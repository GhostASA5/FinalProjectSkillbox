package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.modul.Lemma;
import searchengine.modul.Site;

import javax.persistence.LockModeType;
import java.util.List;

@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    void deleteAllBySiteId(Site siteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Lemma findByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") Site siteId);

    @Query("SELECT l FROM Lemma l WHERE l.lemma = :lemma")
    List<Lemma> findAllLemmasByLemma(@Param("lemma") String lemma);


}
