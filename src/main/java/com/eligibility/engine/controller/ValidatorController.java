package com.eligibility.engine.controller;
import com.eligibility.engine.model.RuleNode;
import com.eligibility.engine.service.MockValidatorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validator")
public class ValidatorController {

    private final MockValidatorService validator;

    public ValidatorController(MockValidatorService validator) {
        this.validator = validator;
    }

    @PostMapping("/validate")
    public MockValidatorService.ValidationReport validate(@RequestBody RuleNode rule) {
        return validator.validate(rule);
    }
}