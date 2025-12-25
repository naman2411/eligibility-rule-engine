package com.eligibility.engine.controller;

import com.eligibility.engine.model.*;
import com.eligibility.engine.service.RuleAuthoringOrchestrator;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final RuleAuthoringOrchestrator orchestrator;

    public ChatController(RuleAuthoringOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/finalize")
    public RuleAuthoringOrchestrator.FinalizeResult finalizeRule(@RequestBody ChatRequest request) {
        return orchestrator.finalizeRule(request.getSessionId());
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }


        AgentTurnResult result = orchestrator.processUserMessage(sessionId, request.getMessage());


        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setReply(result.getBotReply());
        response.setCurrentRule(result.getDraftRule());

        if (result.hasErrors()) {
            response.setValidationErrors(result.getErrors());
        } else {
            response.setValidationErrors(List.of());
        }

        return response;
    }

    @GetMapping("/history")
    public ConversationState getHistory(@RequestParam String sessionId) {
        return orchestrator.getHistory(sessionId);
    }
}




