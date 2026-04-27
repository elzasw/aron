package cz.aron.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuEntitySimple;


@Repository
public interface ApuEntitySimpleRepository extends JpaRepository<ApuEntitySimple, Long> {

    List<ApuEntitySimple> findAllByUuidIn(Collection<String> uuids);

}
