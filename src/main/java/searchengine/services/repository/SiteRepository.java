package searchengine.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.modul.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Site getSiteByName(String name);

}
