package com.eligibility.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ListRule extends RuleNode {
    private String listName;
    private boolean inList;

    @Override
    public String toString() {
        return (inList ? "IN " : "NOT IN ") + listName;
    }
}