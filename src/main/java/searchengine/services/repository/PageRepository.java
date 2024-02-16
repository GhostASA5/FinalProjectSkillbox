package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.modul.Page;
import searchengine.modul.Site;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Modifying
    @Query("delete Page p where p.siteId = :siteId")
    void deleteAllBySiteId(Site siteId);

    Page findBySiteIdAndPath(Site siteId, String path);

    @Query("SELECT p FROM Page p JOIN p.indices i WHERE i.id = :indId")
    Page findByIndexId(@Param("indId") Integer indId);

    List<Page> findAllBySiteId(Site site);

}
