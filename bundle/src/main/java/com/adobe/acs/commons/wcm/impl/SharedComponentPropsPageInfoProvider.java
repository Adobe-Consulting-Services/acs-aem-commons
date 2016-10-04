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
import com.day.cq.wcm.api.PageInfoProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PageInfoProvider which indicates that site-wide component properties
 * are enabled.  Note that this provider requires Page Root Provider to
 * be configured.
 */
@Component
@Service
public class SharedComponentPropsPageInfoProvider implements PageInfoProvider {
    private static final Logger log = LoggerFactory.getLogger(SitewideComponentPropsPageInfoProvider.class);

    @Reference
    private PageRootProvider pageRootProvider;

    @Override
    public void updatePageInfo(SlingHttpServletRequest request, JSONObject info, Resource resource)
            throws JSONException {

        JSONObject props = new JSONObject();
        props.put("enabled", false);

        if (pageRootProvider != null) {
            Page page = pageRootProvider.getRootPage(resource);
            if (page != null) {
                props.put("enabled", true);
                props.put("root", page.getPath());
            }
        } else {
            log.warn("Page Root Provider must be configured for shared component properties to be supported");
        }
        info.put("sharedComponentProperties", props);
    }

}
