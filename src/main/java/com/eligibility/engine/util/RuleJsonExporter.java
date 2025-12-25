package com.eligibility.engine.util;

import com.eligibility.engine.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

public class RuleJsonExporter {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String export(RuleNode root) {
        try {
            Map<String, Object> map = toMap(root);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Map<String, Object> toMap(RuleNode node) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (node instanceof LogicalRule) {
            LogicalRule lr = (LogicalRule) node;
            if ("NOT".equalsIgnoreCase(lr.getOperator())) {
                map.put("op", "NOT");
                if (!lr.getRules().isEmpty()) {
                    map.put("child", toMap(lr.getRules().get(0)));
                }
            } else {
                map.put("op", lr.getOperator());
                map.put("children", lr.getRules().stream()
                        .map(RuleJsonExporter::toMap)
                        .collect(Collectors.toList()));
            }
        }
        else if (node instanceof AttributeRule) {
            AttributeRule ar = (AttributeRule) node;
            map.put("type", "attr");
            map.put("attribute", ar.getAttribute());
            map.put("operator", ar.getOperator());
            map.put("value", ar.getValue());
        }
        else if (node instanceof ListRule) {
            ListRule lr = (ListRule) node;
            map.put("type", "list");
            map.put("list", lr.getListName());

            map.put("operator", "IN");
        }
        return map;
    }
}