package com.eligibility.engine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentTurnResult {
    private String sessionId;
    private String botReply;
    private RuleNode draftRule;
    private boolean readyToFinalize;
    private String pendingAttributeCandidate;
    private List<String> questions = new ArrayList<>();
    private List<String> errors = new ArrayList<>();


    public boolean hasDraft() { return draftRule != null; }
    public boolean isAskingQuestion() { return !questions.isEmpty(); }
    public boolean hasErrors() { return !errors.isEmpty(); }
}