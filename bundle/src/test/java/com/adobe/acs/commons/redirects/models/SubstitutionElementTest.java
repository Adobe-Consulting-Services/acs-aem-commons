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
package com.adobe.acs.commons.redirects.models;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adobe.acs.commons.redirects.models.SubstitutionElement.BackReferenceElement;
import static com.adobe.acs.commons.redirects.models.SubstitutionElement.StaticElement;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class SubstitutionElementTest {

    public String toString(SubstitutionElement[] el) {
        StringBuilder out = new StringBuilder();
        for (SubstitutionElement e : el) {
            out.append(e.toString());
        }
        return out.toString();
    }

    @Test
    public void testParseStatic() {
        String target = "/content/we-retail/en/two";
        SubstitutionElement[] el = SubstitutionElement.parse(target);
        assertEquals(1, el.length);
        assertEquals(SubstitutionElement.StaticElement.class, el[0].getClass());
        assertEquals(target, el[0].toString());
        assertEquals(target, toString(el));

    }

    @Test
    public void testParseBackReferences() {
        String target = "/content/we-retail/$1/two/$2";
        SubstitutionElement[] el = SubstitutionElement.parse(target);
        assertEquals(4, el.length);

        assertEquals(StaticElement.class, el[0].getClass());
        assertEquals("/content/we-retail/", el[0].toString());

        assertEquals(BackReferenceElement.class, el[1].getClass());
        assertEquals("$1", el[1].toString());

        assertEquals(StaticElement.class, el[2].getClass());
        assertEquals("/two/", el[2].toString());

        assertEquals(BackReferenceElement.class, el[3].getClass());
        assertEquals("$2", el[3].toString());

        assertEquals(target, toString(el));

    }

    @Test
    public void testInvalidTrailingBackReference() {
        String target = "/content/we-retail/$1/$"; // trailing digit is missing
        SubstitutionElement[] el = SubstitutionElement.parse(target);
        assertEquals(3, el.length);

        assertEquals(StaticElement.class, el[0].getClass());
        assertEquals("/content/we-retail/", el[0].toString());

        assertEquals(BackReferenceElement.class, el[1].getClass());
        assertEquals("$1", el[1].toString());

        assertEquals(StaticElement.class, el[2].getClass());
        assertEquals("/$", el[2].toString()); // invalid back references fallback to static

        assertEquals(target, toString(el));

    }

    @Test
    public void testInvalidBackReference() {
        String target = "/content/we-retail/$b/$2"; // invalid backref $a
        SubstitutionElement[] el = SubstitutionElement.parse(target);
        assertEquals(4, el.length);

        assertEquals(StaticElement.class, el[0].getClass());
        assertEquals("/content/we-retail/", el[0].toString());

        assertEquals(StaticElement.class, el[1].getClass());
        assertEquals("$b", el[1].toString()); // invalid back references fallback to static

        assertEquals(StaticElement.class, el[2].getClass());
        assertEquals("/", el[2].toString());

        assertEquals(BackReferenceElement.class, el[3].getClass());
        assertEquals("$2", el[3].toString());

        assertEquals(target, toString(el));

    }

    @Test
    public void testEvaluateStatic() {
        String value = "/static/text";
        SubstitutionElement st = new StaticElement(value);
        assertEquals(value, st.evaluate(null));
        assertEquals(value, st.evaluate(Pattern.compile(".*").matcher("/any/string")));

    }

    @Test
    public void testEvaluateBackReference() {
        Matcher m = Pattern.compile("/en/research/(.*)").matcher("/en/research/1/2");
        assertTrue(m.matches());
        SubstitutionElement st = new BackReferenceElement(1);
        assertEquals("1/2", st.evaluate(m));

    }

    /**
     * referencing a  non-existing group should return an empty string
     */
    @Test
    public void testEvaluateBackReferenceOoutOfBounds() {
        Matcher m = Pattern.compile("/en/research/(.*)").matcher("/en/research/1/2");
        assertTrue(m.matches());
        SubstitutionElement st = new BackReferenceElement(5);
        assertEquals("", st.evaluate(m));

    }
}
