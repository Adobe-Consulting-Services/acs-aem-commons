/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.marketo.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.acs.commons.marketo.MarketoForm;

import io.wcm.testing.mock.aem.junit.AemContext;


public class TestMarketoFormImpl {

  @Rule
  public final AemContext context = new AemContext();

  @Before
  public void init() {
    context.addModelsForPackage("com.adobe.acs.commons.marketo", "com.adobe.acs.commons.marketo.impl");
    context.load().json("/com/adobe/acs/commons/marketo/pages.json", "/content/page");
    context.load().json("/com/adobe/acs/commons/marketo/cloudconfig.json", "/conf/test");

    ConfigurationResourceResolver configrr = Mockito.mock(ConfigurationResourceResolver.class);
    Mockito.when(configrr.getResourceCollection(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Collections.singletonList(context.resourceResolver().getResource("/conf/test")));
    context.registerService(ConfigurationResourceResolver.class, configrr);
  }

  @Test
  public void testConfig() {
    Resource resource = context.resourceResolver().getResource("/content/page/about-us/jcr:content/root/marketo-form");
    context.request().setResource(resource);
    MarketoForm mfc = context.request().adaptTo(MarketoForm.class);
    assertNotNull(mfc);
    assertNotNull(mfc.getFormId());
    assertNull(mfc.getScript());
    assertNull(mfc.getSuccessUrl());
    assertNull(mfc.getValues());
  }

  @Test
  public void testAdvancedConfig() {
    Resource resource = context.resourceResolver()
        .getResource("/content/page/about-us/jcr:content/root/marketo-form-1");
    context.request().setResource(resource);
    MarketoForm mfc = context.request().adaptTo(MarketoForm.class);
    assertNotNull(mfc);
    assertEquals("123", mfc.getFormId());
    assertEquals("alert('hi')", mfc.getScript());
    assertNotNull(mfc.getSuccessUrl());
    assertNotNull(mfc.getValues());
    assertNotNull(mfc.getHidden());
    assertFalse(mfc.isEdit());
  }

  @Test
  public void testInvalidResource() {
    Resource resource = context.resourceResolver().getResource("/content/page/about-us/jcr:content/root");
    context.request().setResource(resource);
    MarketoForm mfc = context.request().adaptTo(MarketoForm.class);
    assertNotNull(mfc);
    assertNull(mfc.getFormId());
  }
}
