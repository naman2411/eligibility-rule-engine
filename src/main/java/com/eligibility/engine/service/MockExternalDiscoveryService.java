package com.eligibility.engine.service;

import com.eligibility.engine.model.AttributeDef;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MockExternalDiscoveryService {

    public Map<String, AttributeDef> fetchSchemaFromRemote() {
        try { Thread.sleep(500); } catch (InterruptedException e) {} // Simulate network lag

        Map<String, AttributeDef> map = new HashMap<>();
        map.put("income", new AttributeDef("income", "Integer", Set.of(">", "<", ">=", "<=", "==", "!=")));
        map.put("age", new AttributeDef("age", "Integer", Set.of(">", "<", ">=", "<=", "==", "!=")));
        map.put("city", new AttributeDef("city", "String", Set.of("==", "!=", "IN")));
        map.put("credit_score", new AttributeDef("credit_score", "Integer", Set.of(">", "<", ">=", "<=", "==", "!=")));
        return map;
    }

    public Set<String> fetchListsFromRemote() {
        try { Thread.sleep(500); } catch (InterruptedException e) {} // Simulate network lag
        return Set.of("premium_users", "blocked_users", "vip_list", "employees");
    }
}