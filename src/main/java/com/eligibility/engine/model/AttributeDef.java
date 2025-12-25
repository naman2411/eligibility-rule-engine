package com.eligibility.engine.model;

import java.util.Set;

public record AttributeDef(String name, String type, Set<String> operators) {}