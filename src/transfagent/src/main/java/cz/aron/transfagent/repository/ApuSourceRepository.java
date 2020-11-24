package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;

@Repository
public interface ApuSourceRepository extends JpaRepository<ApuSource, Integer> {

}
