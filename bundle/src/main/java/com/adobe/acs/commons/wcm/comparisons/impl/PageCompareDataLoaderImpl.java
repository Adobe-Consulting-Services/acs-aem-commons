/*
 *
 *  * #%L
 *  * ACS AEM Commons Bundle
 *  * %%
 *  * Copyright (C) 2016 Adobe
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 *
 */
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLoader;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;

@Component(label = "ACS AEM Commons - Page Compare Tool",
        description = "Have a look at the evolution of a resource on a property/resource level.", metatype = true)
@Service
@org.apache.felix.scr.annotations.Properties({
        @Property(label = "Ignored property names",
                description = "Property names (regex possible) listed here will be excluded from the page compare feature.",
                name = PageCompareDataLoaderImpl.PROPERTY_IGNORES, value = { "(.*/)?jcr:uuid", "(.*/)?(cq|jcr):lastModified", "(.*/)?(cq|jcr):lastModifiedBy", "(.*/)?jcr:frozenUuid", "(.*/)?jcr:primaryType", "(.*/)?jcr:frozenPrimaryType" }, cardinality = Integer.MAX_VALUE),
        @Property(label = "Ignored resource names",
                description = "Resource names (regex possible) listed here will be excluded from the page compare feature.",
                name = PageCompareDataLoaderImpl.RESOURCE_IGNORES, value = { "" }, cardinality = Integer.MAX_VALUE) })
public class PageCompareDataLoaderImpl implements PageCompareDataLoader {

    private static final Logger log = LoggerFactory.getLogger(PageCompareDataLoaderImpl.class);

    static final String PROPERTY_IGNORES = "properties.ignore";
    static final String RESOURCE_IGNORES = "resources.ignore";

    private FilterConfig filterConfig;

    @Override
    public PageCompareData load(Resource resource, String versionName) throws RepositoryException {
        return new PageCompareDataImpl(resource, versionName, filterConfig);
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        String[] propertyIgnores = PropertiesUtil.toStringArray(config.get(PROPERTY_IGNORES), new String[] { "" });
        String[] resourceIgnores = PropertiesUtil.toStringArray(config.get(RESOURCE_IGNORES), new String[] { "" });
        this.filterConfig = new FilterConfig(propertyIgnores, resourceIgnores);
        log.debug("Ignored properties: {}", propertyIgnores);
        log.debug("Ignored resources: {}", resourceIgnores);
    }
}
