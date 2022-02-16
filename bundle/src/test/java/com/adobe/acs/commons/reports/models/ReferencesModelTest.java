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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.models.references.MockReferenceList;
import com.adobe.acs.commons.reports.models.references.MockReferencesAggregator;
import com.adobe.granite.references.Reference;
import com.adobe.granite.references.ReferenceAggregator;
import com.adobe.granite.references.ReferenceList;

public class ReferencesModelTest {

  private static final Logger log = LoggerFactory.getLogger(ReferencesModelTest.class);

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

    ReferenceList references = new MockReferenceList(mockResourceSource);
    references.add(new Reference(mockResourceSource, mockResourceTarget, "VALID"));
    references.add(new Reference(mockResourceSource, mockResourceSource, "INVALID"));

    ReferenceAggregator aggregator = new MockReferencesAggregator(references);

    referencesModel = new ReferencesModel(mockResourceSource);
    Field af = ReferencesModel.class.getDeclaredField("aggregator");
    af.setAccessible(true);
    af.set(referencesModel, aggregator);
    referencesModel.init();
  }

  @Test
  public void testGetReferences() throws IllegalAccessException {
    log.info("testGetReferences");

    assertNotNull(referencesModel.getReferences());
    assertTrue(1 == referencesModel.getReferences().size());
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
}
