package com.eligibility.engine.service;

import com.eligibility.engine.model.*;
import com.eligibility.engine.repository.ConversationRepository;
import com.eligibility.engine.repository.FinalizedRuleDoc;
import com.eligibility.engine.repository.FinalizedRuleRepository;
import com.eligibility.engine.util.RuleJsonExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleAuthoringOrchestrator {

    private final ConversationRepository conversationRepo;
    private final RuleAgentService ruleAgentService;
    private final MockValidatorService validatorService;
    private final FinalizedRuleRepository finalRuleRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public RuleAuthoringOrchestrator(ConversationRepository conversationRepo,
                                     RuleAgentService ruleAgentService,
                                     MockValidatorService validatorService,
                                     FinalizedRuleRepository finalRuleRepo
                                     ) {
        this.conversationRepo = conversationRepo;
        this.ruleAgentService = ruleAgentService;
        this.validatorService = validatorService;
        this.finalRuleRepo = finalRuleRepo;

    }

    public AgentTurnResult processUserMessage(String sessionId, String userText) {
        ConversationState state = conversationRepo.findById(sessionId).orElse(new ConversationState());
        state.setId(sessionId);
        state.addMessage("User", userText);

        String textToParse = userText.trim();
        String upperText = textToParse.toUpperCase();

        if (state.getPendingAttribute() != null) {
            textToParse = state.getPendingAttribute() + " " + textToParse;
            state.setPendingAttribute(null);
        }

        String joinOp = null;
        if (state.getCurrentDraftRule() != null) {
            if (upperText.startsWith("AND ")) {
                joinOp = "AND";
                textToParse = textToParse.substring(4).trim();
            } else if (upperText.startsWith("OR ")) {
                joinOp = "OR";
                textToParse = textToParse.substring(3).trim();
            }
        }

        AgentTurnResult result = ruleAgentService.parseToDraft(textToParse);

        if (result.getDraftRule() != null) {
            RuleNode newRule = result.getDraftRule();

            if (joinOp != null && state.getCurrentDraftRule() != null) {
                LogicalRule combined = new LogicalRule(joinOp, List.of(state.getCurrentDraftRule(), newRule));
                state.setCurrentDraftRule(combined);
                result.setDraftRule(combined);
                result.setBotReply("Updated rule with " + joinOp + " condition.");
            } else {
                state.setCurrentDraftRule(newRule);
            }
        }
        else if (!result.getQuestions().isEmpty()) {
            if (result.getPendingAttributeCandidate() != null) {
                state.setPendingAttribute(result.getPendingAttributeCandidate());
            }
            else if (textToParse.matches("^[a-zA-Z_]+$")) {
                state.setPendingAttribute(textToParse);
            }
        }

        state.addMessage("Bot", result.getBotReply());
        conversationRepo.save(state);
        return result;
    }

    public FinalizeResult finalizeRule(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return new FinalizeResult(false, "Session ID is missing.", null, null);
        }

        ConversationState state = conversationRepo.findById(sessionId).orElse(null);
        if (state == null || state.getCurrentDraftRule() == null) {
            return new FinalizeResult(false, "No active rule to finalize.", null, null);
        }

        MockValidatorService.ValidationReport report = validatorService.validate(state.getCurrentDraftRule());

        String reportJson;
        try {
            reportJson = mapper.writeValueAsString(report);
        } catch (Exception e) {
            reportJson = "{\"valid\":false, \"errors\":[\"Report Serialization Failed\"]}";
        }

        if (!report.valid) {
            return new FinalizeResult(false, "Validation Failed: " + (report.errors.isEmpty() ? "" : report.errors.get(0)), null, reportJson);
        }

        try {
            String finalJson = RuleJsonExporter.export(state.getCurrentDraftRule());

            FinalizedRuleDoc doc = new FinalizedRuleDoc(sessionId, finalJson, reportJson);
            finalRuleRepo.save(doc);

            state.addMessage("Bot", "Finalized and saved.");
            conversationRepo.save(state);

            return new FinalizeResult(true, "Rule Published!", finalJson, reportJson);
        } catch (Exception e) {
            return new FinalizeResult(false, "Export failed: " + e.getMessage(), null, reportJson);
        }
    }

    public record FinalizeResult(boolean success, String message, String finalRuleJson, String validationReportJson) {}
    public ConversationState getHistory(String sessionId) {
        return conversationRepo.findById(sessionId).orElse(null);
    }
}