# DESIGN.md — Rule-Based Eligibility Engine (Credit / Offer / Access)

## Overview
This project implements a rule-based eligibility engine where eligibility rules are authored via a chat interface in natural language. The backend interprets messages, asks clarifying questions when needed, validates the resulting rule, and produces a final machine-readable rule JSON. The system is designed around correctness, validation, caching, and state persistence rather than UI polish.

Key capabilities:
- Compose rules using:
    - **User Attributes** (e.g., income, age, city, credit_score)
    - **Static Lists** discovered dynamically (e.g., premium_users, blocked_users, employees)
    - **Boolean logic** (AND / OR / NOT), nested
- Agent behavior:
    - asks clarifying questions when input is ambiguous/incomplete
    - pushes back on invalid attributes/operators/lists with suggestions
- Caching:
    - schema + list metadata cached in-memory for hot path
    - cache snapshot persisted in MongoDB and rehydrated on restart
- Persistence:
    - conversation + draft rule persisted in MongoDB so sessions survive restart
- Validation:
    - final rule must pass validator before publishing

---

## Tech Stack
- Backend: **Spring Boot 4.0.1**
- Database: **MongoDB**
- JSON: Jackson (polymorphic typing for RuleNode)
- Caching: in-memory structures (Map/Set) + persisted snapshots in Mongo
- UI: simple HTML/JS chat interface

---

## Public APIs

### Chat APIs (`ChatController`, base `/api`)
- `POST /api/chat`
    - Input: `{ sessionId, message }`
    - Output: `{ sessionId, reply, currentRule, validationErrors }`
    - Behavior: updates/creates a draft rule for the session and returns live rule preview.

- `POST /api/finalize`
    - Input: `{ sessionId, message }` (message unused for finalize)
    - Output: `{ success, message, finalRuleJson, validationReportJson }`
    - Behavior: validates the current draft; if valid, exports and persists the final rule.

- `GET /api/history?sessionId=...`
    - Output: ConversationState containing chat history + currentDraftRule.

### Validator API (`ValidatorController`, base `/validator`)
- `POST /validator/validate`
    - Input: RuleNode JSON
    - Output: `ValidationReport { valid, errors, warnings }`

---

## Rule Model

### Internal Rule Tree (`RuleNode`)
Rules are represented as a tree of polymorphic nodes:

- `RuleNode` (abstract base)
    - Jackson polymorphic typing:
        - `type=ATTRIBUTE` → `AttributeRule`
        - `type=LIST` → `ListRule`
        - `type=LOGICAL` → `LogicalRule`

This allows the draft rule tree to be persisted in MongoDB and restored across restarts.

#### Node types
- **AttributeRule**
    - fields: `attribute`, `operator`, `value (Object)`
    - example: income > 50000

- **ListRule**
    - fields: `listName`, `inList` (boolean)
    - example: IN premium_users

- **LogicalRule**
    - fields: `operator` ("AND" | "OR" | "NOT"), `rules[]`
    - `NOT` is represented as a unary logical node with one child.

---

## Natural Language Parsing (Agent)
Parsing is deterministic and designed for correctness and testability.

### Tokenization + Parsing
`RuleAgentService` converts user text → tokens → AST:
- Tokenizer supports identifiers, numbers, strings, booleans, operators, AND/OR/NOT, parentheses, and IN.
- Expression parser implements precedence:
    - NOT > AND > OR
- Supports parentheses for grouping.

### Attribute validation + clarifying questions
- Attributes are looked up via `UserSchemaService`.
- Operators are validated against the schema’s allowed operator set for that attribute.
- Value type is validated (Integer/String/Boolean).
- If attribute is known but operator/value is missing:
    - agent asks a clarifying question
    - orchestrator stores `pendingAttribute` to allow the next user message to complete the condition.


---

## Orchestration & Conversation State

### Conversation Persistence (`ConversationState`)
Stored in MongoDB collection `conversations`:
- `id` (sessionId)
- `history` (list of ChatMessage with timestamps)
- `currentDraftRule` (RuleNode tree)
- `pendingAttribute` (for clarifying flow)

### Turn processing (`RuleAuthoringOrchestrator.processUserMessage`)
1. Load or create conversation state.
2. Append user message.
3. Apply `pendingAttribute` if present.
4. Support incremental composition:
    - if message begins with `AND ` or `OR ` and a draft exists, combine old + new into a `LogicalRule`.
5. Call `RuleAgentService.parseToDraft(...)`.
6. Persist updated state.

---

## Validation & Publishing

### Validation (`MockValidatorService`)
The validator walks the RuleNode tree and produces:
- `valid` boolean
- `errors[]`
- `warnings[]`

Mock policy checks included (examples):
- credit_score minimum policy conditions
- employees exclusion policy constraint

### Finalization (`RuleAuthoringOrchestrator.finalizeRule`)
1. Ensure draft exists.
2. Validate draft rule with validator.
3. If valid:
    - export final machine-readable JSON via `RuleJsonExporter`
    - persist to Mongo `finalized_rules` (FinalizedRuleDoc)
    - return `finalRuleJson` + `validationReportJson`

---

## Exported Final Rule JSON (Machine-Readable)
The system distinguishes:
1) **Draft Rule Preview JSON** (RuleNode polymorphic format, UI-friendly, persisted in Mongo)
2) **Final Published JSON** (compact exported format)

`RuleJsonExporter` outputs:
- Logical nodes:
    - AND/OR: `{ "op": "AND", "children": [...] }`
    - NOT: `{ "op": "NOT", "child": {...} }`
- Leaf nodes:
    - Attribute: `{ "type": "attr", "attribute": "...", "operator": "...", "value": ... }`
    - List: `{ "type": "list", "list": "...", "operator": "IN" }`

Trade-off: exporter is intentionally compact and decoupled from internal draft schema.

---

## Caching (Schema + Lists) & Hot Path Requirement

### Persistent cache storage
Cached discovery data is stored in MongoDB collection `system_metadata`:
- `CachedMetadata { id, json, updatedAtEpochMs }`
  Keys:
- `SCHEMA` → JSON of `Map<String, AttributeDef>`
- `LISTS` → JSON of `Set<String>`

### Warmup on startup (`MetadataWarmupService`)
Runs on startup via `@PostConstruct`:
1. Load SCHEMA from Mongo; if missing, call `MockExternalDiscoveryService.fetchSchemaFromRemote()` and persist.
2. Load LISTS from Mongo; if missing, call `MockExternalDiscoveryService.fetchListsFromRemote()` and persist.
3. Apply snapshot into in-memory services via `schemaService.applySnapshot(schemaMap, listSet)`.

### Hot path = 0 discovery calls
After warmup:
- schema checks and list checks use in-memory snapshot
- chat parsing/authoring does not call external discovery

Trade-off: real systems require cache invalidation (TTL/versioning). For this assignment, snapshots are durable until refreshed.

---

## Trade-offs
- **JSON rule tree vs string DSL**: tree is easier to validate/serialize but more verbose.
- **Deterministic parsing vs LLM**: predictable and testable, but less flexible for open-ended language.
- **Mongo persistence of draft rules**: reliable across restarts; requires polymorphic typing (implemented via `@JsonTypeInfo`).
- **Warmup cache + persisted snapshot**: ensures hot path performance and restart safety; requires refresh strategy if schema/lists change.
- **Mock validator**: demonstrates validation boundary and policies; limited unless extended into full schema-driven validator.

---

## Testing Strategy (what is covered)
Recommended/implemented tests should cover:
- invalid attribute/operator/list → pushback + suggestions
- ambiguity → clarifying question + pending attribute behavior
- boolean nesting correctness (AND/OR/NOT, parentheses)
- finalize blocks invalid rules and publishes valid ones
- caching:
    - first run populates persisted cache
    - subsequent runs do 0 remote discovery calls
- persistence:
    - conversation draft survives restart
    - cache survives restart (rehydrated from Mongo)

---

## Future Improvements
- Add explicit cache invalidation (TTL/versioning, refresh endpoint)
- Exporter enhancements (preserve NOT-IN semantics for list rules)
- Structured validator errors with JSON paths for better UX
- Rule versioning and audit history
