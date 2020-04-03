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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import javax.script.Bindings;
import javax.script.SimpleBindings;

@RunWith(MockitoJUnitRunner.class)
public class SharedComponentPropertiesBindingsValuesProviderTest {

  public static final String SITE_ROOT = "/content/acs-commons";
  public static final String RESOURCE_TYPE = "acs-commons/components/content/generic-text";

  private PageRootProvider pageRootProvider;
  private Resource resource;
  private Resource sharedPropsResource;
  private Resource globalPropsResource;
  private Page page;
  private Bindings bindings;
  private Component component;
  private ResourceResolver resourceResolver;
  private ComponentManager componentManager;
  private ValueMap sharedProps;
  private ValueMap globalProps;

  @Before
  public void setUp() throws Exception {
    resource = mock(Resource.class);
    pageRootProvider = mock(PageRootProvider.class);
    page = mock(Page.class);
    bindings = new SimpleBindings();
    component = mock(Component.class);
    sharedPropsResource = mock(Resource.class);
    globalPropsResource = mock(Resource.class);
    resourceResolver = mock(ResourceResolver.class);
    componentManager = mock(ComponentManager.class);

    final String globalPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
    final String sharedPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES +  "/"
        + RESOURCE_TYPE;

    bindings.put("resource", resource);

    when(resource.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.getResource(sharedPropsPath)).thenReturn(sharedPropsResource);
    when(resourceResolver.getResource(globalPropsPath)).thenReturn(globalPropsResource);
    when(resourceResolver.adaptTo(ComponentManager.class)).thenReturn(componentManager);
    when(componentManager.getComponentOfResource(resource)).thenReturn(component);

    when(page.getPath()).thenReturn(SITE_ROOT);
    when(pageRootProvider.getRootPage(resource)).thenReturn(page);
    when(component.getResourceType()).thenReturn(RESOURCE_TYPE);

    sharedProps = new ValueMapDecorator(new HashMap<String, Object>());
    globalProps = new ValueMapDecorator(new HashMap<String, Object>());
    sharedProps.put("shared", "value");
    globalProps.put("global", "value");

    when(globalPropsResource.getValueMap()).thenReturn(globalProps);
    when(sharedPropsResource.getValueMap()).thenReturn(sharedProps);
  }

  @Test
  public void addBindings() {
    final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
        = new SharedComponentPropertiesBindingsValuesProvider();

    Whitebox.setInternalState(sharedComponentPropertiesBindingsValuesProvider, "pageRootProvider", pageRootProvider);

    sharedComponentPropertiesBindingsValuesProvider.addBindings(bindings);

    assertEquals(sharedPropsResource, bindings.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE));
    assertEquals(globalPropsResource, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE));
    assertEquals(sharedProps, bindings.get(SharedComponentProperties.SHARED_PROPERTIES));
    assertEquals(globalProps, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
  }
}