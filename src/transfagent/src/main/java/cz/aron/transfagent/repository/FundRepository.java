package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.Fund;

public interface FundRepository extends JpaRepository<Fund, Integer> {

    @EntityGraph(attributePaths = { "apuSource" })
    Fund findByCode(String code);

}
