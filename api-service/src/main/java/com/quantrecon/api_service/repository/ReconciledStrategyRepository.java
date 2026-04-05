package com.quantrecon.api_service.repository;

import com.quantrecon.api_service.model.ReconciledStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ReconciledStrategyRepository extends JpaRepository<ReconciledStrategy, Integer> {
    
}
