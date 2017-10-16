/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2017 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@Deprecated
public class PageRootProviderImplTest {
    PageRootProviderImpl provider = new PageRootProviderImpl();

    Map<String, Object> config = new HashMap<String, Object>();

    @Test
    public void getPageRootPatterns() throws Exception {
        config.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content"});
        provider.activate(config);

        assertEquals(Arrays.asList("^(/content)(|/.*)$"), PageRootProviderConfigTest.toStringList(provider.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Deactivate() throws Exception {
        assertNull(provider.getPageRootPatterns());

        config.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        provider.activate(config);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), PageRootProviderConfigTest.toStringList(provider.getPageRootPatterns()));

        provider.deactivate();
        assertNull(provider.getPageRootPatterns());
    }
}
