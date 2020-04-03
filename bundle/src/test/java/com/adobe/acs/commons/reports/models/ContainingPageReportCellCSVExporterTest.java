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
package com.adobe.acs.commons.reports.models;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class ContainingPageReportCellCSVExporterTest {

    private static final Logger log = LoggerFactory.getLogger(ContainingPageReportCellCSVExporterTest.class);

    @Mock
    private Resource validResource;
    @Mock
    private Resource invalidResource;

    @Mock
    private PageManager pageManager;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Page page;

    private static final String VALID_PATH = "/content/test";

    @Before
    public void init() {
        log.info("init");

        MockitoAnnotations.initMocks(this);

        when(validResource.getResourceResolver()).thenReturn(resolver);
        when(invalidResource.getResourceResolver()).thenReturn(resolver);
        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getContainingPage(validResource)).thenReturn(page);
        when(page.getPath()).thenReturn(VALID_PATH);

    }

    @Test
    public void testExporter() {
        log.info("testExporter");

        ContainingPageReportCellCSVExporter valid = new ContainingPageReportCellCSVExporter();
        String value = valid.getValue(validResource);
        assertEquals(VALID_PATH, value);

        log.info("Test successful!");
    }

    @Test
    public void testInvalidPage() {
        log.info("testInvalidPage");

        ContainingPageReportCellCSVExporter invalid = new ContainingPageReportCellCSVExporter();
        String value = invalid.getValue(invalidResource);
        assertEquals("", value);

        log.info("Test successful!");
    }

}
