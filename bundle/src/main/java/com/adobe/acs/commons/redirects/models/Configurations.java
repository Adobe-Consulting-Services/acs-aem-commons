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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 *  A Sling model to list available redirect configurations on
 *  /apps/acs-commons/content/redirect-manager/redirects.html
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class Configurations {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService(injectionStrategy= InjectionStrategy.OPTIONAL)
    private RedirectFilterMBean redirectFilter;

    private static final String REDIRECTS_RESOURCE_TYPE = "acs-commons/components/utilities/manage-redirects/redirects";

    public Collection<RedirectConfiguration> getConfigurations() {
        String sql = "SELECT * FROM [nt:unstructured] AS s WHERE ISDESCENDANTNODE([/conf]) "
                + "AND s.[sling:resourceType]='" + REDIRECTS_RESOURCE_TYPE + "'";
        log.debug(sql);
        Iterator<Resource> it = request.getResourceResolver().findResources(sql, Query.JCR_SQL2);
        List<RedirectConfiguration> lst = new ArrayList<>();
        String bucketName = redirectFilter == null ? RedirectFilter.DEFAULT_CONFIG_BUCKET : redirectFilter.getBucket();
        String configName = redirectFilter == null ? RedirectFilter.DEFAULT_CONFIG_NAME : redirectFilter.getConfigName();

        while (it.hasNext()) {
            Resource resource = it.next();
            String storageSuffix = bucketName + "/" + configName;
            RedirectConfiguration cfg = new RedirectConfiguration(resource, storageSuffix);
            lst.add(cfg);
        }
        lst.sort(Comparator.comparing(RedirectConfiguration::getName));
        return lst;
    }
}
