package com.eligibility.engine.service;

import com.eligibility.engine.model.AttributeDef;
import java.util.List;

public interface UserSchemaService {
    AttributeDef getAttribute(String name);
    List<String> allAttributeNames();
    List<String> suggestAttributes(String wrong);
}