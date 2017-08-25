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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        label = "ACS AEM Commons - Page Root Provider (deprecated)",
        description = "Deprecated configuration for Page Root Provider. Please use PageRootProviderConfig instead.",
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        inherit = false
)
@Service(PageRootProviderConfig.class)
@Deprecated
/**
 * Backward compatible configuration for Page Root Provider.
 *
 * @see PageRootProviderConfig
 */
public class PageRootProviderImpl extends PageRootProviderConfig {

    @Property(
            label = "Root page path pattern",
            description = "Regex(es) used to select the root page root path. Evaluates list top-down; first match wins. Defaults to [ " + DEFAULT_PAGE_ROOT_PATH + " ]",
            cardinality = Integer.MAX_VALUE,
            value = { DEFAULT_PAGE_ROOT_PATH })
    private static final String PAGE_ROOT_PATH = PageRootProviderConfig.PAGE_ROOT_PATH;

    private static final Logger LOG = LoggerFactory.getLogger(PageRootProviderImpl.class);

    @Activate
    protected void activate(Map<String, Object> props) {
        warnDepreciation();
        super.activate(props);
    }

    @Deactivate
    protected void deactivate() {
        warnDepreciation();
        super.deactivate();
    }

    private void warnDepreciation() {
        LOG.warn("{} is deprecated. Please use {} instead!", getClass().getSimpleName(),
                 PageRootProviderConfig.class.getName());
    }

    // TODO: remove it in future releases

}
