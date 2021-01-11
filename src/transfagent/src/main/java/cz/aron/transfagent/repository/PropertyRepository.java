package cz.aron.transfagent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.Property;

public interface PropertyRepository extends JpaRepository<Property, Integer> {

    Property findFirstByOrderByIdDesc();

}
