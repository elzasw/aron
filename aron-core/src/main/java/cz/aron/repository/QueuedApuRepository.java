package cz.aron.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.aron.domain.QueuedApu;

@Repository
public interface QueuedApuRepository extends JpaRepository<QueuedApu, Long>  {
	
	// TODO return only projection to be readonly
	List<QueuedApu> findTop1000ByRequestSentIsFalse();
	
	QueuedApu findByApuId(String apuId);

    @Query("DELETE FROM QueuedApu qa WHERE qa.apuId=?1")
    @Modifying
    int removeForApuId(String apuId);
	
}
