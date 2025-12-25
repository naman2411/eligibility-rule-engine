package com.eligibility.engine.controller;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatRequest {

        private String sessionId;
        private String message;


}
