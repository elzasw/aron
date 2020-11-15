package cz.aron.transfagent.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "dao_request")
public class DaoRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dao_request_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dao_file_id", nullable = false)
    private DaoFile daoFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_file_id", nullable = true)
    private DaoFile thumbnailFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "view_file_id", nullable = true)
    private DaoFile viewFile;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DaoFile getDaoFile() {
        return daoFile;
    }

    public void setDaoFile(DaoFile daoFile) {
        this.daoFile = daoFile;
    }

    public DaoFile getThumbnailFile() {
        return thumbnailFile;
    }

    public void setThumbnailFile(DaoFile thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }

    public DaoFile getViewFile() {
        return viewFile;
    }

    public void setViewFile(DaoFile viewFile) {
        this.viewFile = viewFile;
    }

}
