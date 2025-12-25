package com.eligibility.engine.util;

import com.eligibility.engine.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleJsonExporterTest {

    @Test
    void exportsNotNodeAsChild() {
        AttributeRule ar = new AttributeRule();
        ar.setAttribute("city");
        ar.setOperator("==");
        ar.setValue("San Francisco");

        LogicalRule not = new LogicalRule();
        not.setOperator("NOT");
        not.addRule(ar);

        String json = RuleJsonExporter.export(not);

        assertTrue(json.contains("\"op\" : \"NOT\""));
        assertTrue(json.contains("\"child\""));
        assertTrue(json.contains("\"attribute\" : \"city\""));
    }

    @Test
    void exportsAndNodeAsChildren() {
        AttributeRule a = new AttributeRule();
        a.setAttribute("income");
        a.setOperator(">");
        a.setValue(50000);

        AttributeRule b = new AttributeRule();
        b.setAttribute("age");
        b.setOperator(">=");
        b.setValue(21);

        LogicalRule and = new LogicalRule();
        and.setOperator("AND");
        and.addRule(a);
        and.addRule(b);

        String json = RuleJsonExporter.export(and);

        assertTrue(json.contains("\"op\" : \"AND\""));
        assertTrue(json.contains("\"children\""));
        assertTrue(json.contains("\"attribute\" : \"income\""));
        assertTrue(json.contains("\"attribute\" : \"age\""));
    }
}