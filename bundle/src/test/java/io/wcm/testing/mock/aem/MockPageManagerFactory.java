/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
 * 
 * Copied here from https://github.com/wcm-io/wcm-io-testing/blob/5264197d5c6cecf054b915b5ed23ae42998917b9/aem-mock/core/src/main/java/io/wcm/testing/mock/aem/MockPageManagerFactory.java#L1
 * Only necessary as long as upgrade to AEM Mocks 3.0+ is not yet done (https://github.com/Adobe-Consulting-Services/acs-aem-commons/pull/2260)
 */
package io.wcm.testing.mock.aem;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;

import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

/**
 * Mock implementation of {@link PageManagerFactory}.
 */
@Component(service = PageManagerFactory.class)
@ProviderType
public final class MockPageManagerFactory implements PageManagerFactory {

  @Override
  public PageManager getPageManager(ResourceResolver resourceResolver) {
    return new MockPageManager(resourceResolver);
  }

}
