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
package com.adobe.acs.commons.mcp.model;

import org.junit.Test;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

public class ReportPageTest {
    @Test
    public void slicesAndClampsPages() throws Exception {
        GenericReport report = new GenericReport();
        for (int i = 0; i < 205; i++) {
            report.getRows().add(new ValueMapDecorator(Collections.singletonMap("row", i)));
        }
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        when(request.getResource()).thenReturn(resource);
        when(resource.adaptTo(GenericReport.class)).thenReturn(report);
        when(request.getParameter("page")).thenReturn("1");
        when(request.getRequestURI()).thenReturn("/report.html");
        ReportPage model = new ReportPage();
        FieldUtils.writeField(model, "request", request, true);
        model.init();
        assertEquals(100, model.getRows().size());
        assertEquals(100, model.getRows().get(0).get("row"));
        assertEquals(101, model.getFirstRow());
        assertEquals(200, model.getLastRow());
        assertTrue(model.isHasPrevious());
        assertTrue(model.isHasNext());
        assertEquals("/report.html?page=2&pageSize=100", model.getNextUrl());
        when(request.getParameter("page")).thenReturn("2147483647");
        model.init();
        assertEquals(3, model.getPageNumber());
        assertEquals(5, model.getRows().size());
        assertFalse(model.isHasNext());
        assertEquals(205, report.getRows().size());
        report.getRows().clear();
        model.init();
        assertEquals(1, model.getPageCount());
        assertEquals(0, model.getFirstRow());
        assertEquals(0, model.getLastRow());
        assertTrue(model.getRows().isEmpty());
    }

    @Test
    public void validatesPageParameters() {
        assertEquals(0, ReportPage.parsePage(null));
        assertEquals(0, ReportPage.parsePage("invalid"));
        assertEquals(0, ReportPage.parsePage("-1"));
        assertEquals(0, ReportPage.parsePage("9999999999999999"));
        assertEquals(2, ReportPage.parsePage("2"));
        assertEquals(100, ReportPage.parsePageSize(null));
        assertEquals(100, ReportPage.parsePageSize("0"));
        assertEquals(100, ReportPage.parsePageSize("-1"));
        assertEquals(500, ReportPage.parsePageSize("501"));
        assertEquals(25, ReportPage.parsePageSize("25"));
    }
}
