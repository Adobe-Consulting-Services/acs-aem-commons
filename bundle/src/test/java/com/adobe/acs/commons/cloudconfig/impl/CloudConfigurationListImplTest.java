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
package com.adobe.acs.commons.cloudconfig.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.cloudconfig.CloudConfiguration;
import com.adobe.acs.commons.cloudconfig.CloudConfigurationList;

import io.wcm.testing.mock.aem.junit.AemContext;

public class CloudConfigurationListImplTest {

  @Rule
  public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

  @Before
  public void init() {
    context.load().json("/com/adobe/acs/commons/cloudconfig/cloudconfig.json", "/conf/test");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void valid()
      throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
    requestPathInfo.setSuffix("/apps/core/wcm/templates/marketocloudconfig");

    CloudConfigurationList list = new CloudConfigurationListImpl(context.request());

    assertNotNull(list);
    Field field = list.getClass().getDeclaredField("configs");
    field.setAccessible(true);

    assertNotNull(field.get(list));

    CloudConfiguration config = new CloudConfigurationImpl(context.resourceResolver().getResource("/conf/test"));
    ((List<CloudConfiguration>) field.get(list)).add(config);

    assertNotNull(list.getCloudConfigurations());
    assertEquals(1, list.getCloudConfigurations().size());

    config = list.getCloudConfigurations().get(0);
    assertNotNull(config);
    assertEquals("/conf/test", config.getItemPath());
    assertEquals("/", config.getConfigPath());
  }

  @Test
  public void invalid() {
    MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
    requestPathInfo.setSuffix("");

    CloudConfigurationList list = new CloudConfigurationListImpl(context.request());

    assertNotNull(list);
    assertNotNull(list.getCloudConfigurations());
    assertEquals(0, list.getCloudConfigurations().size());
  }
}
