package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.modul.Site;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Site getSiteByName(String name);

    Site getSiteByUrl(String url);

    Site getById(Integer id);

    @Query("select count(*) from Site ")
    Integer getSitesCount();

}
