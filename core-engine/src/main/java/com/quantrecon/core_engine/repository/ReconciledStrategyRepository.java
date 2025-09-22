package com.quantrecon.core_engine.repository;

import com.quantrecon.core_engine.model.ReconciledStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciledStrategyRepository extends JpaRepository<ReconciledStrategy, Integer> {
    // Spring Data JPA automatically creates all the database methods for us!
}
