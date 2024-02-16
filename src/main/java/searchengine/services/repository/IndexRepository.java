package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.modul.Index;
import searchengine.modul.Lemma;
import searchengine.modul.Page;

import java.util.List;

@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Modifying
    @Query("delete from Index i")
    void deleteAll();

    void deleteByLemmaId(Lemma lemma);

    List<Index> findAllByLemmaId(Lemma lemmaId);

}
