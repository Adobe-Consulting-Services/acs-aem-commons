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

import java.util.ArrayList;
import java.util.List;
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

    private static final Logger LOG = LoggerFactory.getLogger(PageRootProviderMultiImpl.class);
    
   	@Reference(name = "config", referenceInterface = PageRootProviderConfig.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private List<PageRootProviderConfig> configList = new ArrayList<PageRootProviderConfig>();

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
			for (Pattern pattern : config.getPageRootPatterns()) {
				final Matcher matcher = pattern.matcher(resourcePath);

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
    
	@Activate
	protected void activate() {
		LOG.debug("Activating");
	}
	
	@Deactivate
	protected void deactivate() {
		LOG.debug("Deactivating");
	}
   
	protected void bindConfig(final PageRootProviderConfig config) {
		this.configList.add(config);
	}

	protected void unbindConfig(final PageRootProviderConfig config) {
		this.configList.remove(config);
	}
	
}
