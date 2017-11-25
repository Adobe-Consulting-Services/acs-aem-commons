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
package com.adobe.acs.commons.forms.components;

import aQute.bnd.annotation.ProviderType;

import com.adobe.acs.commons.forms.Form;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

@ProviderType
@SuppressWarnings("squid:S00112")
public interface FormComponent {
    /**
     *
     * @return
     */
    public String getFormName();

    /**
     * Get the data from the HTTP Request and move into the Map-based Form abstraction
     * @param request
     * @return
     */
    public Form getForm(SlingHttpServletRequest request);

    /**
     * Validate the provided form data. Create any Error records on the form itself.
     *
     * @param form
     * @return
     */
    public Form validate(Form form);

    /**
     * Save the data to the underlying data store; implementation specific. This could be CRX or external data store.
     * @param form
     * @return
     */
    public boolean save(Form form);

    /**
     * Handle successful form submission. Typically includes a 302 redirect to a Success page.
     * @param form
     * @param request
     * @param response
     */
    public void onSuccess(Form form, SlingHttpServletRequest request, SlingHttpServletResponse response) throws Exception;

    /**
     * Handle unsuccessful form submission. Typically includes a 302 redirect back to self.
     * @param form
     * @param request
     * @param response
     */
    public void onFailure(Form form, SlingHttpServletRequest request, SlingHttpServletResponse response) throws Exception;
}