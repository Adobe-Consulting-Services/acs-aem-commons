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
package com.adobe.acs.commons.wcm.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;


public class PageRootProviderConfigTest {
    PageRootProviderConfig config = null;

    @Before
    public final void setUp() throws Exception {
        config = new PageRootProviderConfig();
    }

    @Test
    public void getPageRootPatterns() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Regex() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content/site/([a-z_-]+)"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content/site/([a-z_-]+))(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_RegexEnd() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content/site/[a-z]{2}"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content/site/[a-z]{2})(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order1() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content", "/content/a"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content)(|/.*)$", "^(/content/a)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order2() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content/a", "/content"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }


    @Test
    public void getPageRootPatterns_Deactivate() {
        assertNull(config.getPageRootPatterns());

        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content/a", "/content"}, "", false, false);
        config.activate(cfg);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));

        config.deactivate();
        assertNull(config.getPageRootPatterns());
    }

    @Test
    public void getXFRootPathMethod_Empty() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content"}, "", false, false);
        config.activate(cfg);

        assertEquals("", config.getXfRootPathMethod());
    }

    @Test
    public void getXFRootPathMethod_Site() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content"}, "site", false, false);
        config.activate(cfg);

        assertEquals("site",  config.getXfRootPathMethod());
    }

    @Test
    public void getHistoryViewerFallback_False() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content"}, "", false, false);
        config.activate(cfg);

        assertFalse(config.getHistoryViewerFallback());
    }

    @Test
    public void getHistoryViewerFallback_True() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{}, "", true, false);
        config.activate(cfg);

        assertTrue(config.getHistoryViewerFallback());
    }

    @Test
    public void getLaunchFallback_False() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{"/content"}, "", false, false);
        config.activate(cfg);

        assertFalse(config.getLaunchFallback());
    }

    @Test
    public void getLaunchFallback_True() {
        PageRootProviderConfig.Config cfg = mockConfig(new String[]{}, "", false, true);
        config.activate(cfg);

        assertTrue(config.getLaunchFallback());
    }

    private PageRootProviderConfig.Config mockConfig(String[] pageRootPaths, String xfRootPathMethod, boolean historyViewerFallback, boolean launchFallback) {
        PageRootProviderConfig.Config cfg = mock(PageRootProviderConfig.Config.class);
        when(cfg.page_root_path()).thenReturn(pageRootPaths);
        when(cfg.xf_root_path_method()).thenReturn(xfRootPathMethod != null ? xfRootPathMethod : "");
        when(cfg.history_viewer_fallback()).thenReturn(historyViewerFallback);
        when(cfg.launch_fallback()).thenReturn(launchFallback);
        return cfg;
    }

    static List<String> toStringList(final List<Pattern> patterns) {
        List<String> list = new ArrayList<>();

        for (Pattern p : patterns) {
                list.add(p.toString());
        }

        return list;
    }
}
