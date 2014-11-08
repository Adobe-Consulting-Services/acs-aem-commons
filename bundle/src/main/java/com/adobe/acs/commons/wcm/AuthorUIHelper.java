/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

import org.apache.sling.api.resource.ResourceResolver;
import aQute.bnd.annotation.ProviderType;

/**
 * A Service that provides centralized logic for generating links to edit Pages
 * and Assets on the author environment. Driven based on an OSGi configuration
 * that allows each implementation to toggle whether to generate links to the
 * Touch UI or Classic UI. Uses the {@link com.day.cq.commons.Externalizer
 * com.day.cq.commons.Externalizer} to create absolute URLs.
 */
@ProviderType
public interface AuthorUIHelper {

    /**
     * Indicates whether the author experience is using the Touch UI or Classic
     * UI
     * 
     * @return true if configured to use Touch UI
     * 
     */
    boolean isTouchUI();

    /***
     * Generates a URL to edit a page in AEM author environment. Will return a
     * url using either the classic or touch UI url based on configuration
     * properties. Defaults to Touch UI ('/editor.html') appends the '.html'
     * extension to the returned URL String
     * 
     * @param pagePath
     *            the path to the page
     * @param absoluteUrl
     *            if set to true will use the Externalizer to generate an
     *            absolute Url with author hostname
     * @param resolver
     *            used by the {@link com.day.cq.commons.Externalizer
     *            Externalizer} for resource mapping, can be passed as null to
     *            avoid extra processing.
     * @return - returns a relative or absolute URL to edit the passed in page
     *         path
     */
    String generateEditPageLink(String pagePath, boolean absoluteUrl, ResourceResolver resolver);

    /***
     * Generates a URL to edit a DAM asset in AEM author environment. Will
     * return a url using either the classic or touch UI url based on
     * configuration properties. Defaults to Touch UI ('/assetdetails.html')
     * 
     * @param assetPath
     *            the path to the asset in the DAM
     * @param absoluteUrl
     *            if set to true will use the Externalizer to generate an
     *            absolute Url with author hostname
     * @param resolver
     *            used by the {@link com.day.cq.commons.Externalizer
     *            Externalizer} for resource mapping, can be passed as null to
     *            avoid extra processing.
     * @return - returns a relative or absolute URL to edit the passed in asset
     *         path
     */
    String generateEditAssetLink(String assetPath, boolean absoluteUrl, ResourceResolver resolver);
}
