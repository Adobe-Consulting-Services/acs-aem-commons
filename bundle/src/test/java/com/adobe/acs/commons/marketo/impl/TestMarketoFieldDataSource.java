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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.adobe.acs.commons.marketo.client.impl.StaticResponseMarketoClient;
import com.adobe.granite.ui.components.ds.DataSource;

import io.wcm.testing.mock.aem.junit.AemContext;

public class TestMarketoFieldDataSource {

  @Rule
  public final AemContext context = new AemContext();

  @Before
  public void init() {
    context.addModelsForPackage("com.adobe.acs.commons.marketo", "com.adobe.acs.commons.marketo.impl");
    context.load().json("/com/adobe/acs/commons/marketo/pages.json", "/content/page");
    context.load().json("/com/adobe/acs/commons/marketo/cloudconfig.json", "/conf/test");

    Resource resource = Mockito.mock(Resource.class);
    Mockito.when(resource.getResourceType()).thenReturn("cq:Page");
    Mockito.when(resource.getPath()).thenReturn("/mnt/somewhere");
    Mockito.when(resource.getResourceResolver()).thenReturn(context.resourceResolver());
    context.request().setResource(resource);
    context.requestPathInfo().setSuffix("/content/page");

  }

  @Test
  public void testdoGet() throws IOException {

    ConfigurationResourceResolver configrr = Mockito.mock(ConfigurationResourceResolver.class);
    Mockito.when(configrr.getResourceCollection(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Collections.singletonList(context.resourceResolver().getResource("/conf/test")));
    context.registerService(ConfigurationResourceResolver.class, configrr);

    MarketoFieldDataSource mktoDataSrc = new MarketoFieldDataSource();

    MarketoClient client = new StaticResponseMarketoClient(new String[] {
        "/com/adobe/acs/commons/marketo/token-response.json", "/com/adobe/acs/commons/marketo/field-response.json",
        "/com/adobe/acs/commons/marketo/response-noassets.json" });
    mktoDataSrc.bindMarketoClient(client);

    mktoDataSrc.doGet(context.request(), context.response());

    assertNotNull(context.request().getAttribute(DataSource.class.getName()));
  }

  @Test
  public void testInvalidResource() {

    ConfigurationResourceResolver configrr = Mockito.mock(ConfigurationResourceResolver.class);
    Mockito.when(configrr.getResourceCollection(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Collections.emptyList());
    context.registerService(ConfigurationResourceResolver.class, configrr);

    MarketoFieldDataSource mktoDataSrc = new MarketoFieldDataSource();

    MarketoClient client = new StaticResponseMarketoClient(new String[] { "/marketo/token-response.json",
        "/marketo/field-response.json", "/marketo/response-noassets.json" });
    mktoDataSrc.bindMarketoClient(client);

    mktoDataSrc.doGet(context.request(), context.response());

    assertNotNull(context.request().getAttribute(DataSource.class.getName()));
  }
}
