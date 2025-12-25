package com.eligibility.engine.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "finalized_rules")
public class FinalizedRuleDoc {
    @Id
    public String id; // usually generated UUID or same as sessionId

    public String sessionId;
    public String finalRuleJson;
    public String validationReportJson;
    public long createdAtEpochMs;

    public FinalizedRuleDoc() {}

    public FinalizedRuleDoc(String sessionId, String ruleJson, String reportJson) {
        this.sessionId = sessionId;
        this.finalRuleJson = ruleJson;
        this.validationReportJson = reportJson;
        this.createdAtEpochMs = Instant.now().toEpochMilli();
    }
}