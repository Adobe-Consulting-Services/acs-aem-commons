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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.http.entity.ContentType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static com.adobe.acs.commons.redirects.servlets.ExportRedirectMapServlet.SPREADSHEETML_SHEET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Yegor Kozlov
 */
public class RewriteMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private RewriteMapServlet servlet;
    private final String redirectStoragePath = "/conf/acs-commons/redirects";

    @Before
    public void setUp() throws PersistenceException {
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/one")
                .setTarget("/content/two")
                .setStatusCode(302)
                .setUntilDate(new Calendar.Builder().setDate(2022, 9, 9).build())
                .setEffectiveFrom(new Calendar.Builder().setDate(2025, 2, 2).build())
                .setNotes("note-1")
                .setEvaluateURI(true)
                .setContextPrefixIgnored(true)
                .setTagIds(new String[]{"redirects:tag1"})
                .setCreatedBy("john.doe")
                .setModifiedBy("jane.doe")
                .setCreated(new Calendar.Builder().setDate(1974, 1, 16).build())
                .setModified(new Calendar.Builder().setDate(1976, 10, 22).build())
                .build();
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/three")
                .setTarget("/content/four")
                .setStatusCode(301)
                .setTagIds(new String[]{"redirects:tag2"})
                .setModifiedBy("john.doe")
                .build();

        Resource redirects = context.resourceResolver().getResource(redirectStoragePath);
        context.request().setResource(redirects);
        servlet = new RewriteMapServlet();
    }


    @Test
    public void testGet() throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(ContentType.TEXT_PLAIN.getMimeType(), response.getContentType());
        String[] lines = response.getOutputAsString().split("\n");
        assertEquals("# Redirect Map File", lines[0]);
        assertEquals("# note-1", lines[1]);

        String[] rule1 = lines[2].split(" ");
        assertEquals("/content/one", rule1[0]);
        assertEquals("/content/two", rule1[1]);

        String[] rule2 = lines[3].split(" ");
        assertEquals("/content/three", rule2[0]);
        assertEquals("/content/four", rule2[1]);
    }
}