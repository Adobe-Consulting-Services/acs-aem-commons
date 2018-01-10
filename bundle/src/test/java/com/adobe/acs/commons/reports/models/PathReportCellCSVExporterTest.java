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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathReportCellCSVExporterTest {

  private static final Logger log = LoggerFactory.getLogger(PathReportCellCSVExporterTest.class);

  @Mock
  private Resource mockResource;

  private static final String PATH = "/content/acs-aem-commons";

  @Before
  public void init() {
    log.info("init");
    
    MockitoAnnotations.initMocks(this);

    when(mockResource.getPath()).thenReturn(PATH);
  }

  @Test
  public void testExporter() throws IllegalAccessException {
    log.info("testExporter");

    PathReportCellCSVExporter exporter = new PathReportCellCSVExporter();

    String value = exporter.getValue(mockResource);
    assertEquals(PATH, value);

    log.info("Test successful!");
  }
}
