package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.modul.Index;
import searchengine.modul.Page;
import searchengine.modul.Site;

import javax.transaction.Transactional;

@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteAllBySiteId(Site siteId);

    boolean existsByPathAndSiteId(String path, Site siteId);

    Page findBySiteIdAndPath(Site siteId, String path);

    @Query("SELECT p FROM Page p JOIN p.indices i WHERE i.id = :indId")
    Page findByIndexId(@Param("indId") Integer indId);

}
