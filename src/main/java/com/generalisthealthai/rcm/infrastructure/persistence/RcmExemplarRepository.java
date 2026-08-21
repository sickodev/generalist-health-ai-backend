package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RcmExemplarRepository extends JpaRepository<RcmExemplar, UUID> {

    List<RcmExemplar> findByPayerId(String payerId);

    List<RcmExemplar> findByCptCode(String cptCode);

    List<RcmExemplar> findByGroundTruthDecision(ReviewDecision decision);

    @Query(value = "SELECT * FROM rcm_exemplars WHERE payer_id = :payerId AND cpt_code = :cptCode", nativeQuery = true)
    List<RcmExemplar> findByPayerIdAndCptCode(@Param("payerId") String payerId, @Param("cptCode") String cptCode);

    /**
     * Native pgvector cosine distance nearest neighbors query.
     */
    @Query(value = "SELECT * FROM rcm_exemplars ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :k", nativeQuery = true)
    List<RcmExemplar> findNearestNeighborsNative(@Param("vector") String vector, @Param("k") int k);
}
