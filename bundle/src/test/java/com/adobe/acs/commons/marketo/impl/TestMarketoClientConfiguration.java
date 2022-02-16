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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import io.wcm.testing.mock.aem.junit.AemContext;

public class TestMarketoClientConfiguration {

  @Rule
  public final AemContext context = new AemContext();
  private ConfigurationResourceResolver configrr;

  @Before
  public void init() {
    context.addModelsForPackage("com.adobe.acs.commons.marketo", "com.adobe.acs.commons.marketo.impl");
    context.load().json("/com/adobe/acs/commons/marketo/pages.json", "/content/page");
    context.load().json("/com/adobe/acs/commons/marketo/cloudconfig.json", "/conf/test");

    configrr = Mockito.mock(ConfigurationResourceResolver.class);
    
    Mockito.when(configrr.getResourceCollection(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Collections.singletonList(context.resourceResolver().getResource("/conf/test/default")));
    context.registerService(ConfigurationResourceResolver.class, configrr);

  }

  @Test
  public void testConfigMgr() throws LoginException {
    context.request().setResource(context.resourceResolver().getResource("/content/page"));
    ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
    when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(context.resourceResolver());

    MarketoClientConfigurationManagerImpl mccm = new MarketoClientConfigurationManagerImpl(context.request(), configrr, resolverFactory);

    assertNotNull(mccm);
    assertNotNull(mccm.getConfiguration());
  }

  @Test
  public void testConfig() {
    MarketoClientConfiguration mcc = Optional
        .ofNullable(context.resourceResolver().getResource("/conf/test/default/jcr:content"))
        .map(r -> r.adaptTo(MarketoClientConfiguration.class)).orElse(null);
    assertNotNull(mcc);
    assertEquals("123", mcc.getClientId());
    assertEquals("456", mcc.getClientSecret());
    assertEquals("test.mktorest.com", mcc.getEndpointHost());
    assertEquals("123-456-789", mcc.getMunchkinId());
    assertEquals("//test.marketo.com", mcc.getServerInstance());
    assertEquals(48721, mcc.hashCode());

    MarketoClientConfiguration mcc2 = Optional
        .ofNullable(context.resourceResolver().getResource("/conf/test/default/jcr:content"))
        .map(r -> r.adaptTo(MarketoClientConfiguration.class)).orElse(null);
    assertEquals(mcc, mcc2);
    assertNotEquals(mcc, null);
  }


  @Test
  public void testHttps() {
    MarketoClientConfiguration mcc = Optional
        .ofNullable(context.resourceResolver().getResource("/conf/test/https/jcr:content"))
        .map(r -> r.adaptTo(MarketoClientConfiguration.class)).orElse(null);
    assertNotNull(mcc);
    assertEquals("test.mktorest.com", mcc.getEndpointHost());
  }
}
