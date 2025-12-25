package com.eligibility.engine.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "conversations")
public class ConversationState {

    @Id
    private String id;

    private List<ChatMessage> history = new ArrayList<>();
    private RuleNode currentDraftRule;

    private String pendingAttribute;

    public void addMessage(String sender, String text) {
        this.history.add(new ChatMessage(sender, text));
    }
}