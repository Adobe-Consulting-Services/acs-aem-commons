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

import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static junit.framework.TestCase.assertTrue;
import static junitx.framework.Assert.assertFalse;
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
                "note", "note-1",
                "contextPrefixIgnored", true,
                "evaluateURI", true);

        RedirectRule rule = resource.adaptTo(RedirectRule.class);
        assertEquals("/content/we-retail/en/one", rule.getSource());
        assertEquals("/content/we-retail/en/two", rule.getTarget());
        assertEquals(302, rule.getStatusCode());
        assertDateEquals("11 January 2021", rule.getUntilDate());
        assertEquals("note-1", rule.getNote());
        assertTrue(rule.getContextPrefixIgnored());
        assertTrue(rule.getEvaluateURI());
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

        RedirectRule rule = resource.adaptTo(RedirectRule.class);
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

        RedirectRule rule = resource.adaptTo(RedirectRule.class);
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

        RedirectRule rule1 = resource1.adaptTo(RedirectRule.class);
        RedirectRule rule2 = resource2.adaptTo(RedirectRule.class);

        assertTrue(rule1.equals(rule2));
        assertTrue(rule1.hashCode() == rule2.hashCode());

    }

    @Test
    public void testState() throws PersistenceException {
        Resource res1 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302).build();

        RedirectRule rule1 = res1.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.ACTIVE, rule1.getState());

        Calendar dateInFuture = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));
        Resource res2 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setEffectiveFrom(dateInFuture)
                .build();

        RedirectRule rule2 = res2.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.PENDING, rule2.getState());

        Calendar dateInPast = GregorianCalendar.from(ZonedDateTime.now().minusDays(1));
        Resource res3 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setEffectiveFrom(dateInPast)
                .build();

        RedirectRule rule3 = res3.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.ACTIVE, rule3.getState());

        Resource res4 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setUntilDate(dateInPast)
                .build();

        RedirectRule rule4 = res4.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.EXPIRED, rule4.getState());

        Resource res5 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setUntilDate(dateInFuture)
                .build();

        RedirectRule rule5 = res5.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.ACTIVE, rule5.getState());

        Resource res6 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setEffectiveFrom(dateInPast)
                .setUntilDate(dateInFuture)
                .build();

        RedirectRule rule6 = res6.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.ACTIVE, rule6.getState());

        Resource res7 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302)
                .setEffectiveFrom(dateInFuture)
                .setUntilDate(dateInPast)
                .build();

        RedirectRule rule7 = res7.adaptTo(RedirectRule.class);
        assertEquals(RedirectState.INVALID, rule7.getState());
    }

    @Test
    public void testIsPublished() throws PersistenceException {
        Resource res1 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/contact-us")
                .setTarget("/content/geometrixx/en/contact-them")
                .setStatusCode(302).build();

        RedirectRule rule1 = res1.adaptTo(RedirectRule.class);
        assertFalse(rule1.isPublished()); // never published

        Calendar dateInPast = GregorianCalendar.from(ZonedDateTime.now().minusDays(1));
        res1.getParent().adaptTo(ModifiableValueMap.class).put("cq:lastReplicated", dateInPast);
        assertTrue(rule1.isPublished());

        Resource res2 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/1")
                .setTarget("/content/geometrixx/en/2")
                .setStatusCode(302)
                .setCreated(GregorianCalendar.from(ZonedDateTime.now().minusMinutes(60)))
                .build();
        RedirectRule rule2 = res2.adaptTo(RedirectRule.class);
        assertFalse(rule2.isPublished()); // jcr:created after lastReplicated

        Resource res3 = new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/1")
                .setTarget("/content/geometrixx/en/2")
                .setStatusCode(302)
                .setCreated(GregorianCalendar.from(ZonedDateTime.now().minusMinutes(120)))
                .setModified(GregorianCalendar.from(ZonedDateTime.now().minusMinutes(60)))
                .build();
        RedirectRule rule3 = res3.adaptTo(RedirectRule.class);
        assertFalse(rule3.isPublished()); // jcr:lastModified after lastReplicated

        // update cq:lastReplicated, all child redirects should become active
        res1.getParent().adaptTo(ModifiableValueMap.class).put("cq:lastReplicated", Calendar.getInstance());
        assertTrue(rule1.isPublished());
        assertTrue(rule2.isPublished());
        assertTrue(rule3.isPublished());
    }
}
