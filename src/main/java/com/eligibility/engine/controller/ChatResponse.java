package com.eligibility.engine.controller;

import com.eligibility.engine.model.RuleNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

        private String sessionId;
        private String reply;
        private RuleNode currentRule;
        private List<String> validationErrors;

}
