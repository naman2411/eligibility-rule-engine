package com.eligibility.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
    private String sender;
    private String text;
    private long timestamp = System.currentTimeMillis();


    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }
}