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

package com.adobe.acs.commons.wcm.vanity;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * This service interface provides a hook into the VanityUrlService, and allow for further adjustment of the
 * candidateVanity Url (which other wise is simply parsed from the request's requestUri, and then resourceResolver.map(...)'d.
 * <br>
 * <br>For example, by convention a multi-tenant website might namespace sling:vanityPath properties with the tenant name;
 * <br>
 * <br>/content/tenant-a/section/page/jcr:content/@slingVanityPath=tenant-a__super-sale
 * <br>/content/tenant-b/section/page/jcr:content/@slingVanityPath=tenant-b__super-sale
 * <br>
 * <br>with www.tenant-a.com mapped to /content/tenant-a/
 * <br>with www.tenant-b.com mapped to /content/tenant-b/
 * <br>
 * <br>and www.tenant-a.com/super-sale resolving to /content/tenant-a/section/page
 * <br>and www.tenant-b.com/super-sale resolving to /content/tenant-b/section/page
 * <br>
 * <br>an impl of this, could look at the the domain attribute of the request,
 * <br>
 * <br>public String adjust(SlingHttpServletRequest request, String vanityUrl) {
 * <br>
 * <br>  // Trim leading slash so we can prefix w/ something...
 * <br>  vanityUrl = StringUtils.substringAfter(vanityUrl, "/");
 * <br>
 * <br>  if ("www.tenant-a.com").equals(request.getServerName()) {
 * <br>    return "/tenant-a__" + vanityUrl;
 * <br>  } else if ("www.tenant-b.com").equals(request.getServerName()) {
 * <br>    return "/tenant-b__" + vanityUrl;
 * <br>  } else {
 * <br>    return "/" + vanityUrl;
 * <br>  }
 * }
 */
@ConsumerType
public interface VanityUrlAdjuster {
    /**
     * Allows for custom adjustment of the vanity path after its been parsed and resourceResolver.map(..)'d, but before it's been dispatched.
     *
     * @param request the request
     * @param vanityUrl the vanityUrl derived from the request's requestUri passed through resourceResolver.map(..)
     * @return the vanityUrl to try to resolve
     */
    String adjust(SlingHttpServletRequest request, String vanityUrl);
}
