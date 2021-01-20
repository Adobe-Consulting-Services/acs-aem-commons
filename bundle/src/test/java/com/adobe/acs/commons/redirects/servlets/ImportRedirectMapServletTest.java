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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;

public class ImportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ImportRedirectMapServlet servlet;
    private String redirectStoragePath = "/var/redirects";
    private List<RedirectRule> savedRules = Arrays.asList(
            new RedirectRule("/content/one", "/content/two", 302, "16 February 1974"),
            new RedirectRule("/content/three", "/content/four", 301, null)
    );
    private List<RedirectRule> excelRules = Arrays.asList(
            new RedirectRule("/content/1", "/en/we-retail", 301, "16 February 1974"),
            new RedirectRule("/content/2", "/en/we-retail", 301, "16-02-1974"),
            new RedirectRule("/content/3", "/en/we-retail", 301, null)
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
                    RedirectRule.SOURCE, rule.getSource(),
                    RedirectRule.TARGET, rule.getTarget(),
                    RedirectRule.STATUS_CODE, rule.getStatusCode(),
                    RedirectRule.UNTIL_DATE, rule.getUntilDate()
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
        int count = 0;
        for (Resource r : storage.getChildren()) {
            RedirectRule rule = new RedirectRule(r.getValueMap());
            switch (rule.getSource()) {
                case "/content/one":
                    assertEquals("16 February 1974", rule.getUntilDate());
                    assertEquals(rule.getUntilDate(), RedirectRule.DATE_FORMATTER.format(rule.getUntilDateTime()));
                    break;
                case "/content/not-here-1":
                    assertEquals("22 November 1976", rule.getUntilDate());
                    assertEquals(rule.getUntilDate(), RedirectRule.DATE_FORMATTER.format(rule.getUntilDateTime()));
                    break;
                case "/content/not-here-2":
                    assertEquals("Invalid Date", rule.getUntilDate());
                    assertEquals(null, rule.getUntilDateTime());
                    break;
            }
            count++;
        }
        assertEquals("number of redirects ",savedRules.size() + excelRules.size(), count);
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
            ZonedDateTime untilDateTime = excelRules.get(idx).getUntilDateTime();
            LocalDate dt = untilDateTime == null ? null : untilDateTime.query(LocalDate::from);
            if (dt != null) {
                // importer converts input date to dd MMMM yyyy
                assertEquals(RedirectRule.DATE_FORMATTER.format(dt), rule.getUntilDate());
            }
            idx++;
        }
    }

    @Test
    public void testUpdate() throws IOException {
        Resource root = context.resourceResolver().getResource(redirectStoragePath);
        Collection<RedirectRule> rules = Arrays.asList();
        servlet.update(root, rules);
    }

}
