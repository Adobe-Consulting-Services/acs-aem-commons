/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.model.GenericReport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class TestGenericReportExcelServlet {
    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    
    @Test
    public void testReport() throws Exception {
        int numRows = 10;
        String reportPath = "/var/acs-commons/mcp/instances/junit/jcr:content/report";
        ResourceBuilder rb = slingContext.build()
                .resource(reportPath,
                        "columns", new String[]{"ColumnA", "ColumnB"},
                        "name", "report",
                        "sling:resourceType", "acs-commons/components/utilities/process-instance/process-generic-report")
                .resource("rows");
        rb.siblingsMode();
        for (int i = 1; i <= numRows; i++) {
            rb.resource("row-" + i,
                    "ColumnA", "abcdef-" + i, "ColumnB", "qwerty-" + i);
        }
        MockSlingHttpServletRequest request = slingContext.request();
        request.setResource(slingContext.resourceResolver().getResource(reportPath));
        MockSlingHttpServletResponse response = slingContext.response();

        slingContext.addModelsForClasses(GenericReport.class);

        GenericReportExcelServlet servlet = new GenericReportExcelServlet();

        servlet.doGet(request, response);

        assertEquals("application/vnd.ms-excel", response.getContentType());

        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getOutput()));
        Sheet sh = wb.getSheetAt(0);
        assertEquals(numRows, sh.getLastRowNum());
        Row header = sh.getRow(0);
        assertEquals("Column A", header.getCell(0).getStringCellValue());
        assertEquals("Column B", header.getCell(1).getStringCellValue());
        for (int i = 1; i <= numRows; i++) {
            Row row = sh.getRow(i);
            assertEquals("abcdef-" + i, row.getCell(0).getStringCellValue());
            assertEquals("qwerty-" + i, row.getCell(1).getStringCellValue());
        }

    }
}
