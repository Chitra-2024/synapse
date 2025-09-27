package com.quantrecon.api_service.controller;

import com.quantrecon.api_service.model.ReconciledStrategy;
import com.quantrecon.api_service.repository.ReconciledStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ResultsController {

    private final ReconciledStrategyRepository repository;

    public ResultsController(ReconciledStrategyRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/results")
    public List<ReconciledStrategy> getResults() {
        return repository.findAll();
    }
}