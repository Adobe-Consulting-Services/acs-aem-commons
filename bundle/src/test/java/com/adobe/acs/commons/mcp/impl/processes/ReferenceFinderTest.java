/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.mcp.impl.processes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

public class ReferenceFinderTest {

  @Rule
  public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

  private List<Resource> foundResources = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    ctx.load().json("/com/adobe/acs/commons/mcp/impl/processes/tags.json", "/etc/tags");
    ctx.load().json("/com/adobe/acs/commons/mcp/impl/processes/content.json", "/content");

    foundResources.add(ctx.resourceResolver().getResource("/content/jcr:content"));
    foundResources.add(ctx.resourceResolver().getResource("/content/childpagy/jcr:content"));
    foundResources.add(ctx.resourceResolver().getResource("/content/theotherone/jcr:content"));
  }

  @Test
  public void findExactReferences() {
    ReferenceFinder finder = new ReferenceFinder(ctx.resourceResolver(), "workflow:wcm", "/content", true) {
      protected Stream<Resource> findReferences(ResourceResolver resolver, String reference, String searchRoot) {
        return foundResources.stream();
      }
    };
    assertEquals(2, finder.getAllReferences().size());
  }
  

  @Test
  public void findContainsReferences() {
    ReferenceFinder finder = new ReferenceFinder(ctx.resourceResolver(), "workflow:wcm", "/content", false) {
      protected Stream<Resource> findReferences(ResourceResolver resolver, String reference, String searchRoot) {
        return foundResources.stream();
      }
    };
    assertEquals(4, finder.getAllReferences().size());
  }
}
