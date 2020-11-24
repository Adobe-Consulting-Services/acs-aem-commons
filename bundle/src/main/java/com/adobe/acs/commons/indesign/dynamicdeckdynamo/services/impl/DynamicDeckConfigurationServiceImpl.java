/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckConfigurationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = DynamicDeckConfigurationService.class, immediate = true)
@Designate(ocd = DynamicDeckConfiguration.class)
public class DynamicDeckConfigurationServiceImpl implements DynamicDeckConfigurationService {

    private DynamicDeckConfiguration config;


    @Activate
    protected void activate(DynamicDeckConfiguration config) {
        this.config = config;
    }


    public String getPlaceholderImagePath() {
        return config.placeholderImagePath();
    }

    public String getCollectionQuery() {
        return config.collectionQuery();
    }
}
