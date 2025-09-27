package com.quantrecon.core_engine.controller;

import com.quantrecon.core_engine.service.DataProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReconciliationController {

    private final DataProducerService dataProducerService;

    public ReconciliationController(DataProducerService dataProducerService) {
        this.dataProducerService = dataProducerService;
    }

    // This controller's only job is to start the data feed.
    @PostMapping("/start-producers")
    public ResponseEntity<String> startProducers() {
        try {
            dataProducerService.sendCboeData();
            dataProducerService.sendLedgerData();
            return ResponseEntity.ok("Started data producers successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error starting producers: " + e.getMessage());
        }
    }
}