/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.reports.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Minimal templating for report configurations, compatible with the subset of
 * Handlebars that was supported before Handlebars was removed:
 * <ul>
 * <li>{@code {{name}}}: value, HTML escaped exactly like Handlebars does</li>
 * <li>{@code {{{name}}}} or {@code {{&name}}}: value, not escaped</li>
 * <li>{@code {{#if name}}...{{else}}...{{/if}}} and
 * {@code {{#unless name}}...{{else}}...{{/unless}}}, which may be nested</li>
 * <li>{@code {{! comment}}}, {@code {{!-- comment --}}} and {@code \{{} for a literal {@code {{}</li>
 * </ul>
 * A missing or empty parameter renders as an empty string and is falsy. Any other Handlebars construct
 * (helpers, partials, {@code #each}, paths, whitespace control) is rejected with an
 * {@link IllegalArgumentException} rather than silently rendered differently.
 */
final class ParameterTemplate {

    private static final Pattern NAME = Pattern.compile("[\\w:$-]+");

    private static final String IF = "if";
    private static final String UNLESS = "unless";

    private static final String[] ESCAPE_SEARCH = {"&", "<", ">", "\"", "'", "`", "="};
    private static final String[] ESCAPE_REPLACE = {"&amp;", "&lt;", "&gt;", "&quot;", "&#x27;", "&#x60;", "&#x3D;"};

    private ParameterTemplate() {
    }

    /**
     * Renders the template with the given parameters.
     *
     * @throws IllegalArgumentException if the template is malformed or uses unsupported syntax
     */
    static String render(String template, Map<String, String> parameters) {
        if (template == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        new Parser(tokenize(template)).parseUntilClose(null).forEach(n -> n.render(parameters, sb));
        return sb.toString();
    }

    // ---------------------------------------------------------------- tokenizer

    private enum TokenType { TEXT, VAR, RAW_VAR, OPEN, ELSE, CLOSE }

    private static final class Token {
        final TokenType type;
        final String value;
        final String name;

        Token(TokenType type, String value, String name) {
            this.type = type;
            this.value = value;
            this.name = name;
        }
    }

    private static List<Token> tokenize(String template) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int pos = 0;
        while (pos < template.length()) {
            int open = template.indexOf("{{", pos);
            if (open < 0) {
                text.append(template, pos, template.length());
                break;
            }
            if (open > 0 && template.charAt(open - 1) == '\\') {
                text.append(template, pos, open - 1).append("{{");
                pos = open + 2;
                continue;
            }
            text.append(template, pos, open);
            if (text.length() > 0) {
                tokens.add(new Token(TokenType.TEXT, text.toString(), null));
                text.setLength(0);
            }

            if (template.startsWith("{{!--", open)) {
                pos = endOf(template, "--}}", open);
            } else if (template.startsWith("{{{", open)) {
                int end = endOf(template, "}}}", open);
                tokens.add(new Token(TokenType.RAW_VAR, null, name(template.substring(open + 3, end - 3).trim())));
                pos = end;
            } else {
                int end = endOf(template, "}}", open);
                String expression = template.substring(open + 2, end - 2).trim();
                if (!expression.startsWith("!")) {
                    tokens.add(toToken(expression));
                }
                pos = end;
            }
        }
        if (text.length() > 0) {
            tokens.add(new Token(TokenType.TEXT, text.toString(), null));
        }
        return tokens;
    }

    private static int endOf(String template, String delimiter, int open) {
        int end = template.indexOf(delimiter, open);
        if (end < 0) {
            throw new IllegalArgumentException("Unterminated expression at position " + open + ", expected '"
                    + delimiter + "'");
        }
        return end + delimiter.length();
    }

    private static Token toToken(String expression) {
        if (expression.startsWith("&")) {
            return new Token(TokenType.RAW_VAR, null, name(expression.substring(1).trim()));
        } else if ("else".equals(expression) || "^".equals(expression)) {
            return new Token(TokenType.ELSE, null, null);
        } else if (expression.startsWith("#")) {
            String[] parts = expression.substring(1).trim().split("\\s+");
            if (parts.length != 2 || !(IF.equals(parts[0]) || UNLESS.equals(parts[0]))) {
                throw unsupported(expression);
            }
            return new Token(TokenType.OPEN, parts[0], name(parts[1]));
        } else if (expression.startsWith("/")) {
            return new Token(TokenType.CLOSE, expression.substring(1).trim(), null);
        }
        return new Token(TokenType.VAR, null, name(expression));
    }

    private static String name(String name) {
        if (!NAME.matcher(name).matches() || "this".equals(name)) {
            throw unsupported(name);
        }
        return name;
    }

    private static IllegalArgumentException unsupported(String expression) {
        return new IllegalArgumentException("Unsupported template expression '{{" + expression
                + "}}', only {{name}}, {{{name}}}, {{#if name}}, {{#unless name}} and {{else}} are supported");
    }

    // ---------------------------------------------------------------- parser

    private static final class Parser {
        private final List<Token> tokens;
        private int index;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Parses nodes until the matching close (or {@code else}) of the given block, or the end of the template
         * when {@code block} is null. The terminating token is left for the caller.
         */
        List<Node> parseUntilClose(String block) {
            List<Node> nodes = new ArrayList<>();
            while (index < tokens.size()) {
                Token token = tokens.get(index);
                switch (token.type) {
                    case TEXT:
                        nodes.add((p, sb) -> sb.append(token.value));
                        break;
                    case VAR:
                        nodes.add((p, sb) -> sb.append(escape(StringUtils.defaultString(p.get(token.name)))));
                        break;
                    case RAW_VAR:
                        nodes.add((p, sb) -> sb.append(StringUtils.defaultString(p.get(token.name))));
                        break;
                    case OPEN:
                        index++;
                        nodes.add(parseBlock(token));
                        break;
                    case ELSE:
                    case CLOSE:
                        if (block == null) {
                            throw new IllegalArgumentException("Unexpected {{"
                                    + (token.type == TokenType.ELSE ? "else" : "/" + token.value) + "}}");
                        }
                        return nodes;
                    default:
                        throw new IllegalStateException("Unknown token type " + token.type);
                }
                index++;
            }
            if (block != null) {
                throw new IllegalArgumentException("Missing {{/" + block + "}}");
            }
            return nodes;
        }

        private Node parseBlock(Token open) {
            List<Node> whenTrue = parseUntilClose(open.value);
            List<Node> whenFalse = new ArrayList<>();
            if (tokens.get(index).type == TokenType.ELSE) {
                index++;
                whenFalse = parseUntilClose(open.value);
                if (tokens.get(index).type == TokenType.ELSE) {
                    throw new IllegalArgumentException("Duplicate {{else}} in {{#" + open.value + "}}");
                }
            }
            Token close = tokens.get(index);
            if (!open.value.equals(close.value)) {
                throw new IllegalArgumentException("Expected {{/" + open.value + "}} but found {{/" + close.value + "}}");
            }
            boolean unless = UNLESS.equals(open.value);
            List<Node> ifTrue = whenTrue;
            List<Node> ifFalse = whenFalse;
            return (p, sb) -> {
                boolean condition = StringUtils.isNotEmpty(p.get(open.name)) != unless;
                (condition ? ifTrue : ifFalse).forEach(n -> n.render(p, sb));
            };
        }
    }

    @FunctionalInterface
    private interface Node {
        void render(Map<String, String> parameters, StringBuilder sb);
    }

    /**
     * Escapes the same characters, with the same entities, as Handlebars' default HTML escaping.
     */
    static String escape(String value) {
        return StringUtils.replaceEach(value, ESCAPE_SEARCH, ESCAPE_REPLACE);
    }
}
