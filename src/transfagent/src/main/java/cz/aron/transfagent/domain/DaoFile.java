package cz.aron.transfagent.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "dao_file")
public class DaoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dao_file_id")
    private Integer id;

    @Column(nullable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apusource_id", nullable = false)
    private ApuSource apuSource;

    @Column(length = 250, nullable = false)
    private String path;

    @Column(nullable = false)
    private boolean transferred;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private DaoState state;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ApuSource getApuSource() {
        return apuSource;
    }

    public void setApuSource(ApuSource apuSource) {
        this.apuSource = apuSource;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isTransferred() {
        return transferred;
    }

    public void setTransferred(boolean transferred) {
        this.transferred = transferred;
    }

	public DaoState getState() {
		return state;
	}

	public void setState(DaoState state) {
		this.state = state;
	}

}
