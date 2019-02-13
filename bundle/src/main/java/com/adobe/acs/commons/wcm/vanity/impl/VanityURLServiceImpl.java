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
package com.adobe.acs.commons.wcm.vanity.impl;

import com.adobe.acs.commons.wcm.vanity.VanityURLService;
import com.day.cq.commons.PathInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;

@Component
@Service
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class VanityURLServiceImpl implements VanityURLService {

    private static final Logger log = LoggerFactory.getLogger(VanityURLServiceImpl.class);

    private static final String VANITY_DISPATCH_CHECK_ATTR = "acs-aem-commons__vanity-check-loop-detection";
    private static final String DEFAULT_PATH_SCOPE = "/content";

    public boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException, RepositoryException {
        if (request.getAttribute(VANITY_DISPATCH_CHECK_ATTR) != null) {
            log.trace("Processing a previously vanity dispatched request. Skipping...");
            return false;
        }

        request.setAttribute(VANITY_DISPATCH_CHECK_ATTR, true);

        final String requestURI = request.getRequestURI();
        final RequestPathInfo mappedPathInfo = new PathInfo(request.getResourceResolver(), requestURI);
        final String candidateVanity = mappedPathInfo.getResourcePath();
        final String pathScope = StringUtils.removeEnd(requestURI, candidateVanity);

        log.debug("Candidate vanity URL to check and dispatch: [ {} ]", candidateVanity);

        // Check if...
        // 1) the candidateVanity and the requestURI are the same; If they are it means the request has already
        // gone through resource resolution and failed so there is no sense in sending it through again.
        // 2) the candidate is in at least 1 sling:vanityPath under /content
        if (!StringUtils.equals(candidateVanity, requestURI) && isVanityPath(pathScope, candidateVanity, request)) {
            log.debug("Forwarding request to vanity resource [ {} ]", candidateVanity);

            final RequestDispatcher requestDispatcher = request.getRequestDispatcher(candidateVanity);
            requestDispatcher.forward(new ExtensionlessRequestWrapper(request), response);
            return true;
        }

        return false;
    }

    /**
     * Checks if the provided vanity path is a valid redirect
     *
     * @param pathScope The content path to scope the vanity path too.
     * @param vanityPath Vanity path that needs to be validated.
     * @param request SlingHttpServletRequest object used for performing query/lookup
     * @return return true if the vanityPath is a registered sling:vanityPath under /content
     */
    protected boolean isVanityPath(String pathScope, String vanityPath, SlingHttpServletRequest request) throws RepositoryException {
        final Resource vanityResource = request.getResourceResolver().resolve(vanityPath);

        if (vanityResource != null) {
            String targetPath = null;

            if (vanityResource.isResourceType("sling:redirect")) {
                targetPath = vanityResource.getValueMap().get("sling:target", String.class);
            } else if (!StringUtils.equals(vanityPath, vanityResource.getPath())) {
                targetPath = vanityResource.getPath();
            }

            if (targetPath != null && StringUtils.startsWith(targetPath, StringUtils.defaultIfEmpty(pathScope, DEFAULT_PATH_SCOPE))) {
                log.debug("Found vanity resource at [ {} ] for sling:vanityPath [ {} ]", targetPath, vanityPath);
                return true;
            }
        }

        return false;
    }


}