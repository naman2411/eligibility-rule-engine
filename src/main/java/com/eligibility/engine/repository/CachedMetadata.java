package com.eligibility.engine.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "system_metadata")
public class CachedMetadata {

    @Id
    public String id;

    public String json;

    public long updatedAtEpochMs;

    public CachedMetadata() {}


    public CachedMetadata(String id, String json, long updatedAtEpochMs) {
        this.id = id;
        this.json = json;
        this.updatedAtEpochMs = updatedAtEpochMs;
    }
}