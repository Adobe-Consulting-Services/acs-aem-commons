/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.redirects.servlets;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirects.models.Configurations;
import com.adobe.acs.commons.redirects.models.MatchType;
import com.adobe.acs.commons.redirects.models.RedirectConfigurationModel;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.adobe.acs.commons.redirects.models.RedirectSource;

/**
 * Servlet for resolving the effective source and target redirect URLs after applying the context prefix.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = Configurations.REDIRECTS_RESOURCE_TYPE,
        selectors = "effective-source",
        extensions = "txt"
)
public class EffectiveRedirectSourceServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(EffectiveRedirectSourceServlet.class);

    private final ModelFactory modelFactory;

    @Activate
    public EffectiveRedirectSourceServlet(@Reference ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        // use parameters as redirect rule
        String source = requireParameter(request, RedirectRule.SOURCE_PROPERTY_NAME);
        MatchType matchType = getMatchType(request);
        boolean ignoreContextPrefix = getBooleanParameter(request, RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME);
        boolean caseInsensitive = getBooleanParameter(request, RedirectRule.CASE_INSENSITIVE_PROPERTY_NAME);
        boolean evaluateUri = getBooleanParameter(request, RedirectRule.EVALUATE_URI_PROPERTY_NAME);
        RedirectConfigurationModel redirectConfiguration = modelFactory.createModel(request.getResource(), RedirectConfigurationModel.class);
       
        RedirectSource redirectSource = new RedirectSource(source, matchType, ignoreContextPrefix, caseInsensitive, evaluateUri);
        String effectiveSource = getEffectiveSource(redirectSource, redirectConfiguration);
        
        response.setContentType("text/plain");
        response.getWriter().write(effectiveSource);
    }

    private String requireParameter(SlingHttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }
        return value.trim();
    }

    private MatchType getMatchType(SlingHttpServletRequest request) {
        String matchType = request.getParameter(RedirectRule.MATCH_TYPE_PROPERTY_NAME);
        if (StringUtils.isBlank(matchType)) {
            return MatchType.AUTO_DETECT;
        }
        try {
            return MatchType.valueOf(matchType);
        } catch (IllegalArgumentException e) {
            log.warn("unknown matchType value '{}', falling back to AUTO_DETECT", matchType);
        }
        return MatchType.AUTO_DETECT;
    }

    private boolean getBooleanParameter(SlingHttpServletRequest request, String name) {
        return Boolean.parseBoolean(request.getParameter(name));
    }

    // TODO: move to RedirectSource class
    String getEffectiveSource(RedirectSource source, RedirectConfigurationModel redirectConfiguration) {
        // initialize with values from request parameters
        Optional<Pattern> pattern = source.getPattern();
        if (pattern.isPresent()) {
            return pattern.get().pattern();
        } else {
            return source.getText();
        }
        
    }

}
