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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;

@RunWith(MockitoJUnitRunner.class)
public class SharedComponentPropertiesBindingsValuesProviderTest {

  public static final String SITE_ROOT = "/content/acs-commons";
  public static final String RESOURCE_TYPE = "acs-commons/components/content/generic-text";

  private PageRootProvider pageRootProvider;
  private Resource resource;
  private Resource sharedPropsResource;
  private Resource globalPropsResource;
  private SlingHttpServletRequest request;
  private Bindings bindings;
  private ResourceResolver resourceResolver;
  private ValueMap sharedProps;
  private ValueMap globalProps;

  @Before
  public void setUp() throws Exception {
    resource = mock(Resource.class);
    pageRootProvider = mock(PageRootProvider.class);
    bindings = new SimpleBindings();
    sharedPropsResource = mock(Resource.class);
    globalPropsResource = mock(Resource.class);
    resourceResolver = mock(ResourceResolver.class);
    request = mock(SlingHttpServletRequest.class);

    final String globalPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
    final String sharedPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES +  "/"
        + RESOURCE_TYPE;

    bindings.put(SlingBindings.REQUEST, request);
    bindings.put(SlingBindings.RESOURCE, resource);

    when(resource.getResourceResolver()).thenReturn(resourceResolver);
    when(resource.getResourceType()).thenReturn(RESOURCE_TYPE);
    when(resourceResolver.getSearchPath()).thenReturn(new String[]{"/apps/", "/libs/"});
    when(resourceResolver.getResource(sharedPropsPath)).thenReturn(sharedPropsResource);
    when(resourceResolver.getResource(globalPropsPath)).thenReturn(globalPropsResource);

    when(resource.getPath()).thenReturn(SITE_ROOT);
    when(pageRootProvider.getRootPagePath(anyString())).thenReturn(SITE_ROOT);

    
    sharedProps = new ValueMapDecorator(new HashMap<String, Object>());
    globalProps = new ValueMapDecorator(new HashMap<String, Object>());
    sharedProps.put("shared", "value");
    globalProps.put("global", "value");

    when(globalPropsResource.getValueMap()).thenReturn(globalProps);
    when(sharedPropsResource.getValueMap()).thenReturn(sharedProps);
    when(resource.getValueMap()).thenReturn(ValueMap.EMPTY);
  }

  @Test
  public void testGetCanonicalResourceTypeRelativePath() {
    // make this test readable by wrapping the long method name with a function
    final BiFunction<String, List<String>, String> asFunction =
            (resourceType, searchPaths) -> SharedComponentPropertiesImpl
                    .getCanonicalResourceTypeRelativePath(resourceType,
                            Optional.ofNullable(searchPaths)
                                    .map(list -> list.toArray(new String[0])).orElse(null));

    final List<String> emptySearchPaths = Collections.emptyList();
    final List<String> realSearchPaths = Arrays.asList("/apps/", "/libs/");
    assertNull("expect null for null rt", asFunction.apply(null, emptySearchPaths));
    assertNull("expect null for empty rt", asFunction.apply("", emptySearchPaths));
    assertNull("expect null for absolute rt and null search paths",
            asFunction.apply("/fail/" + RESOURCE_TYPE, null));
    assertNull("expect null for cq:Page",
            asFunction.apply("cq:Page", realSearchPaths));
    assertNull("expect null for nt:unstructured",
            asFunction.apply("nt:unstructured", realSearchPaths));
    assertNull("expect null for absolute rt and empty search paths",
            asFunction.apply("/fail/" + RESOURCE_TYPE, emptySearchPaths));
    assertNull("expect null for sling nonexisting rt",
            asFunction.apply(Resource.RESOURCE_TYPE_NON_EXISTING, emptySearchPaths));
    assertEquals("expect same for relative rt", RESOURCE_TYPE,
            asFunction.apply(RESOURCE_TYPE, emptySearchPaths));
    assertEquals("expect same for relative rt and real search paths", RESOURCE_TYPE,
            asFunction.apply(RESOURCE_TYPE, realSearchPaths));
    assertEquals("expect relative for /apps/ + relative and real search paths", RESOURCE_TYPE,
            asFunction.apply("/apps/" + RESOURCE_TYPE, realSearchPaths));
    assertEquals("expect relative for /libs/ + relative and real search paths", RESOURCE_TYPE,
            asFunction.apply("/libs/" + RESOURCE_TYPE, realSearchPaths));
    assertNull("expect null for /fail/ + relative and real search paths",
            asFunction.apply("/fail/" + RESOURCE_TYPE, realSearchPaths));
  }

  @Test
  public void addBindings() {
    final SharedComponentPropertiesImpl sharedComponentProperties = new SharedComponentPropertiesImpl();
    sharedComponentProperties.pageRootProvider = pageRootProvider;

    final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
        = new SharedComponentPropertiesBindingsValuesProvider();

    sharedComponentPropertiesBindingsValuesProvider.sharedComponentProperties = sharedComponentProperties;
    sharedComponentPropertiesBindingsValuesProvider.addBindings(bindings);

    assertEquals(sharedPropsResource, bindings.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE));
    assertEquals(globalPropsResource, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE));
    assertEquals(sharedProps, bindings.get(SharedComponentProperties.SHARED_PROPERTIES));
    assertEquals(globalProps, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
  }
}
