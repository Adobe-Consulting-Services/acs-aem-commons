/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.sitemaps;

import org.apache.sling.api.resource.ResourceResolver;
import org.w3c.dom.Document;
/**
 * service factory. One config each for every site.
 * set root path,domain name and a boolean to consider navigation filter while genrating sitemap
 *
 */
public interface SiteMapGenerator {
    /**
     * generate XML of the sitemap.
     * 
     * @param resolver
     * @return
     */
	Document getSiteMap(ResourceResolver resolver);
}
