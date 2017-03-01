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

import com.adobe.acs.commons.wcm.comparisons.One2OneData;
import com.adobe.acs.commons.wcm.comparisons.One2OneDataLoader;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;


@Component(label = "ACS AEM Commons - One-to-one compare",
        description = "Compare two nodes on property level one by one", metatype = true)
@Service
@Properties({})
public class One2OneDataLoaderImpl implements One2OneDataLoader {

    @Override
    public One2OneData load(Resource resource, String versionName) throws RepositoryException {
        return new One2OneDataImpl(resource, versionName);
    }
}
