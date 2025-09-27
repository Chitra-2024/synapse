package com.quantrecon.core_engine.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class DataProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ResourceLoader resourceLoader;

    public DataProducerService(KafkaTemplate<String, String> kafkaTemplate, ResourceLoader resourceLoader) {
        this.kafkaTemplate = kafkaTemplate;
        this.resourceLoader = resourceLoader;
    }

    public void sendCboeData() throws IOException {
        streamFileToKafka("cboe_exchange_feed.csv", "exchange-feed-topic");
    }

    public void sendLedgerData() throws IOException {
        streamFileToKafka("internal_ledger.csv", "ledger-feed-topic");
    }

    private void streamFileToKafka(String fileName, String topicName) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:data/" + fileName);
        InputStream inputStream = resource.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                // In a real system, you'd define a key. For now, we'll send without one.
                kafkaTemplate.send(topicName, line);
            }
        }
        System.out.println("Finished sending data from " + fileName + " to topic " + topicName);
    }
}