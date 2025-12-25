package com.eligibility.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MetadataRepository extends MongoRepository<CachedMetadata, String> {
}

