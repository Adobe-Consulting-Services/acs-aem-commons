/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.redirects.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;
import java.util.Map;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class RedirectRuleTest {
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Test
    public void testCreateFromResource() {
        Calendar untilDate = new Calendar.Builder().setDate(2021, 0, 11).build();
        Resource resource = context.create().resource("/var/acs-commons/redirects/rule",
                "source", "/content/we-retail/en/one",
                "target", "/content/we-retail/en/two",
                "statusCode", 302,
                "untilDate", untilDate,
                "note", "note-1");

        RedirectRule rule = RedirectRule.from(resource.getValueMap());
        assertEquals("/content/we-retail/en/one", rule.getSource());
        assertEquals("/content/we-retail/en/two", rule.getTarget());
        assertEquals(302, rule.getStatusCode());
        assertDateEquals("11 January 2021", rule.getUntilDate());
        assertEquals("note-1", rule.getNote());

    }

    /**
     * v5.0.4 saved Until Date as a 'dd MMMM yyyy' string.
     * These should be converted into JCR Date by {@link UpgradeLegacyRedirects}
     * If a value was not converted then RedirectRule#getUntilDate returns null
     */
    @Test
    public void testCreateFromResourceDateString() {
        Resource resource = context.create().resource("/var/acs-commons/redirects/rule",
                "source", "/content/we-retail/en/one",
                "target", "/content/we-retail/en/two",
                "statusCode", 302,
                "untilDate", "11 January 2021");

        RedirectRule rule = RedirectRule.from(resource.getValueMap());
        assertEquals("/content/we-retail/en/one", rule.getSource());
        assertEquals("/content/we-retail/en/two", rule.getTarget());
        assertEquals(302, rule.getStatusCode());
        assertEquals(null, rule.getUntilDate());

    }

    @Test
    public void testInvalidUntilDate() {
        Resource resource = context.create().resource("/var/acs-commons/redirects/rule",
                "source", "/content/we-retail/en/one",
                "target", "/content/we-retail/en/two",
                "statusCode", 302,
                "untilDate", "11 xxx 2021");

        RedirectRule rule = RedirectRule.from(resource.getValueMap());
        assertEquals(null, rule.getUntilDate());

    }

    @Test
    public void testEquals() {
        Map<String, Object> props = MapUtil.toMap("source", "/content/we-retail/en/one",
                "target", "/content/we-retail/en/two",
                "statusCode", 302,
                "untilDate", "11 January 2021");

        Resource resource1 = context.create().resource("/var/acs-commons/redirects/rule1", props);

        Resource resource2 = context.create().resource("/var/acs-commons/redirects/rule2", props);

        RedirectRule rule1 = RedirectRule.from(resource1.getValueMap());
        RedirectRule rule2 = RedirectRule.from(resource2.getValueMap());

        assertTrue(rule1.equals(rule2));
        assertTrue(rule1.hashCode() == rule2.hashCode());

    }
}
