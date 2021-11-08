package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;

@Repository
public interface ApuSourceRepository extends JpaRepository<ApuSource, Integer> {
    
    Optional<ApuSource> findByUuid(UUID uuid);

    List<ApuSource> findFirst1000ByReimport(boolean reimport);

    @Query(nativeQuery = true, value = "UPDATE apu_source AS a "
            + "SET a.reimport = true FROM archival_entity ae "
            + "WHERE ae.apusource_id = a.apusource_id AND ae.entity_id in :ids")
    void setReimportTrueByIds(@Param("ids") List<Integer> ids); 
}
