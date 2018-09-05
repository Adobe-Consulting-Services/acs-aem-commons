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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(metatype=false)
@Service(PageRootProvider.class)
/**
 * Service to fetch the site root page (i.e. home page) for a given resource.
 * Supports multiple (independent) configurations.
 *
 * @see PageRootProviderConfig
 */
public class PageRootProviderMultiImpl implements PageRootProvider {

    private static final Logger log = LoggerFactory.getLogger(PageRootProviderMultiImpl.class);

    @Reference(name = "config", referenceInterface = PageRootProviderConfig.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private RankedServices<PageRootProviderConfig> configList = new RankedServices<>(Order.ASCENDING);

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
                return rootPage;
            }
        }

        return null;
    }

    @Override
    public String getRootPagePath(String resourcePath) {
        for (PageRootProviderConfig config : this.configList) {
            for (Pattern pattern : config.getPageRootPatterns()) {
                final Matcher matcher = pattern.matcher(resourcePath);

                if (matcher.find()) {
                    String rootPath = matcher.group(1);
                    log.debug("Page Root found at [ {} ]", rootPath);
                    return rootPath;
                }
            }
        }

        log.debug("Resource path does not include the configured page root path.");
        return null;
    }

    protected void bindConfig(final PageRootProviderConfig config, Map<String, Object> props) {
        this.configList.bind(config, props);
    }

    protected void unbindConfig(final PageRootProviderConfig config, Map<String, Object> props) {
        this.configList.unbind(config, props);
    }

}
