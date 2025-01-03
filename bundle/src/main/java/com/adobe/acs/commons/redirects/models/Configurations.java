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
package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 *  A Sling model to list available redirect configurations on
 *  /apps/acs-commons/content/redirect-manager/redirects.html
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class Configurations {

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService(injectionStrategy= InjectionStrategy.OPTIONAL)
    private RedirectFilterMBean redirectFilter;

    private static final String REDIRECTS_RESOURCE_TYPE = "acs-commons/components/utilities/manage-redirects/redirects";

    public Collection<RedirectConfiguration> getConfigurations() {
        List<RedirectConfiguration> configurations = new ArrayList<>();

        String bucketName = redirectFilter != null ? redirectFilter.getBucket() : RedirectFilter.DEFAULT_CONFIG_BUCKET;
        String configName = redirectFilter != null ? redirectFilter.getConfigName() : RedirectFilter.DEFAULT_CONFIG_NAME;
        String storageSuffix = bucketName + "/" + configName;

        Resource confRoot = request.getResourceResolver().getResource("/conf");

        for (Resource child : confRoot.getChildren()) {
            Resource res = child.getChild(storageSuffix);
            if (res != null && res.isResourceType(REDIRECTS_RESOURCE_TYPE)) {
                configurations.add(new RedirectConfiguration(res, storageSuffix));
            }
        }

        configurations.sort(Comparator.comparing(RedirectConfiguration::getName));
        return configurations;
    }


}
