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
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
import com.google.gson.JsonObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.util.VanityPathChecker;

import org.apache.commons.lang3.StringUtils;

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Servlet for validating vanity urls.",
		ServletResolverConstants.SLING_SERVLET_PATHS+"=/bin/acs-commons/vanity-path-validator", ServletResolverConstants.SLING_SERVLET_METHODS+"="+HttpConstants.METHOD_GET })
public class VanityPathValidator extends SlingSafeMethodsServlet {


	private static final long serialVersionUID = 929627085262363731L;
	private static final Logger log = LoggerFactory.getLogger(VanityPathValidator.class);
	public static final String AUTHOR_RUN_MODE = "author";
    public static final String IS_UNIQUE ="isUnique";
    public static final String REQUEST_PARAMETER_URL ="url";
    public static final String REQUEST_PARAMETER_CURRENT_PATH ="currentPath";
    public static final String ERROR_MESSAGE = "Error encountered while validating vanity url.";
    public static final String ERROR_MESSAGE_JSON = "{isAllowed: \"false\", response: \"error\" }";
	
	@Reference
    private SlingSettingsService slingSettingsService;

	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {

		JsonObject obj = new JsonObject();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            if(slingSettingsService.getRunModes().contains(AUTHOR_RUN_MODE)){
                final String vanityPathUrl = request.getParameter(REQUEST_PARAMETER_URL);
                if(StringUtils.isNotBlank(vanityPathUrl)) {
                    final ResourceResolver resourceResolver = request.getResourceResolver();
                    log.debug("Validating url: " +vanityPathUrl);
                    final boolean isUnique = VanityPathChecker.ValidateVanityPath(
                            resourceResolver, vanityPathUrl,request.getParameter(REQUEST_PARAMETER_CURRENT_PATH));
                    obj.addProperty(IS_UNIQUE, isUnique);
                    log.debug("Validated url {} is unique = {}",vanityPathUrl, isUnique);
                    response.getWriter().write(obj.toString());
                }
            }
        } catch (Exception e) {
            log.error(ERROR_MESSAGE, e);
            response.getWriter().write(ERROR_MESSAGE_JSON);
        }
	}

}
