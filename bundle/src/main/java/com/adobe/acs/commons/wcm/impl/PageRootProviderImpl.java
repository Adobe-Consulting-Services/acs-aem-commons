/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - Page Root Provider",
        description = "Service to fetch the site root page (i.e. home page) for a given resource.",
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true
)
@Service
public class PageRootProviderImpl implements PageRootProvider {
    private static final Logger log = LoggerFactory.getLogger(PageRootProviderImpl.class);
    private static final String DEFAULT_PAGE_ROOT_PATH = "/content";

    @Property(
            label = "Root page path pattern",
            description = "Regex(es) used to select the root page root path. Evaluates list top-down; first match wins. Defaults to [ " + DEFAULT_PAGE_ROOT_PATH + " ]",
            cardinality = Integer.MAX_VALUE,
            value = { DEFAULT_PAGE_ROOT_PATH })
    private static final String PAGE_ROOT_PATH = "page.root.path";

    private List<Pattern> pageRootPatterns = new ArrayList<Pattern>();

    @Override
    public Page getRootPage(Resource resource) {
        String pagePath = getRootPagePath(resource.getPath());

        if (pagePath != null) {
            PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
            Page rootPage = pageManager.getPage(pagePath);

            if (rootPage == null) {
                log.debug("Page Root not found at [ {} ]", pagePath);
            } else if (!rootPage.isValid()) {
                log.debug("Page Root invalid at [ {} ]", pagePath);
            } else {
                log.debug("Page Root found at [ {} ]", pagePath);
                return rootPage;
            }
        }

        log.debug("Resource path does not include the configured page root path.");
        return null;
    }

    protected String getRootPagePath(String resourcePath) {
        for (Pattern pattern : pageRootPatterns) {
            final Matcher matcher = pattern.matcher(resourcePath);

            if (matcher.find()) {
                String tmp = StringUtils.trim(matcher.group(1));
                tmp = org.apache.commons.lang.StringUtils.removeEnd(tmp, "/");
                return tmp;
            }
        }

        return null;
    }

    @Activate
    protected void activate(Map<String, Object> props) {
        pageRootPatterns = new ArrayList<Pattern>();
        String[] regexes = PropertiesUtil.toStringArray(props.get(PAGE_ROOT_PATH), new String[] { DEFAULT_PAGE_ROOT_PATH });

        for(String regex : regexes) {
            try {
                Pattern p = Pattern.compile("^(" + regex + ")");
                pageRootPatterns.add(p);
                log.debug("Added Page Root Pattern [ {} ] to PageRootProvider", p.toString());
            } catch (Exception e) {
                log.error("Could not compile regex [ {} ] to pattern. Skipping...", regex, e);
            }
        }
    }
}
