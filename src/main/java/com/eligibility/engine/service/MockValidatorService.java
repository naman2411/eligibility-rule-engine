package com.eligibility.engine.service;

import com.eligibility.engine.model.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockValidatorService {


    public static class ValidationReport {
        public boolean valid;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    public ValidationReport validate(RuleNode root) {
        ValidationReport report = new ValidationReport();

        if (root == null) {
            report.errors.add("Rule is empty.");
        } else {

            validateNode(root, report, false);
        }

        report.valid = report.errors.isEmpty();
        return report;
    }


    private void validateNode(RuleNode node, ValidationReport report, boolean insideNot) {
        if (node instanceof LogicalRule) {
            LogicalRule lr = (LogicalRule) node;
            boolean isNot = "NOT".equalsIgnoreCase(lr.getOperator());

            for (RuleNode child : lr.getRules()) {

                validateNode(child, report, insideNot ^ isNot);
            }
        }
        else if (node instanceof AttributeRule) {
            AttributeRule ar = (AttributeRule) node;


            if ("credit_score".equalsIgnoreCase(ar.getAttribute())) {
                if (ar.getValue() instanceof Number) {
                    int val = ((Number) ar.getValue()).intValue();

                    if (val < 300 && (ar.getOperator().equals("<") || ar.getOperator().equals("<="))) {
                        report.errors.add("Policy Violation: Credit score minimum is 300.");
                    }
                }
            }
        }
        else if (node instanceof ListRule) {
            ListRule lr = (ListRule) node;


            boolean effectivelyExcluded = (lr.isInList() && insideNot) || !lr.isInList();

            if ("employees".equalsIgnoreCase(lr.getListName()) && effectivelyExcluded) {
                report.errors.add("Policy Violation: You cannot exclude 'employees' from eligibility.");
            }
        }
    }
}