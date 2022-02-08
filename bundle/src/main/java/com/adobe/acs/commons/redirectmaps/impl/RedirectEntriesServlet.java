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
package com.adobe.acs.commons.redirectmaps.impl;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RequireAem;

/**
 * Servlet rendering the redirect map to a JSON Array
 */
@SlingServlet(methods = { "GET" }, resourceTypes = { "acs-commons/components/utilities/redirectmappage" }, selectors = {
        "redirectentries" }, extensions = { "json" }, metatype = false)
public class RedirectEntriesServlet extends SlingSafeMethodsServlet {
  
  // Disable this feature on AEM as a Cloud Service
  @Reference(target="(distribution=classic)")
  transient RequireAem requireAem;

    private static final long serialVersionUID = -2825679173210628699L;
    private static final Logger log = LoggerFactory.getLogger(RedirectEntriesServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doGet");

        RedirectEntriesUtils.writeEntriesToResponse(request, response,"Retrieved Redirect Maps");
    }
    
}
