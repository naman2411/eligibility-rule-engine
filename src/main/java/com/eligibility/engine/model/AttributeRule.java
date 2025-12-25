package com.eligibility.engine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeRule extends RuleNode {
    private String attribute;
    private String operator;
    private Object value;
    @Override
    public String toString() {
        return attribute + " " + operator + " " + value;
    }
}