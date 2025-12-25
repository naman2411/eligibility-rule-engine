package com.eligibility.engine.service;

import com.eligibility.engine.model.AgentTurnResult;
import com.eligibility.engine.model.AttributeDef;
import com.eligibility.engine.model.AttributeRule;
import com.eligibility.engine.model.LogicalRule;
import com.eligibility.engine.model.ListRule;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RuleAgentServiceTest {

    // Simple in-memory schema/list services for unit testing
    static class InMemorySchema implements UserSchemaService {
        private final Map<String, AttributeDef> defs;
        InMemorySchema(Map<String, AttributeDef> defs) { this.defs = defs; }

        @Override public AttributeDef getAttribute(String name) { return defs.get(name); }
        @Override public List<String> allAttributeNames() { return new ArrayList<>(defs.keySet()); }
        @Override public List<String> suggestAttributes(String wrong) { return List.of(); }
    }

    static class InMemoryLists implements ListCatalogService {
        private final Set<String> lists;
        InMemoryLists(Set<String> lists) { this.lists = lists; }

        @Override public boolean listExists(String listName) { return lists.contains(listName); }
        @Override public List<String> allLists() { return new ArrayList<>(lists); }
        @Override public List<String> suggestLists(String wrong) { return List.of(); }
    }

    private RuleAgentService newAgent() {
        Map<String, AttributeDef> schema = new HashMap<>();
        schema.put("income", new AttributeDef("income", "Integer", Set.of(">", "<", ">=", "<=", "==", "!=")));
        schema.put("age", new AttributeDef("age", "Integer", Set.of(">", "<", ">=", "<=", "==", "!=")));
        schema.put("city", new AttributeDef("city", "String", Set.of("==", "!=")));
        return new RuleAgentService(new InMemorySchema(schema), new InMemoryLists(Set.of("premium_users", "blocked_users")));
    }

    @Test
    void parsesSimpleAttributeRule() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("income > 50000");
        assertTrue(r.getErrors().isEmpty());
        assertTrue(r.getQuestions().isEmpty());
        assertNotNull(r.getDraftRule());

        assertTrue(r.getDraftRule() instanceof AttributeRule);
        AttributeRule ar = (AttributeRule) r.getDraftRule();
        assertEquals("income", ar.getAttribute());
        assertEquals(">", ar.getOperator());
        assertTrue(ar.getValue() instanceof Number);
        assertEquals(50000, ((Number) ar.getValue()).intValue());
    }

    @Test
    void asksClarifyingQuestionWhenOperatorMissing() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("income");
        assertNull(r.getDraftRule());
        assertFalse(r.getQuestions().isEmpty());
        assertEquals("income", r.getPendingAttributeCandidate());
    }

    @Test
    void rejectsUnknownAttribute() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("incom > 10");
        assertNull(r.getDraftRule());
        assertFalse(r.getErrors().isEmpty());
        assertTrue(r.getErrors().get(0).toLowerCase().contains("unknown attribute"));
    }

    @Test
    void parsesAndExpression() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("income > 50000 AND age >= 21");
        assertNotNull(r.getDraftRule());
        assertTrue(r.getDraftRule() instanceof LogicalRule);

        LogicalRule lr = (LogicalRule) r.getDraftRule();
        assertEquals("AND", lr.getOperator());
        assertEquals(2, lr.getRules().size());
    }

    @Test
    void parsesNotExpression() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("NOT city == \"San Francisco\"");
        assertNotNull(r.getDraftRule());
        assertTrue(r.getDraftRule() instanceof LogicalRule);

        LogicalRule not = (LogicalRule) r.getDraftRule();
        assertEquals("NOT", not.getOperator());
        assertEquals(1, not.getRules().size());
    }

    @Test
    void parsesListMembershipIn() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("in premium_users");
        assertNotNull(r.getDraftRule());
        assertTrue(r.getDraftRule() instanceof ListRule);

        ListRule lr = (ListRule) r.getDraftRule();
        assertEquals("premium_users", lr.getListName());
        assertTrue(lr.isInList());
    }

    @Test
    void rejectsUnknownList() {
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("in premum_users");
        assertNull(r.getDraftRule());
        assertFalse(r.getErrors().isEmpty());
        assertTrue(r.getErrors().get(0).toLowerCase().contains("unknown list"));
    }

    @Test
    void operatorPrecedence_andBeforeOr() {
        // a OR b AND c  => OR(a, AND(b,c))
        RuleAgentService agent = newAgent();

        AgentTurnResult r = agent.parseToDraft("income > 10 OR age > 1 AND age < 99");
        assertNotNull(r.getDraftRule());
        assertTrue(r.getDraftRule() instanceof LogicalRule);

        LogicalRule top = (LogicalRule) r.getDraftRule();
        assertEquals("OR", top.getOperator());
        assertEquals(2, top.getRules().size());
        assertTrue(top.getRules().get(1) instanceof LogicalRule);
        assertEquals("AND", ((LogicalRule) top.getRules().get(1)).getOperator());
    }
}