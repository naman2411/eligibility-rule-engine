package com.eligibility.engine.repository;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FinalizedRuleRepository extends MongoRepository<FinalizedRuleDoc, String> {}