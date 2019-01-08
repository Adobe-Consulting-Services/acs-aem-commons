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
package com.adobe.acs.commons.wcm.vanity;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;

@ProviderType
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public interface VanityURLService {

    /**
     * This method checks if a given request URI (after performing the Resource Resolver Mapping) is a valid vanity URL,
     * if true it will perform the FORWARD using Request Dispatcher.
     *
     * @param request the request object
     * @param response the response object
     * @return true if this request is dispatched because it's a valid Vanity path, else false.
     */
    boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException, RepositoryException;
}