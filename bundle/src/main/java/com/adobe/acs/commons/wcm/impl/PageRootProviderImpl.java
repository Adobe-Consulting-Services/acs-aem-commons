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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backward compatible configuration for Page Root Provider.
 *
 * @see PageRootProviderConfig
 * @deprecated use PageRootProviderConfig instead
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=PageRootProviderConfig.class
)
@Designate(ocd=PageRootProviderImpl.Config.class)
@Deprecated
public class PageRootProviderImpl extends PageRootProviderConfig {
   
   @ObjectClassDefinition()
   public @interface Config {
       @AttributeDefinition(
               name = "Root page path pattern",
               description = "Regex(es) used to select the root page root path. Evaluates list top-down; first match wins. Defaults to [ " + DEFAULT_PAGE_ROOT_PATH + " ]",
               cardinality = Integer.MAX_VALUE,
               defaultValue = { DEFAULT_PAGE_ROOT_PATH })
      String[] page_root_path();
   }
    private static final Logger log = LoggerFactory.getLogger(PageRootProviderImpl.class);

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
        log.warn("{} is deprecated. Please use {} instead!", getClass().getSimpleName(),
                 PageRootProviderConfig.class.getName());
    }

    // TODO: remove it in future releases

}
