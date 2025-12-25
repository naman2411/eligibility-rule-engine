package com.eligibility.engine.service;

import com.eligibility.engine.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class RuleAgentService {

    private final UserSchemaService schemaService;
    private final ListCatalogService listCatalogService;

    public RuleAgentService(UserSchemaService schemaService, ListCatalogService listCatalogService) {
        this.schemaService = schemaService;
        this.listCatalogService = listCatalogService;
    }

    public AgentTurnResult parseToDraft(String naturalLanguage) {
        AgentTurnResult result = new AgentTurnResult();

        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            result.setBotReply("I'm listening. Please describe eligibility criteria (e.g., 'income > 50000').");
            return result;
        }

        try {
            List<Token> tokens = Tokenizer.tokenize(naturalLanguage.trim());
            RuleNode ast = new ExpressionParser(tokens, schemaService, listCatalogService, result).parseExpression();

            if (ast != null && result.getErrors().isEmpty() && result.getQuestions().isEmpty()) {
                result.setDraftRule(ast);
                result.setReadyToFinalize(true);
                result.setBotReply("Draft rule updated. You can refine it or finalize.");
            }
            else if (!result.getQuestions().isEmpty()) {
                result.setBotReply(result.getQuestions().get(0));
            }
            else if (!result.getErrors().isEmpty()) {
                result.setBotReply("I didn't understand that. " + result.getErrors().get(0));
            }
            else {
                result.setBotReply("Could not understand the rule. Try: 'income > 50000 AND NOT in blocked_users'.");
            }

            return result;

        } catch (Exception e) {
            result.getErrors().add("Parsing error: " + e.getMessage());
            result.setBotReply("An internal error occurred while parsing.");
            return result;
        }
    }


    enum TokenType { IDENT, NUMBER, STRING, BOOLEAN, OP, AND, OR, NOT, LPAREN, RPAREN, IN }

    record Token(TokenType type, String text) {}

    static class Tokenizer {
        private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        private static final Set<String> OPS = Set.of(">=", "<=", "!=", "==", ">", "<", "=");

        static List<Token> tokenize(String s) {
            List<Token> out = new ArrayList<>();
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c)) { i++; continue; }
                if (c == '(') { out.add(new Token(TokenType.LPAREN, "(")); i++; continue; }
                if (c == ')') { out.add(new Token(TokenType.RPAREN, ")")); i++; continue; }
                if (i + 1 < s.length() && OPS.contains(s.substring(i, i + 2))) { out.add(new Token(TokenType.OP, s.substring(i, i + 2))); i += 2; continue; }
                if (OPS.contains(String.valueOf(c))) { out.add(new Token(TokenType.OP, String.valueOf(c))); i++; continue; }
                if (c == '"' || c == '\'') {
                    char quote = c; int j = i + 1;
                    StringBuilder sb = new StringBuilder();
                    while (j < s.length() && s.charAt(j) != quote) sb.append(s.charAt(j++));
                    out.add(new Token(TokenType.STRING, sb.toString())); i = j + 1; continue;
                }
                int j = i;
                while (j < s.length()) {
                    char cj = s.charAt(j);
                    if (Character.isWhitespace(cj) || "()><=! ".indexOf(cj) >= 0) break;
                    j++;
                }
                String word = s.substring(i, j);
                String w = word.toLowerCase(Locale.ROOT);
                if (w.equals("and")) out.add(new Token(TokenType.AND, "AND"));
                else if (w.equals("or")) out.add(new Token(TokenType.OR, "OR"));
                else if (w.equals("not")) out.add(new Token(TokenType.NOT, "NOT"));
                else if (w.equals("in")) out.add(new Token(TokenType.IN, "IN"));
                else if (w.equals("true") || w.equals("false")) out.add(new Token(TokenType.BOOLEAN, w));
                else if (NUMBER.matcher(word).matches()) out.add(new Token(TokenType.NUMBER, word));
                else out.add(new Token(TokenType.IDENT, word));
                i = j;
            }
            return out;
        }
    }
    static class ExpressionParser {
        private final List<Token> tokens;
        private int pos = 0;
        private final UserSchemaService schema;
        private final ListCatalogService lists;
        private final AgentTurnResult out;

        ExpressionParser(List<Token> tokens, UserSchemaService schema, ListCatalogService lists, AgentTurnResult out) {
            this.tokens = tokens;
            this.schema = schema;
            this.lists = lists;
            this.out = out;
        }

        RuleNode parseExpression() {
            RuleNode expr = parseOr();
            if (pos < tokens.size()) out.getErrors().add("Unexpected token: " + tokens.get(pos).text());
            return expr;
        }

        private RuleNode parseOr() {
            RuleNode left = parseAnd();
            while (match(TokenType.OR)) {
                RuleNode right = parseAnd();
                left = logical("OR", left, right);
            }
            return left;
        }

        private RuleNode parseAnd() {
            RuleNode left = parseNot();
            while (match(TokenType.AND)) {
                RuleNode right = parseNot();
                left = logical("AND", left, right);
            }
            return left;
        }

        private RuleNode parseNot() {
            if (match(TokenType.NOT)) {
                RuleNode inner = parseNot();
                if (inner == null) return null;

                LogicalRule notRule = new LogicalRule();
                notRule.setOperator("NOT");
                notRule.addRule(inner);
                return notRule;
            }
            return parsePrimary();
        }

        private RuleNode parsePrimary() {
            if (match(TokenType.LPAREN)) {
                RuleNode inside = parseOr();
                expect(TokenType.RPAREN, "Missing closing ')'");
                return inside;
            }

            if (peek(TokenType.IDENT)
                    && tokens.get(pos).text().equalsIgnoreCase("users")
                    && pos + 1 < tokens.size()
                    && tokens.get(pos + 1).type() == TokenType.IN) {
                consume();
            }

            if (match(TokenType.IN)) {
                return parseListPredicate();
            }

            if (peek(TokenType.IDENT)) {
                return parseAttributePredicate();
            }

            out.getErrors().add("Expected a condition (attribute or 'IN list').");
            return null;
        }

        private RuleNode parseListPredicate() {
            Token listTok = consume();
            if (listTok == null || listTok.type() != TokenType.IDENT) {
                out.getErrors().add("Expected list name after 'in'. Example: in premium_users");
                return null;
            }
            String listName = normalizeListName(listTok.text());

            if (!lists.listExists(listName)) {
                out.getErrors().add("Unknown list '" + listName + "'. Known lists: " + lists.allLists());
                List<String> suggestions = lists.suggestLists(listName);
                if (!suggestions.isEmpty()) {
                    out.getErrors().add("Did you mean: " + suggestions + " ?");
                }
                return null;
            }

            ListRule listRule = new ListRule();
            listRule.setListName(listName);
            listRule.setInList(true);
            return listRule;
        }

        private RuleNode parseAttributePredicate() {
            Token attrTok = consume();
            String rawAttr = attrTok.text();
            AttributeDef def = schema.getAttribute(rawAttr);

            if (def == null) {
                out.getErrors().add("Unknown attribute '" + rawAttr + "'. Valid: " + schema.allAttributeNames());
                List<String> suggestions = schema.suggestAttributes(rawAttr);
                if (!suggestions.isEmpty()) out.getErrors().add("Did you mean: " + suggestions + " ?");
                return null;
            }

            Token opTok = peek();

            if ((opTok == null || opTok.type() != TokenType.OP)
                    && "Boolean".equalsIgnoreCase(def.type())) {
                AttributeRule rule = new AttributeRule();
                rule.setAttribute(def.name());
                rule.setOperator("==");
                rule.setValue(true);
                return rule;
            }

            if (opTok == null || opTok.type() != TokenType.OP) {
                out.getQuestions().add("How should I check '" + rawAttr + "'? (e.g., " + rawAttr + " > 50000)");

                out.setPendingAttributeCandidate(rawAttr);

                return null;
            }

            consume();
            String op = normalizeOperator(opTok.text());

            Token lit = consume();
            if (lit == null) {
                out.getQuestions().add("What value should '" + rawAttr + " " + op + "' compare against?");
                return null;
            }

            Object value = parseLiteral(lit);

            if (!def.operators().contains(op)) {
                out.getErrors().add("Operator '" + op + "' not allowed for attribute '" + def.name() + "'. Allowed: " + def.operators());
                return null;
            }
            if (!typeMatches(def.type(), value)) {
                out.getErrors().add("Value type mismatch for '" + def.name() + "'. Expected " + def.type());
                return null;
            }

            AttributeRule rule = new AttributeRule();
            rule.setAttribute(def.name());
            rule.setOperator(op);
            rule.setValue(value);
            return rule;
        }

        private static boolean typeMatches(String type, Object val) {
            return switch (type.toLowerCase()) {
                case "integer", "number" -> val instanceof Number;
                case "string" -> val instanceof String;
                case "boolean" -> val instanceof Boolean;
                default -> true;
            };
        }
        private static Object parseLiteral(Token t) {
            return switch (t.type()) {
                case NUMBER -> t.text().contains(".") ? Double.parseDouble(t.text()) : Integer.parseInt(t.text());
                case STRING -> t.text();
                case BOOLEAN -> Boolean.parseBoolean(t.text());
                default -> t.text();
            };
        }
        private static String normalizeOperator(String op) { return op.equals("=") ? "==" : op; }
        private static String normalizeListName(String s) { return s.trim().toLowerCase().replace(" ", "_"); }
        private void expect(TokenType t, String m) { if (!match(t)) out.getErrors().add(m); }
        private boolean match(TokenType t) { if (pos < tokens.size() && tokens.get(pos).type() == t) { pos++; return true; } return false; }
        private boolean peek(TokenType t) { return pos < tokens.size() && tokens.get(pos).type() == t; }
        private Token consume() { return pos < tokens.size() ? tokens.get(pos++) : null; }
        private Token peek() { return pos < tokens.size() ? tokens.get(pos) : null; }
        private RuleNode logical(String op, RuleNode l, RuleNode r) { LogicalRule lr = new LogicalRule(); lr.setOperator(op); lr.addRule(l); lr.addRule(r); return lr; }
    }
}