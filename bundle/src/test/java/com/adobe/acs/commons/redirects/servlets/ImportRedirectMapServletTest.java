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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.getRules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ImportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ImportRedirectMapServlet servlet;
    private String redirectStoragePath = "/conf/acs-commons/redirects";
    Calendar calendar = new Calendar.Builder().setDate(1974, 01, 16).build();
    private List<RedirectRule> savedRules = Arrays.asList(
            new RedirectRule("/content/one", "/content/two", 302, calendar, "note-1"),
            new RedirectRule("/content/three", "/content/four", 301, null, "")
    );
    private List<RedirectRule> excelRules = Arrays.asList(
            new RedirectRule("/content/1", "/en/we-retail", 301, calendar, "note-2"),
            new RedirectRule("/content/2", "/en/we-retail", 301, null, ""),
            new RedirectRule("/content/three", "/en/we-retail", 301, null, "")
    );
    private byte[] excelBytes;

    @Before
    public void setUp() {
        servlet = new ImportRedirectMapServlet();
        ResourceBuilder rb = context.build().resource(redirectStoragePath).siblingsMode();
        int idx = 0;
        for (RedirectRule rule : savedRules) {
            rb.resource("redirect-" + (++idx),
                    "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                    RedirectRule.SOURCE_PROPERTY_NAME, rule.getSource(),
                    RedirectRule.TARGET_PROPERTY_NAME, rule.getTarget(),
                    RedirectRule.STATUS_CODE_PROPERTY_NAME, rule.getStatusCode(),
                    RedirectRule.UNTIL_DATE_PROPERTY_NAME, rule.getUntilDate() == null ? null : GregorianCalendar.from(rule.getUntilDate()),
                    RedirectRule.NOTE_PROPERTY_NAME, rule.getNote()
            );
        }
        context.request().addRequestParameter("path", redirectStoragePath);

    }

    @Test
    public void testPost() throws ServletException, IOException {
        XSSFWorkbook wb = ExportRedirectMapServlet.export(excelRules);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        excelBytes = out.toByteArray();

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("file", excelBytes, "binary/data");

        servlet.doPost(request, response);

        Resource storage = context.resourceResolver().getResource(redirectStoragePath);
        Map<String, RedirectRule> rules = getRules(storage)
                .stream().collect(Collectors.toMap(RedirectRule::getSource, r -> r,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new)); // rules keyed by source
        assertEquals("number of redirects after import ",4, rules.size());

        RedirectRule rule1 = rules.get("/content/one");
        assertEquals("/content/two", rule1.getTarget());
        assertDateEquals("16 February 1974", rule1.getUntilDate());
        assertEquals("note-1", rule1.getNote());

        RedirectRule rule2 = rules.get("/content/three");
        assertEquals("/en/we-retail", rule2.getTarget());
        assertEquals(301, rule2.getStatusCode());

        RedirectRule rule3 = rules.get("/content/1");
        assertEquals("/en/we-retail", rule3.getTarget());
        assertDateEquals("16 February 1974", rule3.getUntilDate());
        assertEquals("note-2", rule3.getNote());

        RedirectRule rule4 = rules.get("/content/2");
        assertEquals("/en/we-retail", rule4.getTarget());
        assertEquals(null, rule4.getUntilDate());

    }

    @Test
    public void testReadEntries() throws IOException {
        XSSFWorkbook wb = ExportRedirectMapServlet.export(excelRules);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        excelBytes = out.toByteArray();

        Collection<RedirectRule> entries = servlet.readEntries(new ByteArrayInputStream(excelBytes));
        assertEquals(excelRules.size(), entries.size());

        Iterator<RedirectRule> it = entries.iterator();
        int idx = 0;
        while (it.hasNext()) {
            RedirectRule rule = it.next();
            assertEquals(excelRules.get(idx).getSource(), rule.getSource());
            assertEquals(excelRules.get(idx).getTarget(), rule.getTarget());
            assertEquals(excelRules.get(idx).getStatusCode(), rule.getStatusCode());
            assertEquals(excelRules.get(idx).getNote(), rule.getNote());
            ZonedDateTime untilDateTime = excelRules.get(idx).getUntilDate();
             if (untilDateTime != null) {
                // importer converts input date to dd MMMM yyyy
                assertEquals(untilDateTime, rule.getUntilDate());
            }
            idx++;
        }
    }

    @Test
    public void testUpdate() throws IOException {
        Resource root = context.resourceResolver().getResource(redirectStoragePath);
        RedirectRule rule1 = new RedirectRule("/a1", "/b1", 301, null, null);
        RedirectRule rule2 = new RedirectRule("/a2", "/b2", 302, Calendar.getInstance(), "note");
        Collection<RedirectRule> rules = Arrays.asList(rule1, rule2);
        servlet.update(root, rules);

        ValueMap vm1 = root.getChild("redirect-rule-1").getValueMap();
        assertEquals(vm1.get(RedirectRule.SOURCE_PROPERTY_NAME), rule1.getSource());
        assertEquals(vm1.get(RedirectRule.TARGET_PROPERTY_NAME), rule1.getTarget());
        assertFalse(vm1.containsKey(RedirectRule.UNTIL_DATE_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.NOTE_PROPERTY_NAME));

        ValueMap vm2 = root.getChild("redirect-rule-2").getValueMap();
        assertEquals(vm2.get(RedirectRule.SOURCE_PROPERTY_NAME), rule2.getSource());
        assertEquals(vm2.get(RedirectRule.TARGET_PROPERTY_NAME), rule2.getTarget());
        assertEquals(vm2.get(RedirectRule.NOTE_PROPERTY_NAME), rule2.getNote());
    }

}
