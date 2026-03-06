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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * Service to fetch the site root page (i.e. home page) for a given resource.
 * Supports multiple (independent) configurations.
 *
 * @see PageRootProviderConfig
 */
@Component(service = PageRootProvider.class)
public class PageRootProviderMultiImpl implements PageRootProvider {

    private static final Pattern VERSION_HISTORY_PATTERN = Pattern.compile("/tmp/versionhistory/[0-9a-f]+/[0-9a-f-]+/(.*)");
    private static final Pattern XF_PATH_PATTERN = Pattern.compile("/content/experience-fragments/(.*)");
    private static final Pattern LAUNCH_PATH_PATTERN = Pattern.compile("/content/launches/.*?/content/(.*)");
    private static final Logger LOG = LoggerFactory.getLogger(PageRootProviderMultiImpl.class);

    private final RankedServices<PageRootProviderConfig> configList = new RankedServices<>(Order.ASCENDING);

    @Override
    public Page getRootPage(Resource resource) {
        String pagePath = getRootPagePath(resource.getPath());

        if (pagePath != null) {
            PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
            Page rootPage = pageManager.getPage(pagePath);

            if (rootPage == null) {
                LOG.debug("Page Root not found at [ {} ]", pagePath);
            } else if (!rootPage.isValid()) {
                LOG.debug("Page Root invalid at [ {} ]", pagePath);
            } else {
                return rootPage;
            }
        }

        return null;
    }

    @Override
    public String getRootPagePath(String resourcePath) {
        for (PageRootProviderConfig config : this.configList) {
            String pathToSearch = resourcePath;

            // If page/XF history viewer should use the corresponding live content tree to determine the root...
            if (config.getHistoryViewerFallback()) {
                pathToSearch = VERSION_HISTORY_PATTERN.matcher(pathToSearch).replaceFirst("/content/$1");
            }

            // If launch content should use the corresponding live content tree to determine the root...
            if (config.getLaunchFallback()) {
                pathToSearch = LAUNCH_PATH_PATTERN.matcher(pathToSearch).replaceFirst("/content/$1");
            }

            // If XF should use the corresponding site content tree to determine the root...
            if ("site".equals(config.getXfRootPathMethod())) {
                pathToSearch = XF_PATH_PATTERN.matcher(pathToSearch).replaceFirst("/content/$1");
            }

            for (Pattern pattern : config.getPageRootPatterns()) {
                final Matcher matcher = pattern.matcher(pathToSearch);

                if (matcher.find()) {
                    String rootPath = matcher.group(1);
                    LOG.debug("Page Root found at [ {} ]", rootPath);
                    return rootPath;
                }
            }
        }

        LOG.debug("Resource path does not include the configured page root path.");
        return null;
    }

    @Reference(name = "config", service = PageRootProviderConfig.class, cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void bindConfig(final PageRootProviderConfig config, Map<String, Object> props) {
        this.configList.bind(config, props);
    }

    protected void unbindConfig(final PageRootProviderConfig config, Map<String, Object> props) {
        this.configList.unbind(config, props);
    }

}
