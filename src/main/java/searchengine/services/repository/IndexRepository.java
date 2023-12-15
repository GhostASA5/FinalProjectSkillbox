package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.modul.Index;
import searchengine.modul.Lemma;
import searchengine.modul.Page;

@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {

    void deleteAllByLemmaId(Lemma lemmaId);

}
