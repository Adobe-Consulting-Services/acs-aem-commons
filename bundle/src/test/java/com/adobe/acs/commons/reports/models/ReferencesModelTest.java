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
package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.internal.DelimiterConfiguration;
import com.adobe.acs.commons.reports.models.references.MockReferenceList;
import com.adobe.acs.commons.reports.models.references.MockReferencesAggregator;
import com.adobe.granite.references.Reference;
import com.adobe.granite.references.ReferenceAggregator;
import com.adobe.granite.references.ReferenceList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ReferencesModelTest {

  private static final Logger log = LoggerFactory.getLogger(ReferencesModelTest.class);

  private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
    put("field.delimiter", ",");
    put("multi.value.delimiter", ";");
  }};

  @Mock
  private DelimiterConfiguration delimiterConfiguration;

  @Mock
  private Resource mockResourceSource;

  @Mock
  private Resource mockResourceTarget;

  private ReferencesModel referencesModel;

  @Before
  public void init()
      throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    log.info("init");

    MockitoAnnotations.initMocks(this);

    when(mockResourceSource.getPath()).thenReturn("/source");
    when(mockResourceTarget.getPath()).thenReturn("/target");

    when(delimiterConfiguration.getMultiValueDelimiter()).thenReturn(CONFIG.get("multi.value.delimiter"));

    ReferenceList references = new MockReferenceList(mockResourceSource);
    references.add(new Reference(mockResourceSource, mockResourceTarget, "VALID"));
    references.add(new Reference(mockResourceSource, mockResourceSource, "INVALID"));

    ReferenceAggregator aggregator = new MockReferencesAggregator(references);

    referencesModel = new ReferencesModel(mockResourceSource, delimiterConfiguration);
    Field af = ReferencesModel.class.getDeclaredField("aggregator");
    af.setAccessible(true);
    af.set(referencesModel, aggregator);
    referencesModel.init();
  }

  @Test
  public void testGetReferences() throws IllegalAccessException {
    log.info("testGetReferences");

    assertNotNull(referencesModel.getReferences());
    assertEquals(1, referencesModel.getReferences().size());
    assertEquals("/target", referencesModel.getReferences().get(0).getTarget().getPath());
    assertEquals("VALID", referencesModel.getReferences().get(0).getType());

    log.info("Test Successful!");
  }

  @Test
  public void testGetValue() throws IllegalAccessException {
    log.info("testGetValue");

    assertNotNull(referencesModel.getValue(mockResourceSource));
    assertEquals("VALID - /target", referencesModel.getValue(mockResourceSource));

    log.info("Test Successful!");
  }

  @Test
  public void testGetValueDelimiter() throws Exception {
    ReferenceList references = new MockReferenceList(mockResourceSource);
    references.add(new Reference(mockResourceSource, mockResourceTarget, "VALID"));
    references.add(new Reference(mockResourceSource, mockResourceTarget, "VALID2"));

    ReferenceAggregator aggregator = new MockReferencesAggregator(references);

    referencesModel = new ReferencesModel(mockResourceSource, delimiterConfiguration);
    Field af = ReferencesModel.class.getDeclaredField("aggregator");
    af.setAccessible(true);
    af.set(referencesModel, aggregator);
    referencesModel.init();

    String delimitedValue = referencesModel.getValue(mockResourceSource);
    assertNotNull(delimitedValue);
    assertTrue(delimitedValue.contains(delimiterConfiguration.getMultiValueDelimiter()));
  }
}
