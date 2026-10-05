/*
 * ACS AEM Commons
 *
 * Copyright (C) 2026 Adobe
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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Sling model for (some) redirect configuration properties (maintained below CAConfigs)
 * This is the properties maintained on parent level of the individual redirect rules.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = org.apache.sling.models.annotations.DefaultInjectionStrategy.OPTIONAL)
public class RedirectConfigurationModel {
    @ValueMapValue()
    private String contextPrefix;

    @ValueMapValue()
    private boolean ignoreSelectors;

    @ValueMapValue()
    @Default(intValues = 100)
    private int pageSize;

    public String getContextPrefix() {
        return contextPrefix;
    }

    public boolean isIgnoreSelectors() {
        return ignoreSelectors;
    }

    public int getPageSize() {
        return pageSize;
    }
}
