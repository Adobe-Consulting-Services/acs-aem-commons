/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrandPortalAgentFilter implements AgentFilter {
    private static final Logger log = LoggerFactory.getLogger(BrandPortalAgentFilter.class);

    private final List<Resource> brandPortalConfigs;
    private static final String PROP_MP_CONFIG = "mpConfig";
    private static final String PROP_TENTANT_URL = "tenantURL";

    public BrandPortalAgentFilter(Resource content) {
        brandPortalConfigs = getBrandPortalConfigs(content);
    }

    public boolean isIncluded(Agent agent) {
        final String transportURI = agent.getConfiguration().getTransportURI();

        for (final Resource config : brandPortalConfigs) {
            if (log.isDebugEnabled()) {
                log.debug("Checking Agent [ {} ] against Brand Portal cloud service config [ {} ] for property [ {} ]", agent.getId(), config.getPath(), PROP_TENTANT_URL);
            }

            final ValueMap properties = config.getValueMap();
            final String tenantUrl = StringUtils.stripToNull(properties.get(PROP_TENTANT_URL, String.class));

            if (StringUtils.isNotBlank(tenantUrl)) {
                boolean included = StringUtils.startsWith(transportURI, tenantUrl + "/");

                if (included) {
                    log.debug("Including replication agent [ {} ]", agent.getId());
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("squid:S3776")
    protected List<Resource> getBrandPortalConfigs(Resource content) {
        if (content == null) {
            return Collections.emptyList();
        } else if (JcrConstants.JCR_CONTENT.equals(content.getName())) {
            content = content.getParent();
        }

        final List<Resource> resources = new ArrayList<Resource>();
        final ResourceResolver resourceResolver = content.getResourceResolver();

        do {
            ValueMap properties = content.getValueMap();
            String[] configs = properties.get(PROP_MP_CONFIG, new String[]{});
            if (ArrayUtils.isNotEmpty(configs)) {
                if (log.isDebugEnabled()) {
                    log.debug("Resolved Brand Portal configs [ {}@{} -> {} ]", content.getPath(), PROP_MP_CONFIG, StringUtils.join(configs, ","));
                }

                for (final String config : configs) {
                    Resource r = resourceResolver.getResource(config + "/" + JcrConstants.JCR_CONTENT);
                    if (r != null) {
                        resources.add(r);
                    }
                }

                break;
            }

            content = content.getParent();

        } while (content != null);

        return resources;
    }
}
