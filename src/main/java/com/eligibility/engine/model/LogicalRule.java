package com.eligibility.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class LogicalRule extends RuleNode {

    private String operator;
    private List<RuleNode> rules = new ArrayList<>();


    public boolean isUnaryNot() {
        return "NOT".equalsIgnoreCase(operator);
    }

    public void addRule(RuleNode rule) {
        this.rules.add(rule);
    }

    @Override
    public String toString() {
        if (rules == null || rules.isEmpty()) return "";
        if (isUnaryNot()) {
            return "NOT (" + rules.get(0).toString() + ")";
        }
        return "(" + rules.stream()
                .map(RuleNode::toString)
                .collect(Collectors.joining(" " + operator + " ")) + ")";
    }
}