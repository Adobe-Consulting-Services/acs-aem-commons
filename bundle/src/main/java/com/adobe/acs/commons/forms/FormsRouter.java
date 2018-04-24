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
package com.adobe.acs.commons.forms;

import aQute.bnd.annotation.ProviderType;

import com.adobe.acs.commons.forms.helpers.FormHelper;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Internal routing used for internal routing of POST form submissions in the Filters.
 */
@ProviderType
@SuppressWarnings("squid:S1214")
public interface FormsRouter {
    String FORM_RESOURCE_INPUT = FormHelper.FORM_RESOURCE_INPUT;
    String FORM_NAME_INPUT = FormHelper.FORM_NAME_INPUT;

    /**
     * Gets the Form Selector for the form POST request.
     *
     * @param slingRequest the SlingRequest obj
     * @return returns the selector as a String
     */
    String getFormSelector(final SlingHttpServletRequest slingRequest);

    /**
     * Gets the suffix to look for to identify ACS AEM Commons form submissions.
     *
     * @return returns the Suffix
     */
    String getSuffix();

    /**
     * Checks if the Request has a suffix that matches  the suffix registered to identify ACS AEM Commons form
     * submissions (and returned by .getSuffix()).
     *
     * @param slingRequest the SlingRequest obj
     *
     * @return true if the Request's suffix matches the suffix registered to identify ACS AEM Commons form submissions
     */
    boolean hasValidSuffix(SlingHttpServletRequest slingRequest);
}