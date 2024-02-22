/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.mcp.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class FieldsetTest {

  private final AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

  @Mock
  private FormField formField;

  @Mock
  private SlingScriptHelper slingScriptHelper;

  @BeforeEach
  public void setup() {
    ctx.currentResource(ctx.create().resource("/dummy/resource"));
    when(slingScriptHelper.getRequest()).thenReturn(ctx.request());
  }

  @Test
  public void classNotSpecified() {
    final FieldsetComponent fieldsetComponent = new FieldsetComponent();
    fieldsetComponent.setup("dummy", null, formField, slingScriptHelper);

    Resource resource = fieldsetComponent.buildComponentResource();
    ValueMap map = resource.getValueMap();

    assertFalse(map.containsKey("class"));
  }
}
