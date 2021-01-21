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

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.Collection;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.servlets.ExportRedirectMapServlet.SPREADSHEETML_SHEET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Yegor Kozlov
 */
public class ExportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ExportRedirectMapServlet servlet;
    private String redirectStoragePath = "/conf/acs-commons/redirects";

    @Before
    public void setUp() {
        servlet = new ExportRedirectMapServlet();
        context.build().resource(redirectStoragePath)
                .siblingsMode()
                .resource("redirect-1",                         "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                        "source", "/content/one", "target", "/content/two", "statusCode", 302)
                .resource("redirect-2",                         "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                        "source", "/content/three", "target", "/content/four", "statusCode", 301)
        ;
        context.request().addRequestParameter("path", redirectStoragePath);
    }


    @Test
    public void testGet() throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(SPREADSHEETML_SHEET, response.getContentType());
        // read the generated spreadsheet
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getOutput()));
        assertSpreadsheet(wb);
    }

    @Test
    public void testExport() {
        Resource resource = context.resourceResolver().getResource(redirectStoragePath);
        Collection<RedirectRule> rules = RedirectFilter.getRules(resource);

        XSSFWorkbook wb = servlet.export(rules);
        assertSpreadsheet(wb);
    }

    public void assertSpreadsheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.getSheet("Redirects");
        assertNotNull(sheet);
        XSSFRow row1 = sheet.getRow(1);
        assertEquals("/content/one", row1.getCell(0).getStringCellValue());
        assertEquals("/content/two", row1.getCell(1).getStringCellValue());
        assertEquals(302, (int) row1.getCell(2).getNumericCellValue());
        XSSFRow row2 = sheet.getRow(2);
        assertEquals("/content/three", row2.getCell(0).getStringCellValue());
        assertEquals("/content/four", row2.getCell(1).getStringCellValue());
        assertEquals(301, (int) row2.getCell(2).getNumericCellValue());
    }
}