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

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;

class AbstractResourceImplTest {

  @Test
  void doesNotAddNullValues() {
    AbstractResourceImpl nullResourceType = new AbstractResourceImpl("/fake", null, "some/fake/type", null);
    assertFalse(nullResourceType.getValueMap().containsKey(ResourceResolver.PROPERTY_RESOURCE_TYPE));

    AbstractResourceImpl nullSuperType = new AbstractResourceImpl("/fake", "some/fake/type", null, null);
    assertFalse(nullSuperType.getValueMap().containsKey("sling:resourceSuperType"));
  }
}
