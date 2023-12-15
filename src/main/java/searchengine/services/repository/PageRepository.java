package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.modul.Page;
import searchengine.modul.Site;

import javax.transaction.Transactional;

@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteAllBySiteId(Site siteId);

    boolean existsByPathAndSiteId(String path, Site siteId);

    Page findBySiteIdAndPath(Site siteId, String path);
}
