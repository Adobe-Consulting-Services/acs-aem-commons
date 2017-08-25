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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;


public class PageRootProviderConfigTest {
    PageRootProviderConfig config = null;

    Map<String, Object> properties = new HashMap<String, Object>();

    @Before
    public final void setUp() throws Exception {
        config = new PageRootProviderConfig();
    }

    @Test
    public void getPageRootPatterns() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Regex() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/site/([a-z_-]+)"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/site/([a-z_-]+))(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_RegexEnd() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/site/[a-z]{2}"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/site/[a-z]{2})(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order1() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content", "/content/a"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content)(|/.*)$", "^(/content/a)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order2() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }


    @Test
    public void getPageRootPatterns_Deactivate() throws Exception {
        assertNull(config.getPageRootPatterns());

        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));

        config.deactivate();
        assertNull(config.getPageRootPatterns());
    }


    static List<String> toStringList(final List<Pattern> patterns) {
        List<String> list = new ArrayList<String>();

        for (Pattern p : patterns) {
                list.add(p.toString());
        }

        return list;
    }
}
