package com.eligibility.engine.service;

import com.eligibility.engine.model.*;
import com.eligibility.engine.repository.ConversationRepository;
import com.eligibility.engine.repository.FinalizedRuleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleAuthoringOrchestratorTest {

    @Test
    void processUserMessage_combinesWithAndPrefix() {
        ConversationRepository convRepo = mock(ConversationRepository.class);
        RuleAgentService agent = mock(RuleAgentService.class);
        MockValidatorService validator = mock(MockValidatorService.class);
        FinalizedRuleRepository finalRepo = mock(FinalizedRuleRepository.class);

        ConversationState state = new ConversationState();
        state.setId("s1");
        AttributeRule existing = new AttributeRule();
        existing.setAttribute("income");
        existing.setOperator(">");
        existing.setValue(10);
        state.setCurrentDraftRule(existing);

        when(convRepo.findById("s1")).thenReturn(Optional.of(state));

        AttributeRule newRule = new AttributeRule();
        newRule.setAttribute("age");
        newRule.setOperator(">");
        newRule.setValue(18);

        AgentTurnResult turn = new AgentTurnResult();
        turn.setDraftRule(newRule);
        turn.setBotReply("ok");

        when(agent.parseToDraft("age > 18")).thenReturn(turn);

        RuleAuthoringOrchestrator orch = new RuleAuthoringOrchestrator(convRepo, agent, validator, finalRepo);

        AgentTurnResult out = orch.processUserMessage("s1", "AND age > 18");
        assertNotNull(out.getDraftRule());
        assertTrue(out.getDraftRule() instanceof LogicalRule);

        LogicalRule combined = (LogicalRule) out.getDraftRule();
        assertEquals("AND", combined.getOperator());
        assertEquals(2, combined.getRules().size());

        verify(convRepo, times(1)).save(any(ConversationState.class));
    }

    @Test
    void finalizeRule_returnsFailure_whenNoDraft() {
        ConversationRepository convRepo = mock(ConversationRepository.class);
        RuleAgentService agent = mock(RuleAgentService.class);
        MockValidatorService validator = mock(MockValidatorService.class);
        FinalizedRuleRepository finalRepo = mock(FinalizedRuleRepository.class);

        ConversationState state = new ConversationState();
        state.setId("s1");
        state.setCurrentDraftRule(null);

        when(convRepo.findById("s1")).thenReturn(Optional.of(state));

        RuleAuthoringOrchestrator orch = new RuleAuthoringOrchestrator(convRepo, agent, validator, finalRepo);
        RuleAuthoringOrchestrator.FinalizeResult res = orch.finalizeRule("s1");

        assertFalse(res.success());
        verify(finalRepo, never()).save(any());
    }

    @Test
    void finalizeRule_savesFinalizedRule_whenValid() {
        ConversationRepository convRepo = mock(ConversationRepository.class);
        RuleAgentService agent = mock(RuleAgentService.class);
        MockValidatorService validator = mock(MockValidatorService.class);
        FinalizedRuleRepository finalRepo = mock(FinalizedRuleRepository.class);

        ConversationState state = new ConversationState();
        state.setId("s1");

        AttributeRule draft = new AttributeRule();
        draft.setAttribute("income");
        draft.setOperator(">");
        draft.setValue(50000);
        state.setCurrentDraftRule(draft);

        when(convRepo.findById("s1")).thenReturn(Optional.of(state));

        MockValidatorService.ValidationReport report = new MockValidatorService.ValidationReport();
        report.valid = true;
        report.errors = List.of();
        report.warnings = List.of();

        when(validator.validate(any())).thenReturn(report);

        RuleAuthoringOrchestrator orch = new RuleAuthoringOrchestrator(convRepo, agent, validator, finalRepo);
        RuleAuthoringOrchestrator.FinalizeResult res = orch.finalizeRule("s1");

        assertTrue(res.success());
        verify(finalRepo, times(1)).save(any());
        verify(convRepo, times(1)).save(any(ConversationState.class));
    }
}