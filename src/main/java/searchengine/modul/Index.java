package searchengine.modul;

import javax.persistence.*;

@Entity
@Table(name = "search_index")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private Page pageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemmaId;

    @Column(name = "lemma_rank", columnDefinition = "FLOAT", nullable = false)
    private Float lemmaRank;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Page getPageId() {
        return pageId;
    }

    public void setPageId(Page pageId) {
        this.pageId = pageId;
    }

    public Lemma getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(Lemma lemmaId) {
        this.lemmaId = lemmaId;
    }

    public Float getLemmaRank() {
        return lemmaRank;
    }

    public void setLemmaRank(Float lemmaRank) {
        this.lemmaRank = lemmaRank;
    }
}
