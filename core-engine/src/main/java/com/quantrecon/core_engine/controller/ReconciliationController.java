package com.quantrecon.core_engine.controller;

import com.quantrecon.core_engine.model.ReconciledStrategy;
import com.quantrecon.core_engine.repository.ReconciledStrategyRepository;
import com.quantrecon.core_engine.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final ReconciledStrategyRepository repository;

    @Autowired
    public ReconciliationController(ReconciliationService reconciliationService, ReconciledStrategyRepository repository) {
        this.reconciliationService = reconciliationService;
        this.repository = repository;
    }

    @PostMapping("/reconcile")
    public ResponseEntity<String> startReconciliation() {
        try {
            reconciliationService.runReconciliation();
            return ResponseEntity.ok("Reconciliation process completed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during reconciliation: " + e.getMessage());
        }
    }

    @GetMapping("/results")
    public List<ReconciledStrategy> getResults() {
        return repository.findAll();
    }
}