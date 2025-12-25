package com.eligibility.engine.service;

import com.eligibility.engine.model.AttributeDef;
import com.eligibility.engine.repository.CachedMetadata;
import com.eligibility.engine.repository.MetadataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetadataWarmupService {

    private final MetadataRepository repo;
    private final MockExternalDiscoveryService api;
    private final SchemaServiceImpl schemaService;

    private final ObjectMapper mapper = new ObjectMapper();

    public MetadataWarmupService(MetadataRepository repo,
                                 MockExternalDiscoveryService api,
                                 SchemaServiceImpl schemaService) {
        this.repo = repo;
        this.api = api;
        this.schemaService = schemaService;
    }

    @PostConstruct
    public void init() {
        System.out.println("--- [WARMUP] Checking Persistent Cache... ---");
        try {
            long now = System.currentTimeMillis();

            Map<String, AttributeDef> schemaMap = null;
            var cachedSchema = repo.findById("SCHEMA");

            if (cachedSchema.isPresent() && cachedSchema.get().json != null) {
                schemaMap = mapper.readValue(cachedSchema.get().json, new TypeReference<Map<String, AttributeDef>>() {});
            }

            if (schemaMap == null) {
                System.out.println("--- [WARMUP] Fetching Schema API... ---");
                schemaMap = api.fetchSchemaFromRemote();
                repo.save(new CachedMetadata("SCHEMA", mapper.writeValueAsString(schemaMap), now));
            }


            Set<String> listSet = null;
            var cachedLists = repo.findById("LISTS");

            if (cachedLists.isPresent() && cachedLists.get().json != null) {
                listSet = mapper.readValue(cachedLists.get().json, new TypeReference<Set<String>>() {});
            }

            if (listSet == null) {
                System.out.println("--- [WARMUP] Fetching Lists API... ---");
                listSet = api.fetchListsFromRemote();
                repo.save(new CachedMetadata("LISTS", mapper.writeValueAsString(listSet), now));
            }

            schemaService.applySnapshot(schemaMap, listSet);
            System.out.println("--- [WARMUP] Ready! ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}