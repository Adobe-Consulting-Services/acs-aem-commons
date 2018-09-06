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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportRunnerTest {

    private static final Logger log = LoggerFactory.getLogger(ReportRunnerTest.class);

    @Mock
    private Resource configParentResource;

    @Mock
    private Resource contentResource;

    @Mock
    Resource oldMaid;

    @Mock
    private Resource configResource;

    @Mock
    private Resource noClassConfigResource;

    @Mock
    private Resource invalidClassConfigResource;

    @Mock
    private Resource incorrectClassConfigResource;

    @Mock
    private SlingHttpServletRequest validRequest;

    @Mock
    private SlingHttpServletRequest invalidRequest;

    @Mock
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private MockReportExecutor exec = new MockReportExecutor();

    @Before
    public void init() {
        log.info("init");

        MockitoAnnotations.initMocks(this);

        when(invalidRequest.getParameter("page")).thenReturn("alpha");
        when(invalidRequest.getResource()).thenReturn(oldMaid);

        when(validRequest.getResource()).thenReturn(contentResource);
        when(validRequest.getParameter("page")).thenReturn("1");
        when(validRequest.adaptTo(MockReportExecutor.class)).thenReturn(exec);
        when(contentResource.getChild("config")).thenReturn(configParentResource);

        Iterator<Resource> it = Arrays.asList(new Resource[] { noClassConfigResource, invalidClassConfigResource,
                incorrectClassConfigResource, configResource }).iterator();
        when(configParentResource.listChildren()).thenReturn(it);

        when(configResource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put(ReportRunner.PN_EXECUTOR, MockReportExecutor.class.getName());
            }
        }));

        when(noClassConfigResource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));

        when(invalidClassConfigResource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put(ReportRunner.PN_EXECUTOR, "not.a.class.CLAZZ");
            }
        }));

        when(incorrectClassConfigResource.getValueMap())
                .thenReturn(new ValueMapDecorator(new HashMap<String, Object>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(ReportRunner.PN_EXECUTOR, "java.lang.String");
                    }
                }));

        when(dynamicClassLoaderManager.getDynamicClassLoader()).thenReturn(this.getClass().getClassLoader());

    }

    @Test
    public void testInvalid() throws RepositoryException {
        log.info("testInvalid");
        ReportRunner reportRunner = new ReportRunner(invalidRequest, dynamicClassLoaderManager);
        reportRunner.init();
        assertEquals("No configurations found!", reportRunner.getFailureMessage());
        assertFalse(reportRunner.isSuccessful());
        log.info("Test Succeeded!");
    }

    @Test
    public void testReportRunner() throws RepositoryException {
        log.info("testReportRunner");
        ReportRunner reportRunner = new ReportRunner(validRequest, dynamicClassLoaderManager);
        reportRunner.init();
        assertTrue(reportRunner.isSuccessful());
        assertNull(reportRunner.getFailureMessage());

        assertNotNull(reportRunner.getReportExecutor());
        assertEquals(exec, reportRunner.getReportExecutor());

        log.info("Test Succeeded!");
    }

}
