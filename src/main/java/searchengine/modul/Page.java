package searchengine.modul;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page", indexes = {
        @Index(name = "ind_id", columnList = "id"),
        @Index(name = "ind_site", columnList = "site_id")
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageId")
    private List<searchengine.modul.Index> indices = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Site getSiteId() {
        return siteId;
    }

    public void setSiteId(Site siteId) {
        this.siteId = siteId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<searchengine.modul.Index> getIndices() {
        return indices;
    }

    public void setIndices(List<searchengine.modul.Index> indices) {
        this.indices = indices;
    }
}
