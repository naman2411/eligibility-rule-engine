package com.eligibility.engine.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeRule.class, name = "ATTRIBUTE"),
        @JsonSubTypes.Type(value = ListRule.class, name = "LIST"),
        @JsonSubTypes.Type(value = LogicalRule.class, name = "LOGICAL")
})
public abstract class RuleNode {

    public abstract String toString();
}