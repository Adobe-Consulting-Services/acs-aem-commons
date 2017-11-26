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

package com.adobe.acs.commons.wcm;

import org.apache.sling.api.SlingHttpServletRequest;

import aQute.bnd.annotation.ProviderType;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface ComponentErrorHandler {
    /**
     * When attribute is set on the Request causes Component Error Handler implementation to be skipped.
     *
     * Ex: request.setAttribute(SUPPRESS_ATTR, true);
     */
    String SUPPRESS_ATTR = "com.adobe.acs.commons.wcm.component-error-handler.suppress";

    /**
     * Suppress component error handling for the Request.
     *
     * @param request Sling Request object
     */
    void suppressComponentErrorHandling(SlingHttpServletRequest request);

    /**
     * Allow component error handling for the Request.
     * Only useful after suppressComponentErrorHandling has been previously called.
     *
     * @param request Sling Request obj
     */
    void allowComponentErrorHandling(SlingHttpServletRequest request);

}
