package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.Property;

public interface PropertyRepository extends JpaRepository<Property, Integer> {

    Property findByName(String name);

}
