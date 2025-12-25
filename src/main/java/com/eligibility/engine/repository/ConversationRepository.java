package com.eligibility.engine.repository;

import com.eligibility.engine.model.ConversationState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends MongoRepository<ConversationState, String> {
}