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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateReportCellCSVExporterTest {

  private static final Logger log = LoggerFactory.getLogger(DateReportCellCSVExporterTest.class);

  @Mock
  private Resource mockResource;

  private Calendar start;

  private static final String FORMAT = "yyMMddHHmmssZ";

  @Before
  public void init() {
    log.info("init");
    
    MockitoAnnotations.initMocks(this);

    if (start == null) {
      start = Calendar.getInstance();
    }
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("now", start);
    when(mockResource.getValueMap()).thenReturn(new ValueMapDecorator(properties));
  }
  
  @Test
  public void testExporter() throws IllegalAccessException{
    log.info("testExporter");
    
    DateReportCellCSVExporter exporter = new DateReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "now", true);
    FieldUtils.writeField(exporter, "format", FORMAT, true);
  
    
    String formattedDate = new SimpleDateFormat(FORMAT).format(start.getTime());
    assertEquals(formattedDate, exporter.getValue(mockResource));
    
    log.info("Test successful!");
  }
  

  @Test
  public void testNullProperty() throws IllegalAccessException{
    log.info("testNullProperty");
    
    DateReportCellCSVExporter exporter = new DateReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "someothertime", true);
    FieldUtils.writeField(exporter, "format", FORMAT, true);
  
    assertNull(exporter.getValue(mockResource));
    
    log.info("Test successful!");
  }

  @Test
  public void testDefaultFormatting() throws IllegalAccessException{
    log.info("testDefaultFormatting");
    
    DateReportCellCSVExporter exporter = new DateReportCellCSVExporter();
    FieldUtils.writeField(exporter, "property", "now", true);
    
    String formattedDate = start.getTime().toString();
    assertEquals(formattedDate, exporter.getValue(mockResource));
    
    log.info("Test successful!");
  }
}
