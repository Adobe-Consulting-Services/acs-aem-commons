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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * The expected values are the output of Handlebars 4.3.1, which was used before for these templates.
 */
public class ParameterTemplateTest {

    private static final String SAMPLE_QUERY = "SELECT * FROM [nt:base] AS s WHERE [sling:resourceType]='{{resourceType}}'"
            + " {{#if path }} AND ISDESCENDANTNODE([{{{path}}}]){{/if}}";

    private static Map<String, String> params(String... keyValues) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.put(keyValues[i], keyValues[i + 1]);
        }
        return params;
    }

    private static void assertRenders(String expected, String template, String... keyValues) {
        assertEquals(expected, ParameterTemplate.render(template, params(keyValues)));
    }

    private static void assertRejected(String template) {
        assertThrows(IllegalArgumentException.class, () -> ParameterTemplate.render(template, Collections.emptyMap()));
    }

    @Test
    public void testVariables() {
        assertRenders("x=v", "x={{x}}", "x", "v");
        assertRenders("x=v", "x={{ x }}", "x", "v");
        assertRenders("x=v", "x={{{ x }}}", "x", "v");
        assertRenders("x=", "x={{missing}}");
        assertRenders("x=v", "x={{my-param}}", "my-param", "v");
        assertRenders("x=v", "x={{a_b}}", "a_b", "v");
        assertRenders("x=v", "x={{a:b}}", "a:b", "v");
        assertRenders("A\nv\nB", "A\n{{x}}\nB", "x", "v");
        assertRenders("", null);
        assertRenders("plain text } }} {", "plain text } }} {");
    }

    @Test
    public void testEscaping() {
        String value = "a<b>c\"d'e`f=g&h/i\\j";
        assertRenders("x=a&lt;b&gt;c&quot;d&#x27;e&#x60;f&#x3D;g&amp;h/i\\j", "x={{x}}", "x", value);
        assertRenders("x=" + value, "x={{{x}}}", "x", value);
        assertRenders("x=<", "x={{&x}}", "x", "<");
        assertRenders("&lt;", "{{#if x}}{{x}}{{/if}}", "x", "<");
    }

    @Test
    public void testIf() {
        assertRenders("Y", "{{#if x}}Y{{/if}}", "x", "v");
        assertRenders("", "{{#if x}}Y{{/if}}", "x", "");
        assertRenders("Y", "{{#if x}}Y{{/if}}", "x", " ");
        assertRenders("Y", "{{#if x}}Y{{/if}}", "x", "false");
        assertRenders("Y", "{{#if x}}Y{{/if}}", "x", "0");
        assertRenders("", "{{#if x}}Y{{/if}}");
        assertRenders("Y", "{{#if x }}Y{{/if}}", "x", "v");
        assertRenders("N", "{{#if x}}Y{{else}}N{{/if}}");
        assertRenders("N", "{{#if x}}Y{{^}}N{{/if}}");
        assertRenders("X", "{{#if x}}{{#if y}}XY{{else}}X{{/if}}{{/if}}", "x", "1");
        assertRenders("XY", "{{#if x}}{{#if y}}XY{{else}}X{{/if}}{{/if}}", "x", "1", "y", "1");
        assertRenders("A\n\nY\n\nB", "A\n{{#if x}}\nY\n{{/if}}\nB", "x", "v");
        assertRenders("A\n  \nB", "A\n  {{#if x}}\nY\n  {{/if}}\nB");
    }

    @Test
    public void testUnless() {
        assertRenders("N", "{{#unless x}}N{{else}}Y{{/unless}}");
        assertRenders("Y", "{{#unless x}}N{{else}}Y{{/unless}}", "x", "v");
        assertRenders("", "{{#unless x}}N{{/unless}}", "x", "v");
    }

    @Test
    public void testCommentsAndLiterals() {
        assertRenders("A", "{{! comment }}A");
        assertRenders("A", "{{!-- comment }} --}}A");
        assertRenders("{{x}}", "\\{{x}}", "x", "v");
    }

    @Test
    public void testSampleQuery() {
        assertRenders("SELECT * FROM [nt:base] AS s WHERE [sling:resourceType]='foundation/components/text'"
                + "  AND ISDESCENDANTNODE([/content/we-retail])",
                SAMPLE_QUERY, "resourceType", "foundation/components/text", "path", "/content/we-retail");
        assertRenders("SELECT * FROM [nt:base] AS s WHERE [sling:resourceType]='a&#x27;&#x27;b' ",
                SAMPLE_QUERY, "resourceType", "a''b");
    }

    @Test
    public void testMalformedTemplates() {
        assertRejected("{{#if x}}Y");
        assertRejected("Y{{/if}}");
        assertRejected("{{else}}");
        assertRejected("{{x");
        assertRejected("{{{x}}");
        assertRejected("{{!-- comment");
        assertRejected("{{}}");
        assertRejected("{{#if x}}A{{/unless}}");
        assertRejected("{{#if x}}A{{else}}B{{else}}C{{/if}}");
    }

    @Test
    public void testUnsupportedHandlebarsFeatures() {
        assertRejected("{{#each x}}S{{/each}}");
        assertRejected("{{#with x}}S{{/with}}");
        assertRejected("{{#x}}S{{/x}}");
        assertRejected("{{^x}}S{{/x}}");
        assertRejected("{{> part}}");
        assertRejected("{{lookup . 'x'}}");
        assertRejected("{{upper x}}");
        assertRejected("{{#if x y}}A{{/if}}");
        assertRejected("{{#if}}A{{/if}}");
        assertRejected("A {{~x~}} B");
        assertRejected("{{a.b}}");
        assertRejected("{{../x}}");
        assertRejected("{{this}}");
        assertRejected("{{@index}}");
    }
}
