package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.IdProjection;

public interface DaoRepository extends JpaRepository<Dao, Integer> {

    @Query("select d from Dao d join fetch d.apuSource a " +
            "where not exists(select q from CoreQueue q where q.apuSource = a) " +
            "and d.state = ?1 and d.transferred = ?2 and a.reimport is false " +
            "order by d.id")
    List<IdProjection> findTopByStateAndTransferredOrderById(DaoState state, boolean transferred, Pageable limit);

    @EntityGraph(attributePaths = { "apuSource" })
    Optional<Dao> findById(int id);

    @EntityGraph(attributePaths = { "apuSource" })
    Optional<Dao> findByUuid(UUID uuid);

    Optional<Dao> findByHandle(String handle);

    @EntityGraph(attributePaths = { "apuSource" })
    List<Dao> findAllByUuidIn(List<UUID> uuids);

    List<Dao> findByApuSource(ApuSource apuSource);

    List<Dao> findTop1000ByStateOrderById(DaoState state);

}
