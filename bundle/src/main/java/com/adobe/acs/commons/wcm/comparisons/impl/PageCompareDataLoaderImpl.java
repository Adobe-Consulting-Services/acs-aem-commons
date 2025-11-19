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
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import org.osgi.service.component.annotations.Component;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLoader;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

@Component
public class PageCompareDataLoaderImpl implements PageCompareDataLoader {

    @Override
    public PageCompareData load(Resource resource, String versionName) throws RepositoryException {
        return new PageCompareDataImpl(resource, versionName);
    }
}
