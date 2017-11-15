/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.redirectmaps.models;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Sling Model interface for each instance of the redirect config.
 */
@Model(adaptables = Resource.class)
public interface RedirectConfigModel {

    /**
     * Gets the domain for mapping the request
     *
     * @return
     */
    @Inject
    String getDomain();

    /**
     * Gets the path under which to search for items to check for the vanity
     * property
     *
     * @return
     */
    @Inject
    String getPath();

    /**
     * Gets the property to use for the vanity paths
     *
     * @return
     */
    @Inject
    String getProperty();

    /**
     * Gets the protocol (http or https) for mapping the request
     *
     * @return
     */
    @Inject
    String getProtocol();

    /**
     * The resource for this configurations
     *
     * @return
     */
    @Self
    Resource getResource();

}
