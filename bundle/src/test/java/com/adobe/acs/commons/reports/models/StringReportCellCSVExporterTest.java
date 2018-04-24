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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.MapEntryTest;

public class StringReportCellCSVExporterTest {

  private static final String[] ARRAY_VALUE = new String[] { "val1", "val2" };

  private static final Logger log = LoggerFactory.getLogger(MapEntryTest.class);

  @Mock
  private Resource mockResource;

  @Before
  public void init() {
    log.info("init");
    
    MockitoAnnotations.initMocks(this);

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("multiple", ARRAY_VALUE);
    properties.put("single", ARRAY_VALUE[0]);
    when(mockResource.getValueMap()).thenReturn(new ValueMapDecorator(properties));
  }

  @Test
  public void testMultiple() throws IllegalAccessException {
    log.info("testMultiple");
    StringReportCellCSVExporter exporter = new StringReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "multiple", true);
    assertEquals(StringUtils.join(ARRAY_VALUE, ";"), exporter.getValue(mockResource));
    log.info("Test successful!");
  }

  @Test
  public void testNotFound() throws IllegalAccessException {
    log.info("testNotFound");
    StringReportCellCSVExporter exporter = new StringReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "somethingelse", true);
    assertEquals("", exporter.getValue(mockResource));
    log.info("Test successful!");
  }

  @Test
  public void testSingle() throws IllegalAccessException {
    log.info("testSingle");
    StringReportCellCSVExporter exporter = new StringReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "single", true);
    assertEquals(ARRAY_VALUE[0], exporter.getValue(mockResource));
    log.info("Test successful!");
  }
}
